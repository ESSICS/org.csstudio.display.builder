package org.csstudio.display.builder.runtime.script;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.Macros;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.diirt.vtype.VType;

/** Script Utilities
 *
 *  <p>Contains static utility methods to be used in scripts:
 *  <ul>
 *  <li>Logging
 *  <li>Dialogs
 *  <li>Obtaining PVs from widgets
 *  </ul>
 *
 * @author Amanda Carpenter
 *
 */
@SuppressWarnings("nls")
public class ScriptUtil
{
    // ================
    // Model gymnastics

    /** Locate a widget by name
     *
     *  @param widget Widget used to locate the widget model
     *  @param name Name of widget to find
     *  @return Widget or <code>null</code>
     *  @throws Exception on error
     */
    public static Widget findWidgetByName(final Widget widget, final String name) throws Exception
    {
        final ChildrenProperty siblings = widget.getDisplayModel().runtimeChildren();
        return siblings.getChildByName(name);
    }



    // ================
    // logging utils

    private static final Logger logger = Logger.getLogger("script");

    /** Get logger for scripts.
     *
     *  <p>The logger is a basic java.util.logging.Logger:
     *  <pre>
     *  getLogger().warning("Script has a problem")
     *  getLogger().info("Script is at step 3")
     *  </pre>
     *  @return Logger for scripts
     */
    public static Logger getLogger()
    {
        return logger;
    }

    // ====================
    // open/close displays

    /** Open a new display
     *
     *  @param widget Widget in 'current' display, used to resolve relative paths
     *  @param file Path to the display
     *  @param target Where to show the display: "REPLACE", "TAB", "WINDOW"
     *  @param macros Macros, may be <code>null</code>
     */
    public static void openDisplay(final Widget widget, final String file, final String target, final Map<String, String> macros)
    {
        OpenDisplayActionInfo.Target the_target;
        try
        {
            the_target = OpenDisplayActionInfo.Target.valueOf(target);
        }
        catch (Throwable ex)
        {
            the_target = OpenDisplayActionInfo.Target.TAB;
        }

        final Macros the_macros;
        if (macros == null  ||  macros.isEmpty())
            the_macros = null;
        else
        {
            the_macros = new Macros();
            for (String name : macros.keySet())
                the_macros.add(name, macros.get(name));
        }

        final OpenDisplayActionInfo open = new OpenDisplayActionInfo("Open from script", file, the_macros, the_target);
        ActionUtil.handleAction(widget, open);
    }

    /** Close a display
     *
     *  @param widget Widget within the display to close
     */
    public static void closeDisplay(final Widget widget)
    {
        try
        {
            final DisplayModel model = RuntimeUtil.getTopDisplayModel(widget);
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(model);
            toolkit.closeWindow(model);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot close display", ex);
        }
    }

    // ====================
    // public alert dialog utils

    /** Show a message dialog.
     *
     *  <p>Call blocks until the user presses "OK"
     *  in the dialog.
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param message Message to display on dialog
     */
    public static void showMessageDialog(final Widget widget, final String message)
    {
        try
        {
            ToolkitRepresentation.getToolkit(widget.getDisplayModel()).showMessageDialog(widget, message);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error showing message dialog '" + message + "'", ex);
        }
    }

    /** Show an error dialog.
     *
     *  <p>Call blocks until the user presses "OK"
     *  in the dialog.
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param error Message to display on dialog
     */
    public static void showErrorDialog(final Widget widget, final String error)
    {
        try
        {
            ToolkitRepresentation.getToolkit(widget.getDisplayModel()).showErrorDialog(widget, error);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error showing error dialog '" + error + "'", ex);
        }
    }

    /** Show confirmation dialog.
     *
     *  <p>Call blocks until the user closes the dialog
     *  by selecting either "Yes" or "No"
     *  ("Confirm", "Cancel", depending on implementation).
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param question Message to display on dialog
     *  @return <code>true</code> if user selected "Yes" ("Confirm")
     */
    public static boolean showConfirmationDialog(final Widget widget, final String question)
    {
        try
        {
            return ToolkitRepresentation.getToolkit(widget.getDisplayModel()).showConfirmationDialog(widget, question);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error asking in dialog for " + question, ex);
        }
        return false;
    }

    /** Show dialog for selecting one item from a list.
     *
     *  <p>Call blocks until the user closes the dialog
     *  by either selecting an item and pressing "OK",
     *  or by pressing "Cancel".
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param title Dialog title
     *  @param options Options to show in dialog
     *  @return Selected item or <code>null</code>
     */
    public static String showSelectionDialog(final Widget widget, final String title, final List<String> options)
    {
        try
        {
            return ToolkitRepresentation.getToolkit(widget.getDisplayModel()).showSelectionDialog(widget, title, options);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error in selection dialog for " + title, ex);
        }
        return null;
    }

    /** Show dialog for entering a password.
     *
     *  <p>Call blocks until the user closes the dialog
     *  by either entering a password and pressing "OK",
     *  or by pressing "Cancel".
     *
     *  <p>When a <code>correct_password</code> is provided to the call,
     *  the password entered by the user is checked against it,
     *  prompting until the user enters the correct one.
     *
     *  <p>When no <code>correct_password</code> is provided to the call,
     *  any password entered by the user is returned.
     *  The calling script would then check the password and maybe open
     *  the dialog again.
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param title Dialog title
     *  @param correct_password Password to check
     *  @return Entered password or <code>null</code>
     */
    public static String showPasswordDialog(final Widget widget, final String title, final String correct_password)
    {
        try
        {
            return ToolkitRepresentation.getToolkit(widget.getDisplayModel()).showPasswordDialog(widget, title, correct_password);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error in selection dialog for " + title, ex);
        }
        return null;
    }

    /** Show file "Save As" dialog for selecting/entering a new file name
     *
     *  <p>Call blocks until the user closes the dialog
     *  by either either entering/selecting a file name, or pressing "Cancel".
     *
     *  @param widget Widget, used to create and position the dialog
     *  @param initial_value Initial path and file name
     *  @return Path and file name or <code>null</code>
     */
    public static String showSaveAsDialog(final Widget widget, final String initial_value)
    {
        try
        {
            return ToolkitRepresentation.getToolkit(widget.getDisplayModel()).showSaveAsDialog(widget, initial_value);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error in save-as dialog for " + initial_value, ex);
        }
        return null;
    }

    /** Play audio
     *
     *  <p>Audio file can be a file
     *  in the same directory as the display,
     *  a workspace path if running under RCP,
     *  an absolute file system path,
     *  or a http URL.
     *
     *  <p>Result can be used to await end of playback via `get()`,
     *  to poll via 'isDone()',
     *  or to `cancel(true)` the playback.
     *  Caller should keep a reference to the future until
     *  and of playback, because otherwise garbage collection
     *  can end the playback.
     *
     *  @param widget Widget, used to coordinate with toolkit
     *  @param audio_file Audio file
     *  @return Handle for controlling the playback
     */
    public static Future<Boolean> playAudio(final Widget widget, final String audio_file)
    {
        try
        {
            final DisplayModel widget_model = widget.getDisplayModel();
            final String display_file = widget_model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
            final String resolved = ModelResourceUtil.resolveResource(display_file, audio_file);
            String url;
            if (resolved.startsWith("http:") || resolved.startsWith("https:"))
                url = resolved;
            else
            {
                final String local = ModelResourceUtil.getLocalPath(resolved);
                if (local != null)
                    url = new File(local).toURI().toString();
                else
                    url = new File(resolved).toURI().toString();
            }
            return ToolkitRepresentation.getToolkit(widget.getDisplayModel()).playAudio(url);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error playing audio " + audio_file, ex);
        }
        return CompletableFuture.completedFuture(false);
    }

    // ==================
    // get PV utils

    /** Get primary PV of given widget.
     *
     *  <p>This is the PV that a Text Update widget displays,
     *  or the one that a Text Entry widget writes.
     *
     *  <p>Some widgets have no primary PV (Label, Rectangle),
     *  or more then one PV (XY Plot).
     *
     *  @param widget Widget to get PV of.
     *  @return Primary PV of widget; otherwise, if not found, null.
     */
    public static RuntimePV getPrimaryPV(final Widget widget)
    {
        Optional<RuntimePV> pv = WidgetRuntime.ofWidget(widget).getPrimaryPV();
        return pv.orElse(null);
    }

    /** Get all PVs of a widget.
     *
     *  <p>This includes the primary PV of a widget as well as
     *  PVs from scripts and rules assigned to the widget.
     *
     *  @param widget Widget of which to get PVs
     *  @return List of PVs.
     */
    public static Collection<RuntimePV> getPVs(final Widget widget)
    {
        return WidgetRuntime.ofWidget(widget).getPVs();
    }

    /** Get widget's PV by name
     *
     *  <p>Locates widget's PV by name, including the primary PV
     *  as well as PVs from scripts and rules.
     *
     *  @param widget Widget to get PV from
     *  @param name  Name of PV to get
     *  @return PV of given widget with given name; otherwise, if not found,
     *          <code>null</code>
     */
    public static RuntimePV getPVByName(final Widget widget, final String name)
    {
        final Collection<RuntimePV> pvs = getPVs(widget);
        for (RuntimePV pv : pvs)
            if (name.equals(pv.getName()))
                return pv;
        logger.warning("Could not find PV with name '" + name + "' for " + widget);
        return null;
    }

    // ==================
    // Workspace helpers

    /** Locate a widget by name, then fetch its value
     *
     *  <p>Value is the content of the "value" property.
     *  If there is no such property, the primary PV
     *  of the widget is read.
     *
     *  @param widget Widget used to locate the widget model
     *  @param name Name of widget to find
     *  @return Value of the widget
     *  @throws Exception on error
     */
    public static VType getWidgetValueByName(final Widget widget, final String name) throws Exception
    {
        final Widget w = findWidgetByName(widget, name);
        final Optional<WidgetProperty<Object>> value_prop = w.checkProperty("value");
        if (value_prop.isPresent())
        {
            final Object value = value_prop.get().getValue();
            if (value == null  ||  value instanceof VType)
                return (VType) value;
        }

        return getPrimaryPV(w).read();
    }

    /** @param workspace_path Path within workspace
     *  @return Location in local file system or <code>null</code>
     */
    public static String workspacePathToSysPath(final String workspace_path)
    {
        return ModelResourceUtil.getLocalPath(workspace_path);
    }
}