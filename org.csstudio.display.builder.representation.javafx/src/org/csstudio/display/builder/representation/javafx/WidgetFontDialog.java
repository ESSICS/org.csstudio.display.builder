/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.NamedWidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;
import org.csstudio.display.builder.model.util.ModelThreadPool;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Dialog for selecting a {@link WidgetFont}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetFontDialog extends Dialog<WidgetFont>
{
    // Sizes are of type double, but comparison of double to set selected element
    // can suffer rounding errors, so use strings
    private final static Collection<String> default_sizes =
        Arrays.asList("8.0", "10.0", "12.0", "14.0", "16.0", "18.0", "22.0", "24.0", "32.0");

    private WidgetFont font;

    private final ListView<NamedWidgetFont> font_names = new ListView<>();
    private final ListView<String> families = new ListView<>();
    private final ListView<WidgetFontStyle> styles = new ListView<>();
    private final TextField size = new TextField();
    private final ListView<String> sizes = new ListView<>();
    private final TextField example = new TextField();

    /** Prevent circular updates */
    private boolean updating = false;

    /** Create dialog
     *  @param initial_font Initial {@link WidgetFont}
     */
    public WidgetFontDialog(final WidgetFont initial_font)
    {
        setTitle(Messages.FontDialog_Title);
        setHeaderText(Messages.FontDialog_Info);

        /* Predefined Fonts    Custom Family   Style  Size
         * [               ]   [            ]  [    ]  ___
         * [               ]   [            ]  [    ] [   ]
         */

        final GridPane content = new GridPane();
        // content.setGridLinesVisible(true); // For debugging
        content.setHgap(10);
        content.setVgap(10);
        content.setPadding(new Insets(10));

        // Get fonts on background thread
        ModelThreadPool.getExecutor().execute(() ->
        {
            final NamedWidgetFonts fonts = WidgetFontService.getFonts();
            final Collection<NamedWidgetFont> values = fonts.getFonts();
            Platform.runLater(() ->
            {
                font_names.getItems().addAll(values);
                if (initial_font instanceof NamedWidgetFont)
                {
                    font_names.getSelectionModel().select((NamedWidgetFont) initial_font);
                    font_names.scrollTo(font_names.getSelectionModel().getSelectedIndex());
                }
            });
        });

        // Uses internal API
        @SuppressWarnings("restriction")
        final List<String> fams = new ArrayList<>(com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().getFamilies());

        // This code uses public API, but AWT's list of font families
        // differs from the one returned by the JFX toolkit.
        // (At least on Linux, SWT has extra fonts)
        // final List<String> fams = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

        families.getItems().addAll(fams);
        styles.getItems().addAll(WidgetFontStyle.values());
        sizes.getItems().addAll(default_sizes);

        example.setText(Messages.FontDialog_ExampleText);

        content.add(new Label(Messages.FontDialog_Predefined), 0, 0);
        content.add(font_names, 0, 1, 1, 2);

        content.add(new Label(Messages.FontDialog_Family), 1, 0, 3, 1);

        content.add(families, 1, 1, 1, 2);

        content.add(new Label(Messages.FontDialog_Style), 2, 0);
        content.add(styles, 2, 1, 1, 2);

        content.add(new Label(Messages.FontDialog_Size), 3, 0);
        size.setPrefColumnCount(3);
        content.add(size, 3, 1);
        content.add(sizes, 3, 2);

        content.add(new Label(Messages.FontDialog_Preview), 0, 3, 4, 1);
        content.add(example, 0, 4, 4, 1);

        GridPane.setVgrow(font_names, Priority.ALWAYS);
        GridPane.setVgrow(families, Priority.ALWAYS);
        GridPane.setVgrow(styles, Priority.ALWAYS);
        GridPane.setVgrow(sizes, Priority.ALWAYS);

        getDialogPane().setContent(content);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // User selects named color -> Update picker, sliders, texts
        font_names.getSelectionModel().selectedItemProperty().addListener((l, old, value) ->
        {
            setFont(value);
        });

        // Double-click exiting named font confirms/closes
        font_names.setOnMouseClicked((event) ->
        {
            if (event.getButton() == MouseButton.PRIMARY  &&
                event.getClickCount() >= 2)
            {
                final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
                ok.fire();
            }
        });

        // Update font as family, style, size is adjusted
        final InvalidationListener update_font = (observable) ->
        {
            if (updating)
                return;
            updating = true;
            try
            {
                final String family = families.getSelectionModel().getSelectedItem();
                final WidgetFontStyle font_style = styles.getSelectionModel().getSelectedItem();
                double font_size;
                try
                {
                    font_size = Double.parseDouble(size.getText());
                }
                catch (NumberFormatException ex)
                {
                    font_size = 1;
                }
                if (font_size < 1)
                    font_size = 1;
                setFont(new WidgetFont(family, font_style, font_size));
            }
            finally
            {
                updating = false;
            }
        };
        families.getSelectionModel().selectedItemProperty().addListener(update_font);
        styles.getSelectionModel().selectedItemProperty().addListener(update_font);
        size.setOnAction(event -> update_font.invalidated(null));
        sizes.getSelectionModel().selectedItemProperty().addListener((l, old, value) ->
        {
            if (value != null)
            {
                size.setText(value);
                update_font.invalidated(null);
            }
        });

        setResizable(true);

        // From http://code.makery.ch/blog/javafx-dialogs-official/,
        // attempts to focus on a field.
        // Will only work if the dialog is opened "soon".
        Platform.runLater(() -> font_names.requestFocus());

        setResultConverter(button ->
        {
            if (button == ButtonType.OK)
                return font;
            return null;
        });

        setFont(initial_font);
    }

    /** Set all display elements to font
     *  @param color WidgetFont
     */
    private void setFont(final WidgetFont font)
    {
        updating = true;
        try
        {
            families.getSelectionModel().select(font.getFamily());
            families.scrollTo(families.getSelectionModel().getSelectedIndex());
            styles.getSelectionModel().select(font.getStyle());
            size.setText(Double.toString(font.getSize()));

            String current_size = String.format("%.1f", font.getSize());
            sizes.getSelectionModel().select(current_size);

            example.setFont(JFXUtil.convert(font));
        }
        finally
        {
            updating = false;
        }
        this.font = font;
    }
}
