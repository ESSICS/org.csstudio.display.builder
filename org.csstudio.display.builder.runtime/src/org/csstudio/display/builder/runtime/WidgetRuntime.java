/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVValue;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVWritable;
import static org.csstudio.display.builder.runtime.RuntimePlugin.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.macros.MacroValueProvider;
import org.csstudio.display.builder.model.persist.WidgetClassesService;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ExecuteScriptActionInfo;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.runtime.internal.RuntimePVs;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.csstudio.display.builder.runtime.script.internal.RuntimeScriptHandler;
import org.csstudio.display.builder.runtime.script.internal.Script;
import org.csstudio.display.builder.runtime.script.internal.ScriptSupport;
import org.diirt.vtype.VType;

/** Common Widget runtime.
 *
 *  <p>Connects to scripts and PVs.
 *
 *  <p>Widgets with additional needs can implement
 *  a derived runtime and register with {@link WidgetRuntimeFactory}.
 *
 *  <p>Derived runtimes can provide {@link RuntimeAction}s
 *  that will be offered in the context menu of the widget.
 *
 *  @author Kay Kasemir
 *  @param <MW> Model widget
 */
@SuppressWarnings("nls")
public class WidgetRuntime<MW extends Widget>
{
    /** Extension point for contributing custom widget runtime */
    public static final String EXTENSION_POINT = "org.csstudio.display.builder.runtime.widgets";

    /** The widget handled by this runtime */
    protected MW widget;

    /** If widget has 'pv_name' and 'value', this listener establishes the primary PV */
    private PVNameListener pv_name_listener = null;

    /** Primary widget PV for behaviorPVName property
     *  <p>SYNC on this
     */
    private Optional<RuntimePV> primary_pv = Optional.empty();

    /** Listener for <code>primary_pv</code> */
    private RuntimePVListener primary_pv_listener;

    /** List of _all_ PVs:
     *  Primary PV,
     *  PVs used by scripts,
     *  PVs used by actions that write,
     *  additional PVs for widgets that have more than just a primary PV.
     *
     *  <p>Lazily created as the first PV is added
     */
    private volatile RuntimePVs runtime_pvs = null;

    /** PVs used by write actions
     *
     *  <p>Lazily created if there are scripts.
     */
    // This is empty for most widgets, or contains very few PVs,
    // so using List with linear lookup by name and not a HashMap
    private volatile List<RuntimePV> writable_pvs = null;

    /** Handlers for widget's behaviorScripts property,
     *  i.e. scripts that are triggered by PVs
     *
     *  <p>Lazily created if there are scripts.
     */
    private volatile List<RuntimeScriptHandler> script_handlers = null;

    /** Scripts invoked by actions, i.e. triggered by user
     *
     *  <p>Lazily created if there are scripts.
     */
    private volatile Map<ExecuteScriptActionInfo, Script> action_scripts = null;

    /** PVListener that updates 'value' property with received VType */
    protected static class PropertyUpdater implements RuntimePVListener
    {
        private final WidgetProperty<VType> property;

        /** @param property Widget property to update with values received from PV */
        public PropertyUpdater(final WidgetProperty<VType> property)
        {
            this.property = property;
            // Send initial 'disconnected' update so widget shows
            // disconnected state until the first value arrives
            disconnected(null);
        }

        @Override
        public void valueChanged(final RuntimePV pv, final VType value)
        {
            property.setValue(value);
        }

        @Override
        public void disconnected(final RuntimePV pv)
        {
            property.setValue(null);
        }
    };

    /** Listener to "pv_name" property. Connects/re-connects primary PV */
    private class PVNameListener implements WidgetPropertyListener<String>
    {
        @Override
        public void propertyChanged(final WidgetProperty<String> name_property,
                                    final String old_name, final String pv_name)
        {
            if (Objects.equals(old_name, pv_name))
                return;

            // In case already connected...
            disconnectPrimaryPV();

            if (pv_name.isEmpty())
            {
                widget.getProperty(runtimePropPVValue).setValue(PVWidget.RUNTIME_VALUE_NO_PV);
                return;
            }

            logger.log(Level.FINER, "Connecting {0} to {1}",  new Object[] { widget, pv_name });

            // Create listener, which marks the value as disconnected
            primary_pv_listener = new PropertyUpdater(widget.getProperty(runtimePropPVValue));
            // Then connect PV, which either gets a value soon,
            // or may throw exception -> widget already shows disconnected
            try
            {
                final RuntimePV pv = PVFactory.getPV(pv_name);
                synchronized (WidgetRuntime.this)
                {
                    primary_pv = Optional.of(pv);
                }
                pv.addListener(primary_pv_listener);
                // For widgets that have a "pv_writable" property, update it
                addPV(pv, widget.checkProperty(runtimePropPVWritable).isPresent());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error connecting PV " + pv_name, ex);
            }
        }
    };

    /** When widget class changes, re-apply class to widget */
    private static final WidgetPropertyListener<String> update_widget_class =
        (prop, old, class_name) ->  WidgetClassesService.getWidgetClasses().apply(prop.getWidget());


    /** @param widget {@link Widget}
     *  @return {@link WidgetRuntime} of that widget
     */
    public static WidgetRuntime<Widget> ofWidget(final Widget widget)
    {
        return widget.getUserData(Widget.USER_DATA_RUNTIME);
    }

    // initialize() could be the constructor, but
    // instantiation from Eclipse registry requires
    // zero-arg constructor

    /** Construct runtime
     *  @param widget Model widget
     */
    public void initialize(final MW widget)
    {
        this.widget = widget;
        widget.setUserData(Widget.USER_DATA_RUNTIME, this);
    }

    /** @param pv PV where widget should track the connection state */
    public void addPV(final RuntimePV pv)
    {
        addPV(pv, false);
    }

    /** @param pv PV where widget should track the connection state
     *  @param need_write_access Does widget need write access to this PV?
     */
    public void addPV(final RuntimePV pv, final boolean need_write_access)
    {
        synchronized (this)
        {
            if (runtime_pvs == null)
                runtime_pvs = new RuntimePVs(widget);
        }
        runtime_pvs.addPV(pv, need_write_access);
    }

    /** @param pv PV where widget should no longer track the connection state */
    public void removePV(final RuntimePV pv)
    {
        runtime_pvs.removePV(pv);
    }

    /** @return All PVs that the widget uses */
    public Collection<RuntimePV> getPVs()
    {
        if (runtime_pvs == null)
            return Collections.emptyList();
        return runtime_pvs.getPVs();
    }

    /** @return {@link Optional} containing primary PV of widget, if present. */
    public Optional<RuntimePV> getPrimaryPV()
    {
        return primary_pv;
    }

    /** Runtime actions
     *
     *  <p>Representation (RCP) will present them in widget's
     *  context menu.
     *
     *  @return Runtime actions
     */
    public Collection<RuntimeAction> getRuntimeActions()
    {
        // Derived class can provide actions. Default has none.
        return Collections.emptyList();
    }

    /** Start: Connect to PVs, start scripts
     *  @throws Exception on error
     */
    public void start() throws Exception
    {
        // Update "value" property from primary PV, if defined
        final Optional<WidgetProperty<String>> name = widget.checkProperty(propPVName);
        final Optional<WidgetProperty<VType>> value = widget.checkProperty(runtimePropPVValue);

        if (name.isPresent() &&  value.isPresent())
        {
            pv_name_listener = new PVNameListener();
            // Fetching the PV name will resolve macros,
            // i.e. set the name property and thus notify listeners
            // -> Do that once before registering listener
            final String pv_name = name.get().getValue();
            name.get().addPropertyListener(pv_name_listener);
            // Initial connection
            pv_name_listener.propertyChanged(name.get(), null, pv_name);
        }

        // Prepare action-related PVs
        final List<ActionInfo> actions = widget.propActions().getValue().getActions();
        if (actions.size() > 0)
        {
            final List<RuntimePV> action_pvs = new ArrayList<>();
            for (final ActionInfo action : actions)
            {
                if (action instanceof WritePVActionInfo)
                {
                    final String pv_name = ((WritePVActionInfo) action).getPV();
                    final String expanded = MacroHandler.replace(widget.getMacrosOrProperties(), pv_name);
                    final RuntimePV pv = PVFactory.getPV(expanded);
                    action_pvs.add(pv);
                    addPV(pv, true);
                }
            }
            if (action_pvs.size() > 0)
                this.writable_pvs = action_pvs;
        }

        widget.propClass().addPropertyListener(update_widget_class);

        // Start scripts in pool because Jython setup is expensive
        RuntimeUtil.getExecutor().execute(this::startScripts);
    }

    /** Start Scripts */
    private void startScripts()
    {
        // Start scripts triggered by PVs
        final List<ScriptInfo> script_infos = widget.propScripts().getValue();
        final List<RuleInfo> rule_infos = widget.propRules().getValue();
        if ((script_infos.size() > 0) || (rule_infos.size() > 0))
        {
            final List<RuntimeScriptHandler> handlers = new ArrayList<>(script_infos.size() + rule_infos.size());

            for (final ScriptInfo script_info : script_infos)
            {
                try
                {
                    handlers.add(new RuntimeScriptHandler(widget, script_info));
                }
                catch (final Exception ex)
                {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Script failed to compile\n");
                    try
                    {
                        final DisplayModel model = widget.getDisplayModel();
                        buf.append("Display '").append(model.getDisplayName()).append("', ");
                    }
                    catch (Exception ignore)
                    {
                        // Skip display model
                    }
                    buf.append(widget).append(", ").append(script_info.getPath());
                    logger.log(Level.WARNING, buf.toString(), ex);
                }
            }

            for (final RuleInfo rule_info : rule_infos)
            {
                try
                {
                    handlers.add(new RuntimeScriptHandler(widget, rule_info));
                }
                catch (final Exception ex)
                {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Rule failed to compile\n");
                    try
                    {
                        final DisplayModel model = widget.getDisplayModel();
                        buf.append("Display '").append(model.getDisplayName()).append("', ");
                    }
                    catch (Exception ignore)
                    {
                        // Skip display model
                    }
                    buf.append(widget).append(", ").append(rule_info.getName());
                    logger.log(Level.WARNING, buf.toString(), ex);
                }
            }

            script_handlers = handlers;
        }



        // Compile scripts invoked by actions
        final List<ActionInfo> actions = widget.propActions().getValue().getActions();
        if (actions.size() > 0)
        {
            final Map<ExecuteScriptActionInfo, Script> scripts = new HashMap<>();
            for (ActionInfo action_info : actions)
            {
                if (! (action_info instanceof ExecuteScriptActionInfo))
                    continue;
                final ExecuteScriptActionInfo script_action = (ExecuteScriptActionInfo) action_info;
                try
                {
                    final MacroValueProvider macros = widget.getMacrosOrProperties();
                    final Script script = RuntimeScriptHandler.compileScript(widget, macros, script_action.getInfo());
                    scripts.put(script_action, script);
                }
                catch (final Exception ex)
                {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Script for action failed to compile\n");
                    try
                    {
                        final DisplayModel model = widget.getDisplayModel();
                        buf.append("Display '").append(model.getDisplayName()).append("', ");
                    }
                    catch (Exception ignore)
                    {
                        // Skip display model
                    }
                    buf.append(widget).append(", ").append(script_action);
                    logger.log(Level.WARNING, buf.toString(), ex);
                }
            }
            if (scripts.size() > 0)
                action_scripts = scripts;
        }
    }

    /** Write a value to the primary PV
     *  @param value
     */
    public void writePrimaryPV(final Object value)
    {
        try
        {
            final RuntimePV pv;
            synchronized (this)
            {
                pv = primary_pv.orElse(null);
            }
            if (pv == null)
                throw new Exception("No PV");
            pv.write(value);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING,
                "Widget " + widget.getName() + " write error for value " + value, ex);
        }
    }

    /** Write a value to a PV
     *  @param pv_name Name of PV to write, may contain macros
     *  @param value Value to write
     *  @throws Exception on error
     */
    public void writePV(final String pv_name, final Object value) throws Exception
    {
        final String expanded = MacroHandler.replace(widget.getMacrosOrProperties(), pv_name);
        String name_to_check = expanded;
        // For local PV,
        if (name_to_check.startsWith("loc://"))
        {
            // strip optional data type ...
            int sep = name_to_check.indexOf('<');
            if (sep > 0)
                name_to_check = name_to_check.substring(0, sep);
            // or initializer ..
            sep = name_to_check.indexOf('(');
            if (sep > 0)
                name_to_check = name_to_check.substring(0, sep);
        }
        final List<RuntimePV> safe_pvs = writable_pvs;
        if (safe_pvs != null)
            for (final RuntimePV pv : safe_pvs)
                if (pv.getName().equals(name_to_check))
                {
                    try
                    {
                        pv.write(value);
                    }
                    catch (final Exception ex)
                    {
                        throw new Exception("Failed to write " + value + " to PV " + name_to_check, ex);
                    }
                    return;
                }
        throw new Exception("Unknown PV '" + pv_name + "' (expanded: '" + name_to_check + "')");
    }

    /** Execute script
     *  @param action_info Which script-based action to execute
     *  @throws NullPointerException if action_info is not valid, runtime not initialized
     */
    public void executeScriptAction(final ExecuteScriptActionInfo action_info) throws NullPointerException
    {
        final Map<ExecuteScriptActionInfo, Script> actions = Objects.requireNonNull(action_scripts);
        final Script script = Objects.requireNonNull(actions.get(action_info));
        script.submit(widget);
    }

    /** Disconnect the primary PV
     *
     *  <p>OK to call when there was no PV
     */
    private void disconnectPrimaryPV()
    {
        final RuntimePV pv;
        synchronized (this)
        {
            pv = primary_pv.orElse(null);
            primary_pv = Optional.empty();
        }
        if (pv == null)
            return;
        removePV(pv);
        pv.removeListener(primary_pv_listener);
        PVFactory.releasePV(pv);
    }

    /** Stop: Disconnect PVs, ... */
    public void stop()
    {
        widget.propClass().removePropertyListener(update_widget_class);

        final List<RuntimePV> safe_pvs = writable_pvs;
        if (safe_pvs != null)
        {
            for (final RuntimePV pv : safe_pvs)
            {
                removePV(pv);
                PVFactory.releasePV(pv);
            }
            writable_pvs = null;
        }

        disconnectPrimaryPV();
        if (pv_name_listener != null)
        {
            widget.getProperty(propPVName).removePropertyListener(pv_name_listener);
            pv_name_listener = null;
        }

        final Map<ExecuteScriptActionInfo, Script> actions = action_scripts;
        if (actions != null)
        {
            actions.clear();
            action_scripts = null;
        }

        final List<RuntimeScriptHandler> handlers = script_handlers;
        if (handlers != null)
        {
            for (final RuntimeScriptHandler handler : handlers)
                handler.shutdown();
            script_handlers = null;
        }

        if (runtime_pvs != null)
        {
            final Collection<RuntimePV> pvs = runtime_pvs.getPVs();
            if (!pvs.isEmpty())
                logger.log(Level.SEVERE, widget + " has unreleased PVs: " + pvs);
        }

        // Close script support that might have been created
        // by RuntimeScriptHandlers or action-invoked scripts
        final ScriptSupport scripting = widget.getUserData(Widget.USER_DATA_SCRIPT_SUPPORT);
        if (scripting != null)
        	scripting.close();
    }
}

