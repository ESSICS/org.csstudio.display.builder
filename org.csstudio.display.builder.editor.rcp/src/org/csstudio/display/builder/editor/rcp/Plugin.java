/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.rcp;

import java.util.function.BiFunction;
import java.util.logging.Logger;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.csstudio.ui.util.dialogs.ResourceSelectionDialog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/** Plugin Info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Plugin extends org.eclipse.core.runtime.Plugin
{
    /** Plugin ID */
    public static final String ID = "org.csstudio.display.builder.editor.rcp";

    /** Suggested logger for RCP Editor */
    public final static Logger logger = Logger.getLogger(Plugin.class.getName());

    /** File prompter for workspace resources */
    private final static BiFunction<Widget, String, String> workspace_file_prompt = (widget, initial) ->
    {
        final String[] extensions = new String[FilenameSupport.file_extensions.length];
        for (int i=0; i<extensions.length; ++i)
            extensions[i] = FilenameSupport.file_extensions[i].getExtensions().get(0);

        final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final ResourceSelectionDialog dialog = new ResourceSelectionDialog(shell, "Select file", extensions);

        try
        {
            final DisplayModel model = widget.getDisplayModel();
            final IPath path;
            if (initial.isEmpty())
                // Use directory of display file
                path = new Path(model.getUserData(DisplayModel.USER_DATA_INPUT_FILE)).removeLastSegments(1);
            else
                // Use current file
                path = new Path(ModelResourceUtil.resolveResource(model, initial));
            dialog.setSelectedResource(path);
        }
        catch (Exception ex)
        {
            // Can't set initial file name, ignore.
            ex.printStackTrace();
        }

        if (dialog.open() == Window.CANCEL)
            return null;

        return dialog.getSelectedResource().toOSString();
    };

    @Override
    public void start(final BundleContext context) throws Exception
    {
        super.start(context);
        // Replace default file prompt for any file with workspace prompt
        FilenameSupport.setFilePrompt(workspace_file_prompt);
    }

    public static ImageDescriptor getIcon(final String name)
    {
        return AbstractUIPlugin.imageDescriptorFromPlugin(ID, "icons/" + name);
    }
}
