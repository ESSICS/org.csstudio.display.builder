/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.internal.AnnotationImpl;
import org.csstudio.javafx.rtplot.internal.MouseMode;
import org.csstudio.javafx.rtplot.internal.Plot;
import org.csstudio.javafx.rtplot.internal.ToolbarHandler;
import org.csstudio.javafx.rtplot.internal.TraceImpl;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** Real-time plot
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RTPlot<XTYPE extends Comparable<XTYPE>> extends BorderPane
{
    final protected Plot<XTYPE> plot;
    final protected ToolbarHandler<XTYPE> toolbar;
    private boolean handle_keys = false;

    /** Constructor
     *  @param active Active mode where plot reacts to mouse/keyboard?
     *  @param type Type of X axis
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected RTPlot(final Class<XTYPE> type, final boolean active)
    {
        // To avoid unchecked casts, factory methods for..() would need
        // pass already constructed Plot<T> and Toolbar<T>, where T is set,
        // into constructor.
        if (type == Double.class)
        {
            plot = (Plot) new Plot<Double>(Double.class, active);
            toolbar = (ToolbarHandler) new ToolbarHandler<Double>((RTPlot)this, active);
        }
        else if (type == Instant.class)
        {
            plot = (Plot) new Plot<Instant>(Instant.class, active);
            toolbar = (ToolbarHandler) new ToolbarHandler<Instant>((RTPlot)this, active);
        }
        else
            throw new IllegalArgumentException("Cannot handle " + type.getName());

        // Plot is not directly size-manageable by a layout.
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
            });
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
        else if (event.getCode() == KeyCode.T)
            showToolbar(! isToolbarVisible());
        else if (event.getCode() == KeyCode.C)
            toolbar.toggleCrosshair();
        else if (event.getCode() == KeyCode.L)
            plot.showLegend(! plot.isLegendVisible());
        else if (event.getCode() == KeyCode.S)
            plot.stagger(true);
        else if (event.getCode() == KeyCode.A)
            plot.enableAutoScale();
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

    /** @param listener Listener to add */
    public void addListener(final RTPlotListener<XTYPE> listener)
    {
        plot.addListener(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final RTPlotListener<XTYPE> listener)
    {
        plot.removeListener(listener);
    }

    /** @return Control for the plot, to attach context menu */
    public Plot<XTYPE> getPlot()
    {
        return plot;
    }

    /** @return Control for the plot, to attach context menu */
    public Node getPlotNode()
    {
        return plot;
    }

    /** @param color Background color */
    public void setBackground(final Color color)
    {
        plot.setBackground(GraphicsUtils.convert(Objects.requireNonNull(color)));
    }

    /** @param title Title text */
    public void setTitle(final String title)
    {
        plot.setTitle(title);
    }

    /** @param font Font to use for title */
    public void setTitleFont(final Font font)
    {
        plot.setTitleFont(GraphicsUtils.convert(Objects.requireNonNull(font)));
    }

    /** @param font Font to use for legend */
    public void setLegendFont(final Font font)
    {
        plot.setLegendFont(GraphicsUtils.convert(Objects.requireNonNull(font)));
    }

    /** @return {@link Image} of current plot. Caller must dispose */
    //    public Image getImage()
    //    {
    //            return plot.getImage();
    //    }

    /** @return <code>true</code> if legend is visible */
    public boolean isLegendVisible()
    {
        return plot.isLegendVisible();
    }

    /** @param show <code>true</code> if legend should be displayed */
    public void showLegend(final boolean show)
    {
        if (isLegendVisible() == show)
            return;
        plot.showLegend(show);
        //toggle_legend.updateText();
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
        //toggle_toolbar.updateText();
        plot.fireToolbarChange(show);
    }

    /** Add a custom tool bar button
     *  @param icon Icon {@link Image}
     *  @param tool_tip Tool tip text
     *  @return {@link Button}
     */
    public Button addToolItem(final Object icon, final String tool_tip)
    {
        if (icon instanceof Image) {
            return toolbar.addItem((Image)icon, tool_tip);
        }
        else if (icon instanceof ImageView) {
            return toolbar.addItem((ImageView)icon, tool_tip);
        }
        else {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Try to add tool item with unsupported icon type: ", icon.getClass().getName()); //$NON-NLS-1$
            return null;
        }
    }

    /** @param show Show the cross-hair cursor? */
    public void showCrosshair(final boolean show)
    {
        plot.showCrosshair(show);
    }

    /** @return Is crosshair enabled? */
    public boolean isCrosshairVisible()
    {
        return plot.isCrosshairVisible();
    }

    /** Stagger the range of axes
     *  @param disable_autoscale Disable autoscaling, or keep it as is?
     */
    public void stagger(final boolean disable_autoscale)
    {
        plot.stagger(disable_autoscale);
    }

    /** @param mode New {@link MouseMode}
     *  @throws IllegalArgumentException if mode is internal
     */
    public void setMouseMode(final MouseMode mode)
    {
        plot.setMouseMode(mode);
    }

    /** @return {@link UndoableActionManager} for this plot */
    public UndoableActionManager getUndoableActionManager()
    {
        return plot.getUndoableActionManager();
    }

    /** @return X/Time axis */
    public Axis<XTYPE> getXAxis()
    {
        return plot.getXAxis();
    }

    /** Add another Y axis
     *  @param name
     *  @return Y Axis that was added
     */
    public YAxis<XTYPE> addYAxis(final String name)
    {
        return plot.addYAxis(name);
    }

    /** @return Y axes */
    public List<YAxis<XTYPE>> getYAxes()
    {
        final List<YAxis<XTYPE>> result = new ArrayList<>();
        result.addAll(plot.getYAxes());
        return Collections.unmodifiableList(result);
    }

    /** @param index Index of Y axis to remove */
    public void removeYAxis(final int index)
    {
        plot.removeYAxis(index);
    }

    /** @param name Name, must not be <code>null</code>
     *  @param units Units, may be <code>null</code>
     *  @param data
     *  @param color
     *  @param type
     *  @param width
     *  @param y_axis
     *  @return {@link Trace} that was added
     */
    public Trace<XTYPE> addTrace(final String name, final String units,
            final PlotDataProvider<XTYPE> data,
            final Color color,
            final TraceType type, final int width,
            final PointType point_type, final int size,
            final int y_axis)
    {
        final TraceImpl<XTYPE> trace = new TraceImpl<XTYPE>(name, units, data, color, type, width, point_type, size, y_axis);
        plot.addTrace(trace);
        return trace;
    }

    /** @return Thread-safe, read-only traces of the plot */
    public Iterable<Trace<XTYPE>> getTraces()
    {
        return plot.getTraces();
    }

    /** @param trace Trace to move from its current Y axis
     *  @param new_y_axis Index of new Y Axis
     */
    public void moveTrace(final Trace<XTYPE> trace, final int new_y_axis)
    {
        plot.moveTrace((TraceImpl<XTYPE>)trace, new_y_axis);
    }

    /** @param trace Trace to remove */
    public void removeTrace(final Trace<XTYPE> trace)
    {
        plot.removeTrace(trace);
    }

    /** Update the dormant time between updates
     *  @param dormant_time How long throttle remains dormant after a trigger
     *  @param unit Units for the dormant period
     */
    public void setUpdateThrottle(final long dormant_time, final TimeUnit unit)
    {
        plot.setUpdateThrottle(dormant_time, unit);
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

    /** @param trace Trace to which an annotation should be added
     *  @param text Text for the annotation
     */
    public void addAnnotation(final Trace<XTYPE> trace, final String text)
    {
        plot.addAnnotation(trace, text);
    }

    /** @param annotation Annotation to add */
    public void addAnnotation(final Annotation<XTYPE> annotation)
    {
        plot.addAnnotation(annotation);
    }

    /** @return Current {@link AnnotationImpl}s */
    public List<Annotation<XTYPE>> getAnnotations()
    {
        return Collections.unmodifiableList(plot.getAnnotations());
    }

    /** Update text of annotation
     *  @param annotation {@link Annotation} to update.
     *         Must be an existing annotation obtained from <code>getAnnotations()</code>
     *  @param text New text
     *  @throws IllegalArgumentException if annotation is unknown
     */
    public void updateAnnotation(final Annotation<XTYPE> annotation, final String text)
    {
        plot.updateAnnotation(annotation, text);
    }

    /** @param annotation Annotation to remove */
    public void removeAnnotation(final Annotation<XTYPE> annotation)
    {
        plot.removeAnnotation(annotation);
    }

    /** Should be invoked when plot no longer used to release resources */
    public void dispose()
    {
        plot.dispose();
    }
}
