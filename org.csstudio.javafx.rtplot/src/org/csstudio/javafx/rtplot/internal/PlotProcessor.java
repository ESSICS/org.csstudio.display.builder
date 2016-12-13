/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.PlotDataSearch;
import org.csstudio.javafx.rtplot.data.ValueRange;
import org.csstudio.javafx.rtplot.internal.undo.AddAnnotationAction;
import org.csstudio.javafx.rtplot.internal.undo.ChangeAxisRanges;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.csstudio.javafx.rtplot.internal.util.Log10;

import javafx.geometry.Point2D;

/** Helper for processing traces of a plot
 *  in a thread pool to avoid blocking UI thread.
 *  @param <XTYPE> Data type of horizontal {@link Axis}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotProcessor<XTYPE extends Comparable<XTYPE>>
{
    final private static ExecutorService thread_pool = ForkJoinPool.commonPool();

    final private Plot<XTYPE> plot;

    /** @param plot Plot on which this processor operates */
    public PlotProcessor(final Plot<XTYPE> plot)
    {
        this.plot = plot;
    }

    /** Submit background job for determining the position range
     *  @param yaxes YAxes that have traces
     *  @return Position range or <code>null</code>
     */
    private Future<AxisRange<XTYPE>> determinePositionRange(final List<YAxisImpl<XTYPE>> yaxes)
    {
        return thread_pool.submit(new Callable<AxisRange<XTYPE>>()
        {
            @Override
            public AxisRange<XTYPE> call() throws Exception
            {
                XTYPE start = null, end = null;
                for (YAxisImpl<XTYPE> yaxis : yaxes)
                {
                    for (Trace<XTYPE> trace : yaxis.getTraces())
                    {
                        final PlotDataProvider<XTYPE> data = trace.getData();
                        data.getLock().lock();
                        try
                        {
                            final int N = data.size();
                            if (N <= 0)
                                continue;
                            // Only checks the first and last position,
                            // assuming all samples are ordered as common
                            // for a time axis or position axis
                            XTYPE pos = data.get(0).getPosition();
                            if (start == null  ||  start.compareTo(pos) > 0)
                                start = pos;
                            if (end == null  ||  end.compareTo(pos) < 0)
                                end = pos;
                            pos = data.get(N-1).getPosition();
                            if (start.compareTo(pos) > 0)
                                start = pos;
                            if (end.compareTo(pos) < 0)
                                end = pos;
                        }
                        finally
                        {
                            data.getLock().unlock();
                        }
                    }
                }
                if (start == null  ||  end == null)
                    return null;
                return new AxisRange<>(start, end);
            }
        });
    }



    /** Submit background job to determine value range
     *  @param data {@link PlotDataProvider} with values
     *  @return {@link Future} to {@link ValueRange}
     */
    public Future<ValueRange> determineValueRange(final PlotDataProvider<XTYPE> data)
    {
        return thread_pool.submit(new Callable<ValueRange>()
        {
            @Override
            public ValueRange call() throws Exception
            {
                double low = Double.MAX_VALUE;
                double high = -Double.MAX_VALUE;
                data.getLock().lock();
                try
                {
                    final int N = data.size();
                    for (int i=0; i<N; ++i)
                    {
                        final PlotDataItem<XTYPE> item = data.get(i);
                        final double value = item.getValue();
                        if (! Double.isFinite(value))
                            continue;
                        if (value < low)
                            low = value;
                        if (value > high)
                            high = value;
                    }
                }
                finally
                {
                    data.getLock().unlock();
                }
                return new ValueRange(low, high);
            }
        });
    }

    /** Submit background job to determine value range
     *  @param axis {@link YAxisImpl} for which to determine range
     *  @return {@link Future} to {@link ValueRange}
     */
    public Future<ValueRange> determineValueRange(final YAxisImpl<XTYPE> axis)
    {
        return thread_pool.submit(new Callable<ValueRange>()
        {
            @Override
            public ValueRange call() throws Exception
            {
                // In parallel, determine range of all traces in this axis
                final List<Future<ValueRange>> ranges = new ArrayList<Future<ValueRange>>();
                for (Trace<XTYPE> trace : axis.getTraces())
                    ranges.add(determineValueRange(trace.getData()));

                // Merge the trace ranges into overall axis range
                double low = Double.MAX_VALUE;
                double high = -Double.MAX_VALUE;
                for (Future<ValueRange> result : ranges)
                {
                    final ValueRange range = result.get();
                    if (range.getLow() < low)
                        low = range.getLow();
                    if (range.getHigh() > high)
                        high = range.getHigh();
                }
                return new ValueRange(low, high);
            }
        });
    }

    /** Round value range up/down to add a little room above & below the exact range.
     *  This results in "locking" to a nice looking range for a while
     *  until a new sample outside of the rounded range is added.
     *
     *  @param low Low and
     *  @param high high end of value range
     *  @return Adjusted range
     */
    public ValueRange roundValueRange(final double low, final double high)
    {
        final double size = Math.abs(high-low);
        if (size > 0)
        {   // Add 2 digits to the 'tight' order of magnitude
            final double order_of_magnitude = Math.floor(Log10.log10(size))-2;
            final double round = Math.pow(10, order_of_magnitude);
            return new ValueRange(Math.floor(low / round) * round,
                                         Math.ceil(high / round) * round);
        }
        else
            return new ValueRange(low, high);
    }

    /** Stagger the range of axes */
    public void stagger()
    {
        thread_pool.execute(() ->
        {
            final double GAP = 0.1;

            // Arrange all axes so they don't overlap by assigning 1/Nth of
            // the vertical range to each one
            // Determine range of each axes' traces in parallel
            final List<YAxisImpl<XTYPE>> y_axes = new ArrayList<>();
            final List<AxisRange<Double>> original_ranges = new ArrayList<>();
            final List<AxisRange<Double>> new_ranges = new ArrayList<>();
            final List<Future<ValueRange>> ranges = new ArrayList<Future<ValueRange>>();
            for (YAxisImpl<XTYPE> axis : plot.getYAxes())
            {
                y_axes.add(axis);
                // As fallback, assume that new range matches old range
                new_ranges.add(axis.getValueRange());
                original_ranges.add(axis.getValueRange());
                ranges.add(determineValueRange(axis));
            }
            final int N = y_axes.size();
            for (int i=0; i<N; ++i)
            {
                final YAxisImpl<XTYPE> axis = y_axes.get(i);
                // Does axis handle itself in another way?
                if (axis.isAutoscale())
                   continue;

                // Fetch range of values on this axis
                final ValueRange axis_range;
                try
                {
                    axis_range = ranges.get(i).get();
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Axis stagger error", ex);
                    continue;
                }

                // Skip axis which for some reason cannot determine its range
                double low = axis_range.getLow();
                double high = axis_range.getHigh();
                if (low > high)
                    continue;
                if (low == high)
                {   // Center trace with constant value (empty range)
                    final double half = Math.abs(low/2);
                    low -= half;
                    high += half;
                }
                if (axis.isLogarithmic())
                {   // Transition into log space
                    low = Log10.log10(low);
                    high = Log10.log10(high);
                }
                double span = high - low;
                // Make some extra space
                low -= GAP*span;
                high += GAP*span;
                span = high-low;

                // With N axes, assign 1/Nth of the vertical plot space to this axis
                // by shifting the span down according to the axis index,
                // using a total of N*range.
                low -= (N-i-1)*span;
                high += i*span;

                final ValueRange rounded = roundValueRange(low, high);
                low = rounded.getLow();
                high = rounded.getHigh();

                if (axis.isLogarithmic())
                {   // Revert from log space
                    low = Log10.pow10(low);
                    high = Log10.pow10(high);
                }

                // Sanity check for empty traces
                if (low < high  &&
                    !Double.isInfinite(low) && !Double.isInfinite(high))
                {
                	final AxisRange<Double> orig = original_ranges.get(i);
                	final boolean normal = orig.getLow() < orig.getHigh();
                	new_ranges.set(i, normal ? new AxisRange<Double>(low, high)
                			                 : new AxisRange<Double>(high, low));
                }
            }

            // 'Stagger' tends to be on-demand,
            // or executed infrequently as archived data arrives after a zoom operation
            // -> Use undo, which also notifies listeners
            plot.getUndoableActionManager().execute(new ChangeAxisRanges<>(plot, Messages.Zoom_Stagger, y_axes, original_ranges, new_ranges));
        });
    }

    /** Compute cursor values for the various traces
     *
     *  <p>Updates the 'selected' sample for each trace,
     *  and sends valid {@link CursorMarker}s to the {@link Plot}
     *
     *  @param cursor_x Pixel location of cursor
     *  @param location Corresponding position on X axis
     *  @param callback Will be called with markers for the cursor location
     */
    public void updateCursorMarkers(final int cursor_x, final XTYPE location, final Consumer<List<CursorMarker>> callback)
    {
        // Run in thread
        thread_pool.execute(() ->
        {
            final List<CursorMarker> markers = new ArrayList<>();
            final PlotDataSearch<XTYPE> search = new PlotDataSearch<>();
            for (YAxisImpl<XTYPE> axis : plot.getYAxes())
                for (TraceImpl<XTYPE> trace : axis.getTraces())
                {
                    final PlotDataProvider<XTYPE> data = trace.getData();
                    final PlotDataItem<XTYPE> sample;
                    data.getLock().lock();
                    try
                    {
                        final int index = search.findSampleLessOrEqual(data, location);
                        sample = index >= 0 ? data.get(index) : null;
                    }
                    finally
                    {
                        data.getLock().unlock();
                    }
                    trace.selectSample(sample);
                    if (sample == null)
                        continue;
                    final double value = sample.getValue();
                    if (Double.isFinite(value)  &&  axis.getValueRange().contains(value))
                    {
                        String label = axis.getTicks().formatDetailed(value);
                        final String units = trace.getUnits();
                        if (! units.isEmpty())
                            label += " " + units;
                        markers.add(new CursorMarker(cursor_x, axis.getScreenCoord(value), GraphicsUtils.convert(trace.getColor()), label));
                    }
                }
            Collections.sort(markers);
            callback.accept(markers);
        });
    }

    /** @param plot Plot where annotation is added
     *  @param trace Trace to which a annotation should be added
     *  @param text Text for the annotation
     */
    public void createAnnotation(final Trace<XTYPE> trace, final String text)
    {
        final AxisPart<XTYPE> x_axis = plot.getXAxis();
        // Run in thread
        thread_pool.execute(() ->
        {
            final AxisRange<Integer> range = x_axis.getScreenRange();
            XTYPE location = x_axis.getValue((range.getLow() + range.getHigh())/2);
            final PlotDataSearch<XTYPE> search = new PlotDataSearch<>();
            final PlotDataProvider<XTYPE> data = trace.getData();
            double value= Double.NaN;
            data.getLock().lock();
            try
            {
                final int index = search.findSampleGreaterOrEqual(data, location);
                if (index >= 0)
                {
                    location = data.get(index).getPosition();
                    value = data.get(index).getValue();
                }
                else
                    location = null;
            }
            finally
            {
                data.getLock().unlock();
            }
            if (location != null)
                plot.getUndoableActionManager().execute(
                    new AddAnnotationAction<XTYPE>(plot,
                                                   new AnnotationImpl<XTYPE>(false, trace, location, value,
                                                                             new Point2D(20, -20),
                                                                             text)));
        });
    }

    public void updateAnnotation(final AnnotationImpl<XTYPE> annotation, final XTYPE location)
    {
        // Run in thread
        thread_pool.execute(() ->
        {
            final PlotDataSearch<XTYPE> search = new PlotDataSearch<>();
            final PlotDataProvider<XTYPE> data = annotation.getTrace().getData();
            XTYPE position;
            double value;
            data.getLock().lock();
            try
            {
                final int index = search.findSampleLessOrEqual(data, location);
                if (index < 0)
                    return;
                position = data.get(index).getPosition();
                value = data.get(index).getValue();
            }
            finally
            {
                data.getLock().unlock();
            }
            plot.updateAnnotation(annotation, position, value, annotation.getOffset());
        });
    }

    /** Perform autoscale for all axes that are marked as such */
    public void autoscale()
    {
        // Determine range of each axes' traces in parallel
        final List<YAxisImpl<XTYPE>> all_y_axes = plot.getYAxes();
        final List<YAxisImpl<XTYPE>> y_axes = new ArrayList<>();
        final List<Future<ValueRange>> ranges = new ArrayList<Future<ValueRange>>();
        for (YAxisImpl<XTYPE> axis : all_y_axes)
            if (axis.isAutoscale())
            {
                y_axes.add(axis);
                ranges.add(determineValueRange(axis));
            }
        // If X axis is auto-scale, schedule fetching its range
        final Future<AxisRange<XTYPE>> pos_range = plot.getXAxis().isAutoscale()
            ? determinePositionRange(all_y_axes)
            : null;

        final int N = y_axes.size();
        for (int i=0; i<N; ++i)
        {
            final YAxisImpl<XTYPE> axis = y_axes.get(i);
            try
            {
                final ValueRange new_range = ranges.get(i).get();
                double low = new_range.getLow(), high = new_range.getHigh();
                if (low > high)
                    continue;
                if (low == high)
                {   // Center trace with constant value (empty range)
                    final double half = Math.abs(low/2);
                    low -= half;
                    high += half;
                }
                if (axis.isLogarithmic())
                {   // Perform adjustment in log space.
                    // But first, refuse to deal with <= 0
                    if (low <= 0.0)
                        low = 1;
                    if (high <= low)
                        high = 100;
                    low = Log10.log10(low);
                    high = Log10.log10(high);
                }
                final ValueRange rounded = roundValueRange(low, high);
                low = rounded.getLow();
                high = rounded.getHigh();
                if (axis.isLogarithmic())
                {
                    low = Log10.pow10(low);
                    high = Log10.pow10(high);
                }
                // Autoscale happens 'all the time'.
                // Do not use undo, but notify listeners.
                if (low != high)
                {
                	final AxisRange<Double> orig = axis.getValueRange();
                	final boolean normal = orig.getLow() < orig.getHigh();
                	final boolean changed = normal ? axis.setValueRange(low, high)
        										   : axis.setValueRange(high, low);
        			if (changed)
                        plot.fireYAxisChange(axis);
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Axis autorange error for " + axis, ex);
            }
        }

        if (pos_range == null)
            return;
        try
        {
            final AxisRange<XTYPE> range = pos_range.get();
            if (range == null)
                return;
            plot.getXAxis().setValueRange(range.getLow(), range.getHigh());
            plot.fireXAxisChange();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Axis autorange error for " + plot.getXAxis(), ex);
        }
    }
}
