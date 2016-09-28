/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.actions;

import java.io.File;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.EditorDemoGUI;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.util.FilenameSupport;

import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Prompt for file name to save model
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LoadModelAction extends ActionDescription
{
    private final Window window = null;
    private final EditorDemoGUI editor;

    /** @param save_handler Will be invoked with file name */
    public LoadModelAction(final EditorDemoGUI editor)
    {
        super("icons/open.png", Messages.LoadDisplay_TT);
        this.editor = editor;
    }

    @Override
    public void run(final DisplayEditor ignored, final boolean selected)
    {
        final FileChooser dialog = new FileChooser();
        dialog.setTitle(Messages.LoadDisplay);

        File file = editor.getFile();
        if (file != null)
        {
            dialog.setInitialDirectory(file.getParentFile());
            dialog.setInitialFileName(file.getName());
        }
        dialog.getExtensionFilters().addAll(FilenameSupport.file_extensions);
        file = dialog.showOpenDialog(window);
        if (file == null)
            return;

        // If file has no extension, use *.bob.
        // Check only the filename, not the complete path for '.'!
        int sep = file.getName().lastIndexOf('.');
        if (sep < 0)
            file = new File(file.getPath() + ".bob");

        editor.loadModel(file);
    }
}
