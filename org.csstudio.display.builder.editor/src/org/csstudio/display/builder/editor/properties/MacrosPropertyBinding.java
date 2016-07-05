/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.macros.Macros;
import org.csstudio.display.builder.model.properties.MacrosWidgetProperty;
import org.csstudio.display.builder.representation.javafx.MacrosDialog;
import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.csstudio.javafx.DialogHelper;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

/** Bidirectional binding between a macro property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
public class MacrosPropertyBinding
       extends WidgetPropertyBinding<Button, MacrosWidgetProperty>
{
    /** Update property panel field as model changes */
    private final WidgetPropertyListener<Macros> model_listener = (p, o, n) ->
    {
        jfx_node.setText(widget_property.getValue().toString());
    };

    /** Update model from user input */
    private EventHandler<ActionEvent> action_handler = event ->
    {
        final MacrosDialog dialog = new MacrosDialog(widget_property.getValue());
        DialogHelper.positionDialog(dialog, DialogHelper.getContainer(jfx_node), -200, -300);
        final Optional<Macros> result = dialog.showAndWait();
        if (result.isPresent())
        {
            undo.execute(new SetWidgetPropertyAction<Macros>(widget_property, result.get()));
            for (Widget w : other)
            {
                final MacrosWidgetProperty other_prop = (MacrosWidgetProperty) w.getProperty(widget_property.getName());
                undo.execute(new SetWidgetPropertyAction<Macros>(other_prop, result.get()));
            }
        }
    };

    public MacrosPropertyBinding(final UndoableActionManager undo,
                                 final Button field,
                                 final MacrosWidgetProperty widget_property,
                                 final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(action_handler);
        jfx_node.setText(widget_property.getValue().toString());
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}
