/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script;

import static org.csstudio.display.builder.runtime.RuntimePlugin.logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.Preferences;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.python.core.PyCode;
import org.python.core.PyList;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/** Jython script support
 *
 *  <p>To debug, see python.verbose which can also be set
 *  as VM property.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class JythonScriptSupport implements AutoCloseable
{
    private final ScriptSupport support;

    final static boolean initialized = init();

    /** Scripts that have been submitted, awaiting execution, to avoid queuing them again.
     *
     *  <p>Relies on the fact that each script is unique identified by the script itself,
     *  they're not submitted with different widget and pvs parameters.
     */
    private final Set<JythonScript> queued_scripts = Collections.newSetFromMap(new ConcurrentHashMap<JythonScript, Boolean>());

    private final PythonInterpreter python;

    /** Perform static, one-time initialization */
    private static boolean init()
    {
        try
        {
            final Properties pre_props = System.getProperties();
            final Properties props = new Properties();

            // Locate the jython plugin for 'home' to allow use of /Lib in there
            final String home = getPluginPath("org.python.jython", "/");
            if (home == null)
                throw new Exception("Cannot locate jython bundle. No OSGi?");

            // Jython 2.7(b3) needs these to set sys.prefix and sys.executable.
            // If left undefined, initialization of Lib/site.py fails with
            // posixpath.py", line 394, in normpath AttributeError:
            // 'NoneType' object has no attribute 'startswith'
            props.setProperty("python.home", home);
            props.setProperty("python.executable", "None");

            // Disable cachedir to avoid creation of cachedir folder.
            // See http://www.jython.org/jythonbook/en/1.0/ModulesPackages.html#java-package-scanning
            // and http://wiki.python.org/jython/PackageScanning
            props.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");

            // With python.home defined, there is no more
            // "ImportError: Cannot import site module and its dependencies: No module named site"
            // Skipping the site import still results in faster startup
            props.setProperty("python.import.site", "false");

            // Prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
            props.setProperty("python.console.encoding", "UTF-8");

            // This will replace entries found on JYTHONPATH
            final String python_path = Preferences.getPythonPath();
            if (! python_path.isEmpty())
                props.setProperty("python.path", python_path);

            // Options: error, warning, message (default), comment, debug
            // props.setProperty("python.verbose", "debug");
            // Options.verbose = Py.DEBUG;

            PythonInterpreter.initialize(pre_props, props, new String[0]);
            return true;
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Once this worked OK, but now the Jython initialization failed. Don't you hate computers?", ex);
        }
        return false;
    }

    /** Locate a path inside a bundle.
     *
     *  <p>If the bundle is JAR-ed up, the {@link FileLocator} will
     *  return a location with "file:" and "..jar!/path".
     *  This method patches the location such that it can be used
     *  on the Jython path.
     *
     *  @param bundle_name Name of bundle
     *  @param path_in_bundle Path within bundle
     *  @return Location of that path within bundle, or <code>null</code> if not found or no bundle support
     *  @throws IOException on error
     */
    private static String getPluginPath(final String bundle_name, final String path_in_bundle) throws IOException
    {
        final Bundle bundle = Platform.getBundle(bundle_name);
        if (bundle == null)
            return null;
        final URL url = FileLocator.find(bundle, new Path(path_in_bundle), null);
        if (url == null)
            return null;
        String path = FileLocator.resolve(url).getPath();

        // Turn politically correct URL into path digestible by jython
        if (path.startsWith("file:/"))
           path = path.substring(5);
        path = path.replace(".jar!", ".jar");

        return path;
    }


    /** Create executor for jython scripts
     *  @param support {@link ScriptSupport}
     */
    public JythonScriptSupport(final ScriptSupport support) throws Exception
    {
        this.support = support;

        // Concurrent creation of python interpreters has in past resulted in
        //     Lib/site.py", line 571, in <module> ..
        //     Lib/sysconfig.py", line 159, in _subst_vars AttributeError: {'userbase'}
        // or  Lib/site.py", line 122, in removeduppaths java.util.ConcurrentModificationException
        // Sync. on JythonScriptSupport to serialize the interpreter creation and avoid above errors.
        final long start = System.currentTimeMillis();
        synchronized (JythonScriptSupport.class)
        {
            // Could create a new 'state' for each interpreter
            // ++ Seems 'correct' since each interpreter then has its own path etc.
            // -- In scan server, some instances of the PythonInterpreter seemed
            //    to fall back to the default PySystemState even though
            //    a custom state was provided. Seemed related to thread local,
            //    not fully understood.
            // -- Using a new PySystemState adds about 3 second startup time,
            //    while using the default state only incurs that 3 second delay
            //    on very first access.
            // ==> Not using state = new PySystemState();
            final PySystemState state = null;
            python = new PythonInterpreter(null, state);
        }
        final long end = System.currentTimeMillis();
        logger.log(Level.FINE, "Time to create jython: {0} ms", (end - start));
    }

    /** Parse and compile script file
     *
     *  @param path Path to add to search path, or <code>null</code>
     *  @param name Name of script (file name, URL)
     *  @param stream Stream for the script content
     *  @return {@link Script}
     *  @throws Exception on error
     */
    public Script compile(final String path, final String name, final InputStream stream) throws Exception
    {
        if (path != null)
        {   // Since using default PySystemState (see above), check if already in paths
            final PyList paths = python.getSystemState().path;
            if (! paths.contains(path))
            {
                logger.log(Level.FINE, "Adding to jython path: {0}", path);
                paths.add(0, path);
            }
        }
        final long start = System.currentTimeMillis();
        final PyCode code = python.compile(new InputStreamReader(stream), name);
        final long end = System.currentTimeMillis();
        logger.log(Level.FINE, "Time to compile {0}: {1} ms", new Object[] { name, (end - start) });
        return new JythonScript(this, name, code);
    }

    /** Request that a script gets executed
     *  @param script {@link JythonScript}
     *  @param widget Widget that requests execution
     *  @param pvs PVs that are available to the script
     *  @return Future for script that was just started
     */
    public Future<Object> submit(final JythonScript script, final Widget widget, final RuntimePV... pvs)
    {
        // Skip script that's already in the queue.
        // Check-then-set, no atomic submit-unless-queued logic.
        // Might still add some scripts twice, but good enough.
        if (queued_scripts.contains(script))
        {
            logger.log(Level.FINE, "Skipping script {0}, already queued for execution", script);
            return null;
        }
        queued_scripts.add(script);

        // System.out.println("Submit on " + Thread.currentThread().getName());
        return support.submit(() ->
        {
            // System.out.println("Executing " + script + " on " + Thread.currentThread().getName());
            // Script may be queued again
            queued_scripts.remove(script);
            try
            {
                // Executor is single-threaded.
                // OK to set 'widget' etc.
                // of the shared python interpreter
                // because only one script will execute at a time.
                python.set("widget", widget);
                python.set("pvs", pvs);
                python.exec(script.getCode());
            }
            catch (final Throwable ex)
            {
                logger.log(Level.WARNING, "Execution of '" + script + "' failed", ex);
            }
            // System.out.println("Finished " + script);
            return null;
        });
    }

    /** Release resources (interpreter, ...) */
    @Override
    public void close()
    {
        python.close();
    }
}
