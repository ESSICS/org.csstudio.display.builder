/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.properties.PointsWidgetProperty;
import org.csstudio.display.builder.representation.javafx.PointsDialog;
import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.eclipse.osgi.util.NLS;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

/** Bidirectional binding between a macro property in model and Java FX Node in the property panel
 *  @author Kay Kasemir
 */
public class PointsPropertyBinding
    extends WidgetPropertyBinding<Button, PointsWidgetProperty>

{
    private WidgetPropertyListener<Points> model_listener = (p, o, n) ->
    {
        jfx_node.setText(NLS.bind(Messages.PointCount_Fmt, widget_property.getValue().size()));
    };

    private EventHandler<ActionEvent> actionHandler = event ->
    {
        final PointsDialog dialog = new PointsDialog(widget_property.getValue());
        final Optional<Points> result = dialog.showAndWait();
        if (result.isPresent())
        {
            // TODO Use undo
            widget_property.setValue(result.get());
            // TODO Same for 'other' widgets?
        }
    };

    public PointsPropertyBinding(final UndoableActionManager undo,
                                 final Button field,
                                 final PointsWidgetProperty widget_property,
                                 final List<Widget> other)
    {
        super(undo, field, widget_property, other);
    }

    @Override
    public void bind()
    {
        widget_property.addPropertyListener(model_listener);
        jfx_node.setOnAction(actionHandler);
        model_listener.propertyChanged(widget_property, null,  null);
    }

    @Override
    public void unbind()
    {
        jfx_node.setOnAction(null);
        widget_property.removePropertyListener(model_listener);
    }
}