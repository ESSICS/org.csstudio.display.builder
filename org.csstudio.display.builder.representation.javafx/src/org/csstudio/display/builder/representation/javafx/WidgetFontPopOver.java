/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.javafx.PopOver;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;


/**
 * PopOver editor for selecting a {@link WidgetFont}s.
 *
 * @author Kay Kasemir, SNS
 * @author claudiorosati, European Spallation Source ERIC
 */
@SuppressWarnings("nls")
public class WidgetFontPopOver extends PopOver {

    /**
     * Create dialog
     *
     * @param font_prop {@link FontWidgetProperty} used to configure initial values.
     * @param fontChangeConsumer Will be called when user press OK to leave the popover.
     */
    public WidgetFontPopOver ( final FontWidgetProperty font_prop, final Consumer<WidgetFont> fontChangeConsumer ) {

        try {

            URL fxml = WidgetFontPopOver.class.getResource("WidgetFontPopOver.fxml");
            InputStream iStream = WidgetFontPopOver.class.getResourceAsStream("messages.properties");
            ResourceBundle bundle = new PropertyResourceBundle(iStream);
            FXMLLoader fxmlLoader = new FXMLLoader(fxml, bundle);
            Node content = (Node) fxmlLoader.load();

            setContent(content);

            WidgetFontPopOverController controller = fxmlLoader.<WidgetFontPopOverController> getController();

            controller.setInitialConditions(
                this,
                font_prop.getValue(),
                font_prop.getDefaultValue(),
                font_prop.getDescription(),
                fontChangeConsumer
            );

        } catch ( IOException ex ) {
            logger.log(Level.WARNING, "Unable to edit font.", ex);
            setContent(new Label("Unable to edit font."));
        }

    }

}
