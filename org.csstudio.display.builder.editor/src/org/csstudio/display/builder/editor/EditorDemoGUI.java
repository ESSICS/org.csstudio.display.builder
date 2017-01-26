/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.logging.Level;

import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.editor.actions.LoadModelAction;
import org.csstudio.display.builder.editor.actions.SaveModelAction;
import org.csstudio.display.builder.editor.properties.PropertyPanel;
import org.csstudio.display.builder.editor.tracker.SelectedWidgetUITracker;
import org.csstudio.display.builder.editor.tree.WidgetTree;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.util.ResourceUtil;
import org.csstudio.display.builder.util.undo.UndoableActionManager;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** All the editor components for standalone test
 *
 *  <pre>
 *  Toolbar
 *  ------------------------------------------------
 *  WidgetTree | Editor (w/ palette) | PropertyPanel
 *  ------------------------------------------------
 *  Status
 *  </pre>
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class EditorDemoGUI
{
    private volatile File file = null;

    private final JFXRepresentation toolkit;

    private DisplayEditor editor;

    private WidgetTree tree;

    private PropertyPanel property_panel;

    final EventHandler<KeyEvent> key_handler = (event) ->
    {
        final KeyCode code = event.getCode();
        if (event.isControlDown()  &&  code == KeyCode.Z)
            editor.getUndoableActionManager().undoLast();
        else if (event.isControlDown()  &&  code == KeyCode.Y)
            editor.getUndoableActionManager().redoLast();
        else if (event.isControlDown()  &&  code == KeyCode.X)
            editor.cutToClipboard();
        else if (event.isControlDown()  &&  code == KeyCode.C)
            editor.copyToClipboard();
        else if (event.isControlDown()  &&  code == KeyCode.V)
        {   // Pasting somewhere in upper left corner
            final Random random = new Random();
            editor.pasteFromClipboard(random.nextInt(100), random.nextInt(100));
        }
        else // Pass on, don't consume
            return;
        event.consume();
    };

    public EditorDemoGUI(final Stage stage)
    {
        toolkit = new JFXRepresentation(true);
        createElements(stage);
    }

    private void createElements(final Stage stage)
    {
        editor = new DisplayEditor(toolkit, 50);

        final ToolBar toolbar = createToolbar(editor.getSelectedWidgetUITracker(),
                editor.getWidgetSelectionHandler(),
                editor.getUndoableActionManager());

        tree = new WidgetTree(editor.getWidgetSelectionHandler());

        property_panel = new PropertyPanel(editor);

        final Label status = new Label("Status");

        final SplitPane center = new SplitPane();
        final Node widgetsTree = tree.create();
        final Label widgetsHeader = new Label("Widgets");

        widgetsHeader.setMaxWidth(Double.MAX_VALUE);
        widgetsHeader.getStyleClass().add("header");

        ((VBox) widgetsTree).getChildren().add(0, widgetsHeader);

        final Label propertiesHeader = new Label("Properties");

        propertiesHeader.setMaxWidth(Double.MAX_VALUE);
        propertiesHeader.getStyleClass().add("header");

        final VBox propertiesBox = new VBox(propertiesHeader, property_panel);

        center.getItems().addAll(widgetsTree, editor.create(), propertiesBox);
        center.setDividerPositions(0.2, 0.8);

        final BorderPane toolbar_center_status = new BorderPane();
        toolbar_center_status.setTop(toolbar);
        toolbar_center_status.setCenter(center);
        toolbar_center_status.setBottom(status);
        BorderPane.setAlignment(center, Pos.TOP_LEFT);

        toolbar_center_status.addEventFilter(KeyEvent.KEY_PRESSED, key_handler);

        stage.setTitle("Editor");
        stage.setWidth(1200);
        stage.setHeight(600);
        final Scene scene = new Scene(toolbar_center_status, 1200, 600);
        stage.setScene(scene);
        EditorUtil.setSceneStyle(scene);

        // If ScenicView.jar is added to classpath, open it here
        //ScenicView.show(scene);

        stage.show();
    }

    private ToolBar createToolbar(final SelectedWidgetUITracker selection_tracker,
            final WidgetSelectionHandler selection_handler,
            final UndoableActionManager undo)
    {
        final Button debug = new Button("Debug");
        debug.setOnAction(event -> editor.debug());

        final Button undo_button = createButton(ActionDescription.UNDO);
        final Button redo_button = createButton(ActionDescription.REDO);
        undo_button.setDisable(true);
        redo_button.setDisable(true);
        undo.addListener((to_undo, to_redo) ->
        {
            undo_button.setDisable(to_undo == null);
            redo_button.setDisable(to_redo == null);
        });

        final Button back_button = createButton(ActionDescription.TO_BACK);
        final Button front_button = createButton(ActionDescription.TO_FRONT);

        return new ToolBar(
                createButton(new LoadModelAction(this)),
                createButton(new SaveModelAction(this)),
                new Separator(),
                createToggleButton(ActionDescription.ENABLE_GRID),
                createToggleButton(ActionDescription.ENABLE_SNAP),
                createToggleButton(ActionDescription.ENABLE_COORDS),
                new Separator(),
                back_button,
                front_button,
                new Separator(),
                undo_button,
                redo_button,
                new Separator(),
                debug);
    }


    private Button createButton(final ActionDescription action)
    {
        final Button button = new Button();
        try
        {
            button.setGraphic(new ImageView(new Image(ResourceUtil.openPlatformResource(action.getIconResourcePath()))));
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load action icon", ex);
        }
        button.setTooltip(new Tooltip(action.getToolTip()));
        button.setOnAction(event -> action.run(editor));
        return button;
    }

    private ToggleButton createToggleButton(final ActionDescription action)
    {
        final ToggleButton button = new ToggleButton();
        try
        {
            button.setGraphic(new ImageView(new Image(ResourceUtil.openPlatformResource(action.getIconResourcePath()))));
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load action icon", ex);
        }
        button.setTooltip(new Tooltip(action.getToolTip()));
        button.setSelected(true);
        button.selectedProperty()
              .addListener((observable, old_value, enabled) -> action.run(editor, enabled) );
        return button;
    }


    /** @return Currently edited file */
    public File getFile()
    {
        return file;
    }

    /** Load model from file
     *  @param file File that contains the model
     */
    public void loadModel(final File file)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            try
            {
                final DisplayModel model = ModelLoader.loadModel(new FileInputStream(file), file.getCanonicalPath());
                setModel(model);
                this.file = file;
            }
            catch (final Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot start", ex);
            }
        });
    }

    /** Save model to file
     *  @param file File into which to save the model
     */
    public void saveModelAs(final File file)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            logger.log(Level.FINE, "Save as {0}", file);
            try
            (
                final ModelWriter writer = new ModelWriter(new FileOutputStream(file));
            )
            {
                writer.writeModel(editor.getModel());
                this.file = file;
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot save as " + file, ex);
            }
        });
    }

    private void setModel(final DisplayModel model)
    {
        // Representation needs to be created in UI thread
        toolkit.execute(() ->
        {
            editor.setModel(model);
            tree.setModel(model);
        });
    }

    public void dispose()
    {
        editor.dispose();
    }
}
