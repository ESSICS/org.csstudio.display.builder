/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.util.NamedDaemonPool;

import javafx.scene.Scene;

/** Editor utility
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EditorUtil
{
    private static final ExecutorService executor = NamedDaemonPool.createThreadPool("DisplayEditor");

    /** @return ExecutorService for thread pool meant for editor-related background tasks */
    public static ExecutorService getExecutor()
    {
        return executor;
    }

    public static void setSceneStyle(Scene scene)
    {
        scene.getStylesheets().add(EditorUtil.class.getResource("opieditor.css").toExternalForm());
    }

    /** Load model from file
     *  @param file File that contains the model
     *  @return
     */
    public Future<DisplayModel> loadModel(final File file)
    {
        final Callable<DisplayModel> task = () ->
        {
            final ModelReader reader = new ModelReader(new FileInputStream(file));
            return reader.readModel();
        };
        return executor.submit(task);
    }
}
