/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Property panel field for a {@link WidgetColor}
 *  @author Kay Kasemir
 */
public class WidgetColorPropertyField extends HBox
{
    private final Canvas blob = new Canvas(16, 16);
    private final Button button = new Button();

    public WidgetColorPropertyField()
    {
        button.setMaxWidth(Double.MAX_VALUE);

        HBox.setMargin(blob, new Insets(5, 5, 5, 0));
        HBox.setHgrow(button, Priority.ALWAYS);

        setSpacing(5.0);
        getChildren().addAll(blob, button);
    }

    /** @param color Color to display */
    public void setColor(final WidgetColor color)
    {
        final GraphicsContext gc = blob.getGraphicsContext2D();
        gc.setFill(JFXUtil.convert(color));
        gc.fillRect(0, 0, 16, 16);

        button.setText(String.valueOf(color));
    }

    /** @param handler Handler to invoke when user wants to change the color */
    public void setOnAction(EventHandler<ActionEvent> handler)
    {
        button.setOnAction(handler);
    }
}
