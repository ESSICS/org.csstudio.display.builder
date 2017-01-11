/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.internal.ImagePlot;
import org.csstudio.javafx.rtplot.internal.ImageToolbarHandler;
import org.csstudio.javafx.rtplot.internal.MouseMode;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.diirt.util.array.ListNumber;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

/** Real-time plot
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
public class RTImagePlot extends BorderPane
{
    final protected ImagePlot plot;
    final protected ImageToolbarHandler toolbar;
    private boolean handle_keys = false;

    /** Constructor
     *  @param active Active mode where plot reacts to mouse/keyboard?
     *  @param type Type of X axis
     */
    public RTImagePlot(final boolean active)
    {
        plot = new ImagePlot(active);
        toolbar = new ImageToolbarHandler(this);

        // Canvas, i.e. plot, is not directly size-manageable by a layout.
        // --> Let BorderPane resize 'center', then plot binds to is size.
        final Pane center = new Pane(plot);
        final ChangeListener<? super Number> resize_listener = (p, o, n) -> plot.setSize(center.getWidth(), center.getHeight());
        center.widthProperty().addListener(resize_listener);
        center.heightProperty().addListener(resize_listener);
        setCenter(center);
        showToolbar(active);

        if (active)
        {
            addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressed);
            // Need focus to receive key events. Get focus when mouse moves.
            // (tried mouse _entered_, but can then loose focus while mouse still in widget)
            addEventFilter(MouseEvent.MOUSE_MOVED, event ->
            {
                handle_keys = true;
                requestFocus();
            } );
            // Don't want to handle key events when mouse is outside the widget.
            // Cannot 'loose focus', so using flag to ignore them
            addEventFilter(MouseEvent.MOUSE_EXITED, event -> handle_keys = false);
        }
    }

    /** onKeyPressed */
    private void keyPressed(final KeyEvent event)
    {
        if (! handle_keys)
            return;
        if (event.getCode() == KeyCode.Z)
            plot.getUndoableActionManager().undoLast();
        else if (event.getCode() == KeyCode.Y)
            plot.getUndoableActionManager().redoLast();
        else if (event.getCode() == KeyCode.C)
            toolbar.toggleCrosshair();
        else if (event.getCode() == KeyCode.T)
            showToolbar(! isToolbarVisible());
        else if (event.isControlDown())
            toolbar.selectMouseMode(MouseMode.ZOOM_IN);
        else if (event.isAltDown())
            toolbar.selectMouseMode(MouseMode.ZOOM_OUT);
        else if (event.isShiftDown())
            toolbar.selectMouseMode(MouseMode.PAN);
        else
            toolbar.selectMouseMode(MouseMode.NONE);
        event.consume();
    }

    /** @param plot_listener Plot listener */
    public void setListener(final RTImagePlotListener plot_listener)
    {
        plot.setListener(plot_listener);
    }

    /** @return <code>true</code> if toolbar is visible */
    public boolean isToolbarVisible()
    {
        return getTop() != null;
    }

    /** @param show <code>true</code> if toolbar should be displayed */
    public void showToolbar(final boolean show)
    {
        if (isToolbarVisible() == show)
            return;
        plot.removeROITracker();
        if (show)
            setTop(toolbar.getToolBar());
        else
            setTop(null);

        // Force layout to reclaim space used by hidden toolbar,
        // or make room for the visible toolbar
        layoutChildren();
        // XX Hack: Toolbar is garbled, all icons in pile at left end,
        // when shown the first time, i.e. it was hidden when the plot
        // was first shown.
        // Manual fix is to hide and show again.
        // Workaround is to force another layout a little later
        if (show)
            ForkJoinPool.commonPool().submit(() ->
            {
                Thread.sleep(1000);
                Platform.runLater(() -> layoutChildren() );
                return null;
            });
    }

    /** @param mode New {@link MouseMode}
     *  @throws IllegalArgumentException if mode is internal
     */
    public void setMouseMode(final MouseMode mode)
    {
         plot.setMouseMode(mode);
    }

    /** Zoom 'in' or 'out' from center
     *  @param zoom_in Zoom 'in' or 'out'?
     */
    public void zoomInOut(final boolean zoom_in)
    {
        plot.zoomInOut(zoom_in);
    }

    /** @return {@link UndoableActionManager} for this plot */
    public UndoableActionManager getUndoableActionManager()
    {
        return plot.getUndoableActionManager();
    }

    /** @param interpolation How to interpolate from image to screen pixels */
    public void setInterpolation(final Interpolation interpolation)
    {
        plot.setInterpolation(interpolation);
    }

    /** @param autoscale  Auto-scale the color mapping? */
    public void setAutoscale(boolean autoscale)
    {
        plot.setAutoscale(autoscale);
    }

    /** @return X axis */
    public Axis<Double> getXAxis()
    {
        return plot.getXAxis();
    }

    /** @return Y axis */
    public Axis<Double> getYAxis()
    {
        return plot.getYAxis();
    }

    /** Add region of interest
     *  @param name
     *  @param color
     *  @param visible
     *  @param interactive
     *  @return {@link RegionOfInterest}
     */
    public RegionOfInterest addROI(final String name, final javafx.scene.paint.Color color,
                                   final boolean visible, final boolean interactive)
    {
        return plot.addROI(name, color, visible, interactive);
    }

    /** @return Regions of interest */
    public List<RegionOfInterest> getROIs()
    {
        return plot.getROIs();
    }

    /** @param index Index of R.O.I. to remove
     *  @throws IndexOutOfBoundsException
     */
    public void removeROI(final int index)
    {
        plot.removeROI(index);
    }

    /** If there is a ROI tracker, remove it */
    public void removeROITracker()
    {
        plot.removeROITracker();
    }

    /** Request a complete redraw of the plot with new layout */
    @Override
    public void requestLayout()
    {
        plot.requestLayout();
    }

    /** Request a complete redraw of the plot */
    public void requestUpdate()
    {
        plot.requestUpdate();
    }

    /** @param color Background color */
    public void setBackground(final javafx.scene.paint.Color color)
    {
        plot.setBackground(GraphicsUtils.convert(color));
    }

    /** @param color_mapping Function that returns color for value 0.0 .. 1.0 */
    public void setColorMapping(final ColorMappingFunction color_mapping)
    {
        plot.setColorMapping(color_mapping);
    }

    /** @param show Show color map? */
    public void showColorMap(final boolean show)
    {
        plot.showColorMap(show);
    }

    /** @param size Color bar size in pixels */
    public void setColorMapSize(final int size)
    {
        plot.setColorMapSize(size);
    }

    /** @param size Color bar size in pixels */
    public void setColorMapFont(final Font font)
    {
        plot.setColorMapFont(font);
    }

    /** @param show Show crosshair, moved on click?
     *              Or update cursor listener with each mouse move,
     *              not showing a persistent crosshair?
     */
    public void showCrosshair(final boolean show)
    {
        if (plot.isCrosshairVisible() == show)
            return;
        toolbar.showCrosshair(show);
        plot.showCrosshair(show);
    }

    /** @return Is crosshair enabled? */
    public boolean isCrosshairVisible()
    {
        return plot.isCrosshairVisible();
    }

    /** Set location of crosshair
     *  @param x_val
     *  @param y_val
     */
    public void setCrosshairLocation(final double x_val, final double y_val)
    {
        plot.setCrosshairLocation(x_val, y_val);
    }

    /** Set axis range for 'full' image
     *  @param min_x
     *  @param max_x
     *  @param min_y
     *  @param max_y
     */
    public void setAxisRange(final double min_x, final double max_x,
                             final double min_y, final double max_y)
    {
        plot.setAxisRange(min_x, max_x, min_y, max_y);
    }

    /** Set color mapping value range
     *  @param min
     *  @param max
     */
    public void setValueRange(final double min, final double max)
    {
        plot.setValueRange(min, max);
    }

    /** Set the data to display
     *  @param width Number of elements in one 'row' of data
     *  @param height Number of data rows
     *  @param data Image elements, starting in 'top left' corner,
     *              proceeding along the row, then to next rows
     *  @param unsigned Is the data meant to be treated as 'unsigned'
     */
    public void setValue(final int width, final int height, final ListNumber data, final boolean unsigned)
    {
        plot.setValue(width, height, data, unsigned);
    }

    /** Should be invoked when plot no longer used to release resources */
    public void dispose()
    {
    	plot.dispose();
    }
}
