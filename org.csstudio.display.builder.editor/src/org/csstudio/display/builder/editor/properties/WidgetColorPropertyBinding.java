/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.io.IOException;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.ColorWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.representation.javafx.WidgetColorPopOver;
import org.csstudio.display.builder.util.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/** Bidirectional binding between a color property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
public class WidgetColorPropertyBinding
       extends WidgetPropertyBinding<WidgetColorPropertyField, ColorWidgetProperty>
{
    /** Update property panel field as model changes */
    private final WidgetPropertyListener<WidgetColor> model_listener = (p, o, n) ->
    {
        jfx_node.setColor(widget_property.getValue());
    };

    /** Update model from user input */
    private EventHandler<ActionEvent> action_handler = event ->
    {

        try {
            WidgetColorPopOver.getPopOver(widget_property.getDescription()).show(jfx_node.getChildren().get(1));
//            WidgetColorPopOver.getPopOver(widget_property.getDescription()).show(jfx_node);
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }






//        final WidgetColorDialog dialog = new WidgetColorDialog(widget_property.getValue());
//        DialogHelper.positionDialog(dialog, DialogHelper.getContainer(jfx_node), -200, -200);
//        ModalityHack.forDialog(dialog);
//        final Optional<WidgetColor> result = dialog.showAndWait();
//        if (result.isPresent())
//        {
//            undo.execute(new SetWidgetPropertyAction<WidgetColor>(widget_property, result.get()));
//            final String path = widget_property.getPath();
//            for (Widget w : other)
//            {
//                final ColorWidgetProperty other_prop = (ColorWidgetProperty) w.getProperty(path);
//                undo.execute(new SetWidgetPropertyAction<WidgetColor>(other_prop, result.get()));
//            }
//        }
    };

    public WidgetColorPropertyBinding(final UndoableActionManager undo,
                                      final WidgetColorPropertyField field,
                                      final ColorWidgetProperty widget_property,
                                      final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(action_handler);
        jfx_node.setColor(widget_property.getValue());
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}
