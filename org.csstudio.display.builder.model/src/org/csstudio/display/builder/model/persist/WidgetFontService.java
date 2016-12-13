/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Preferences;
import org.csstudio.display.builder.model.util.ModelThreadPool;

/** Service that provides {@link NamedWidgetFonts}
 *
 *  <p>Handles the loading and re-loading of fonts
 *  in background thread
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetFontService
{
    /** Current set of named fonts.
     *  When still in the process of loading,
     *  this future will be active, i.e. <code>! isDone()</code>.
     */
    private volatile static Future<NamedWidgetFonts> fonts = CompletableFuture.completedFuture(new NamedWidgetFonts());

    /** Ask service to load fonts from a source.
     *
     *  <p>Service loads the fonts in background thread.
     *  The 'source' is called in that background thread
     *  to provide the input stream.
     *  The source should thus perform any potentially slow operation
     *  (open file, connect to http://) when called, not beforehand.
     *
     *  @param name   Name that identifies the source (for error messages)
     *  @param source Supplier of InputStream for named fonts
     */
    public static void loadFonts(final String name, final Callable<InputStream> source)
    {
        fonts = ModelThreadPool.getExecutor().submit(() ->
        {
            final NamedWidgetFonts fonts = new NamedWidgetFonts();
            try
            {
                final InputStream stream = source.call();
                logger.log(Level.CONFIG, "Loading named fonts from {0}",  name);
                fonts.read(stream);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot load fonts from " + name, ex);
            }
            // In case of error, result may only contain partial content of file
            return fonts;
        });
    }

    /** Obtain current set of named fonts.
     *
     *  <p>If service is still in the process of loading
     *  named fonts from a source, this call will delay
     *  a little bit to await completion.
     *  This method should thus be called off the UI thread.
     *
     *  <p>This method will not wait indefinitely, however.
     *  If loading fonts from a source takes too long,
     *  it will log a warning and return a default set of fonts.
     *
     *  @return {@link NamedWidgetColors}
     */
    public static NamedWidgetFonts getFonts()
    {
        // When in the process of loading, wait a little bit..
        try
        {
            return fonts.get(Preferences.getReadTimeout(), TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException timeout)
        {
            logger.log(Level.WARNING, "Using default fonts because font file is still loading");
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot obtain named fonts", ex);
        }
        return new NamedWidgetFonts();
    }
}
