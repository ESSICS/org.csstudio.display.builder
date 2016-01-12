/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;
import org.diirt.util.array.ListNumber;

/** Data provider for RTPlot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYVTypeDataProvider implements PlotDataProvider<Double>
{
    final private ReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile ListNumber x_data, y_data;

    public void setData(final ListNumber x_data, final ListNumber y_data) throws Exception
    {
        lock.writeLock().lock();
        try
        {
            if (x_data.size() != y_data.size())
            {
                this.x_data = null;
                this.y_data = null;
                throw new Exception("X/Y data size difference: " + x_data.size() + " vs. " + y_data.size());
            }
            this.x_data = x_data;
            this.y_data = y_data;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Lock getLock()
    {
        return lock.readLock();
    }

    @Override
    public int size()
    {
        return y_data == null ? 0 : y_data.size();
    }

    @Override
    public PlotDataItem<Double> get(final int index)
    {
        return new SimpleDataItem<Double>(x_data.getDouble(index), y_data.getDouble(index));
    }
}
