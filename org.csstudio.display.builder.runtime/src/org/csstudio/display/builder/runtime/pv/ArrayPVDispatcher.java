/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv;

import static org.csstudio.display.builder.runtime.RuntimePlugin.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.util.VTypeUtil;
import org.diirt.util.array.ArrayDouble;
import org.diirt.util.array.ArrayInt;
import org.diirt.util.array.ListNumber;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VStringArray;
import org.diirt.vtype.VType;

/** Dispatches elements of an array PV to per-element local PVs
 *
 *  <p>Intended use is for the array widget:
 *  Elements of the original array value are sent to separate PVs,
 *  one per array element.
 *  Changing one of the per-element PVs will update the original
 *  array PV.
 *
 *  <p>Treats scalar input PVs as one-element array.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArrayPVDispatcher implements AutoCloseable
{
    /** Listener interface of the {@link ArrayPVDispatcher} */
    @FunctionalInterface
    public static interface Listener
    {
        /** Notification of new/updated per-element PVs.
         *
         *  <p>Sent on initial connection to the array PV,
         *  and also when the array PV changes its size,
         *  i.e. adds or removes elements.
         *
         *  @param element_pvs One scalar PV for each element of the array
         */
        public void arrayChanged(List<RuntimePV> element_pvs);
    }

    private final RuntimePV array_pv;

    private final String basename;

    private final Listener listener;

    private final RuntimePVListener array_listener = new RuntimePVListener()
    {
        @Override
        public void valueChanged(final RuntimePV pv, final VType value)
        {
            try
            {
                dispatchArrayUpdate(value);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot handle array update from " + pv.getName(), ex);
            }
        }

        @Override
        public void disconnected(final RuntimePV pv)
        {
            notifyOfDisconnect();
        }
    };

    /** Per-element PVs are local PVs which sent notification "right away".
     *  This flag is used to ignore such updates whenever the dispatcher
     *  itself is writing to the per-element PVs.
     */
    private volatile boolean ignore_element_updates = false;

    private final RuntimePVListener element_listener = new RuntimePVListener()
    {
        @Override
        public void valueChanged(final RuntimePV pv, final VType value)
        {
            if (ignore_element_updates)
                return;
            try
            {
                updateArrayFromElements();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot update array from elements, triggered by " + pv.getName(), ex);
            }
        }
    };


    private final AtomicReference<List<RuntimePV>> element_pvs = new AtomicReference<>(Collections.emptyList());

    /** Construct dispatcher
     *
     *  @param array_pv PV that will be dispatched into per-element PVs
     *  @param basename Base name used to create per-element PVs.
     *  @see #close()
     */
    public ArrayPVDispatcher(final RuntimePV array_pv, final String basename,
                             final Listener listener)
    {
        this.array_pv = array_pv;
        this.basename = basename;
        this.listener = listener;

        array_pv.addListener(array_listener);
    }

    /** @param value Value update from array */
    private void dispatchArrayUpdate(final VType value) throws Exception
    {
        if (value == null)
            notifyOfDisconnect();
        else
        {
            if (value instanceof VNumberArray)
                dispatchArrayUpdate(((VNumberArray)value).getData());
            else if (value instanceof VEnumArray)
                dispatchArrayUpdate(((VEnumArray)value).getIndexes());
            else if (value instanceof VStringArray)
                dispatchArrayUpdate(((VStringArray)value).getData());
            // Dispatch scalar PVs as one-element arrays
            else if (value instanceof VNumber)
                dispatchArrayUpdate(new ArrayDouble(((VNumber)value).getValue().doubleValue()));
            else if (value instanceof VEnum)
                dispatchArrayUpdate(new ArrayInt(((VEnum)value).getIndex()));
            else if (value instanceof VString)
                dispatchArrayUpdate(Arrays.asList(((VString)value).getValue()));
            else
                throw new Exception("Cannot handle " + value);
        }
    }

    private void notifyOfDisconnect()
    {
        ignore_element_updates = true;
        try
        {
            for (RuntimePV pv : element_pvs.get())
            {
                try
                {
                    pv.write(null);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot notify element PV " + pv.getName() + " of disconnect", ex);
                }
            }
        }
        finally
        {
            ignore_element_updates = false;
        }
    }

    /** @param value Value update from array of numbers or enum indices */
    private void dispatchArrayUpdate(final ListNumber value) throws Exception
    {
        ignore_element_updates = true;
        try
        {
            List<RuntimePV> pvs = element_pvs.get();
            final int N = value.size();
            if (pvs.size() != N)
            {   // Create new element PVs
                pvs = new ArrayList<>(N);
                for (int i=0; i<N; ++i)
                {
                    final double val = value.getDouble(i);
                    final String name = "loc://" + basename + i;
                    final RuntimePV pv = PVFactory.getPV(name);
                    pv.write(val);
                    pvs.add(pv);
                }
                updateElementPVs(pvs);
            }
            else
            {   // Update existing element PVs
                for (int i=0; i<N; ++i)
                    pvs.get(i).write(value.getDouble(i));
            }
        }
        finally
        {
            ignore_element_updates = false;
        }
    }

    /** @param value Value update from array of strings */
    private void dispatchArrayUpdate(final List<String> value) throws Exception
    {
        throw new Exception("Later"); // TODO
    }

    /** Update the array PV with the current value of all element PVs */
    private void updateArrayFromElements() throws Exception
    {
        // TODO Handle string
        final List<RuntimePV> pvs = element_pvs.get();
        final int N = pvs.size();

        if (N == 1)
        {   // Is 'array' really a scalar?
            if (array_pv.read() instanceof VNumber)
            {
                array_pv.write(pvs.get(0).read());
                return;
            }
        }

        final double[] value = new double[N];
        for (int i=0; i<N; ++i)
            value[i] = VTypeUtil.getValueNumber(pvs.get(i).read()).doubleValue();
        array_pv.write(value);
    }

    /** Update per-element PVs.
     *
     *  <p>Disposes old PVs.
     *
     *  <p>Notifies listeners except for special <code>null</code>
     *  parameter used on close
     *
     *  @param new_pvs New per-element PVs
     */
    private void updateElementPVs(final List<RuntimePV> new_pvs)
    {
        final List<RuntimePV> old = element_pvs.getAndSet(new_pvs);
        for (RuntimePV pv : old)
        {
            pv.removeListener(element_listener);
            PVFactory.releasePV(pv);
        }
        if (new_pvs != null)
        {
            for (RuntimePV pv : new_pvs)
                pv.addListener(element_listener);
            listener.arrayChanged(new_pvs);
        }
    }

    /** Must be called when dispatcher is no longer needed.
     *
     *  <p>Releases the per-element PVs
     */
    @Override
    public void close()
    {
        array_pv.removeListener(array_listener);
        updateElementPVs(null);
    }
}
