/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import java.io.InputStream;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.WidgetClassSupport;
import org.csstudio.display.builder.model.util.ModelResourceUtil;

/** Helper for loading a display model
 *
 *  <p>Resolves display path relative to parent display,
 *  then loads the model,
 *  updates the model's input file information
 *  and applies the class definitions (except for *.bcf files).
 *
 *  @author Kay Kasemir
 */
public class ModelLoader
{
    /** Load model, resolved relative to parent, with classes applied (except for *.bcf itself)
     *
     *  @param parent_display Path to a 'parent' file, may be <code>null</code>
     *  @param display_file Model file
     *  @return {@link DisplayModel}
     *  @throws Exception on error
     */
    public static DisplayModel loadModel(final String parent_display, final String display_file) throws Exception
    {
        final String resolved_name = ModelResourceUtil.resolveResource(parent_display, display_file);
        return loadModel(resolved_name);
    }

    /** Load model, with classes applied (except for *.bcf itself)
     *
     *  @param display_file Model file
     *  @return {@link DisplayModel}
     *  @throws Exception on error
     */
    public static DisplayModel loadModel(final String display_file) throws Exception
    {
        return loadModel(ModelResourceUtil.openResourceStream(display_file), display_file);
    }


    /** Load model, with classes applied (except for *.bcf itself)
    *
    *  @param display_file Model file
    *  @return {@link DisplayModel}
    *  @throws Exception on error
    */
   public static DisplayModel loadModel(final InputStream stream, final String display_path) throws Exception
   {
       final ModelReader reader = new ModelReader(stream);
       final DisplayModel model = reader.readModel();
       model.setUserData(DisplayModel.USER_DATA_INPUT_FILE, display_path);

       // Models from version 2 on support classes
       if (reader.getVersion().getMajor() >= 2  &&
           !display_path.endsWith(WidgetClassSupport.FILE_EXTENSION))
       {
           final WidgetClassSupport classes = WidgetClassesService.getWidgetClasses();
           if (classes != null)
               classes.apply(model);
       }
       return model;
  }
}
