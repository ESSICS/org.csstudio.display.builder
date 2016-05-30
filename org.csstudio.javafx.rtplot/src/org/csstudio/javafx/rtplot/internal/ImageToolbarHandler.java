/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.csstudio.javafx.rtplot.Activator;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.RTImagePlot;
import org.eclipse.osgi.util.NLS;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Tool bar for {@link RTImagePlot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageToolbarHandler
{
    public enum ToolIcons
    {
        ZOOM_IN,
        ZOOM_OUT,
        POINTER,
        UNDO,
        REDO
    };

    final private RTImagePlot plot;

    final private ToolBar toolbar;
    private ToggleButton zoom_in, zoom_out, pointer;

    /** Have any custom items been added? */
    private boolean have_custom_items = false;

    /** Construct tool bar
     *  @param plot {@link RTImagePlot} to control from tool bar
     */
    public ImageToolbarHandler(final RTImagePlot plot)
    {
        this.plot = plot;
        toolbar = new ToolBar();
        makeGUI();
    }

    /** @return The actual toolbar for {@link RTImagePlot} to handle its layout */
    public ToolBar getToolBar()
    {
        return toolbar;
    }

    /** Add a custom tool bar item
     *  @param icon Icon {@link Image}
     *  @param tool_tip Tool tip text
     *  @return {@link ToolItem}
     */
    public Button addItem(final Image icon, final String tool_tip)
    {
        if (!have_custom_items)
        {
            toolbar.getItems().add(new Separator());
            have_custom_items = true;
        }
        final Button item = new Button();
        item.setGraphic(new ImageView(icon));
        item.setTooltip(new Tooltip(tool_tip));
        toolbar.getItems().add(item);
        return item;
    }

    private void makeGUI()
    {
        addMouseModes();
        toolbar.getItems().add(new Separator());
        addUndo();

        // Initially, zooming is selected
        selectMouseMode(zoom_in);
    }

    private void addMouseModes()
    {
        zoom_in = newToggleButton(ToolIcons.ZOOM_IN, Messages.Zoom_In_TT);
        zoom_out = newToggleButton(ToolIcons.ZOOM_OUT, Messages.Zoom_Out_TT);
        pointer = newToggleButton(ToolIcons.POINTER, Messages.Plain_Pointer);

        zoom_in.setOnAction(event ->
        {
            selectMouseMode(zoom_in);
            plot.setMouseMode(MouseMode.ZOOM_IN);
        });
        zoom_out.setOnAction(event ->
        {
            selectMouseMode(zoom_out);
            plot.setMouseMode(MouseMode.ZOOM_OUT);
        });
        pointer.setOnAction(event ->
        {
            selectMouseMode(pointer);
            plot.setMouseMode(MouseMode.NONE);
        });
    }

    private void addUndo()
    {
        final Button undo = newButton(ToolIcons.UNDO, Messages.Undo_TT);
        undo.setOnAction(event -> plot.getUndoableActionManager().undoLast());

        final Button redo = newButton(ToolIcons.REDO, Messages.Redo_TT);
        redo.setOnAction(event -> plot.getUndoableActionManager().redoLast());

        final UndoableActionManager undo_mgr = plot.getUndoableActionManager();
        undo.setDisable(!undo_mgr.canUndo());
        redo.setDisable(!undo_mgr.canRedo());
        undo_mgr.addListener((to_undo, to_redo) ->
        {
        	Platform.runLater(()->
            {
                if (to_undo == null)
                {
                    undo.setDisable(true);
                    undo.setTooltip(new Tooltip(Messages.Undo_TT));
                }
                else
                {
                    undo.setDisable(false);
                    undo.setTooltip(new Tooltip(NLS.bind(Messages.Undo_Fmt_TT, to_undo)));
                }
                if (to_redo == null)
                {
                    redo.setDisable(true);
                    redo.setTooltip(new Tooltip(Messages.Redo_TT));
                }
                else
                {
                    redo.setDisable(false);
                    redo.setTooltip(new Tooltip(NLS.bind(Messages.Redo_Fmt_TT, to_redo)));
                }
            });
        });
    }

    private Button newButton(final ToolIcons icon, final String tool_tip)
    {
    	return (Button) newItem(false, icon, tool_tip);
    }

    private ToggleButton newToggleButton(final ToolIcons icon, final String tool_tip)
    {
    	return (ToggleButton) newItem(true, icon, tool_tip);
    }

    private ButtonBase newItem(final boolean toggle, final ToolIcons icon, final String tool_tip)
    {
    	final ButtonBase item = toggle ? new ToggleButton() : new Button();
		try
		{
			item.setGraphic(new ImageView(Activator.getIcon(icon.name().toLowerCase())));
		}
		catch (Exception ex)
		{
			logger.log(Level.WARNING, "Cannot get icon" + icon, ex);
			item.setText(icon.toString());
		}
        item.setTooltip(new Tooltip(tool_tip));

        toolbar.getItems().add(item);
        return item;
    }

    /** @param mode {@link MouseMode} ZOOM_IN, ZOOM_OUT, PAN or NONE */
    public void selectMouseMode(final MouseMode mode)
    {
        if (mode == MouseMode.ZOOM_IN)
        {
            selectMouseMode(zoom_in);
            plot.setMouseMode(mode);
        }
        else if (mode == MouseMode.ZOOM_OUT)
        {
            selectMouseMode(zoom_out);
            plot.setMouseMode(mode);
        }
        else
        {
            selectMouseMode(pointer);
            plot.setMouseMode(MouseMode.NONE);
        }
    }

    /** @param item Tool item to select, all others will be de-selected */
    private void selectMouseMode(final ToggleButton item)
    {
        for (ToggleButton ti : new ToggleButton[] { zoom_in, zoom_out, pointer })
        	ti.setSelected(ti == item);
    }
}
