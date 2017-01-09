/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.java.time.TimestampFormats;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VTable;
import org.diirt.vtype.VType;

/** Utility for handling PVs and their values in scripts.
 *
 *  @author Kay Kasemir
 *  @author Xihui Chen - Original org.csstudio.opibuilder.scriptUtil.*
 */
@SuppressWarnings("nls")
public class PVUtil
{
    private static VType getVType(final RuntimePV pv) throws NullPointerException
    {
        return Objects.requireNonNull(pv.read(), () -> "PV " + pv.getName() + " has no value");
    }

    /** Try to get a 'double' type number from the PV.
     *  @param pv PV
     *  @return Current value.
     *          <code>Double.NaN</code> in case the value type
     *          does not decode into a number,
     *          or PV has no value.
     */
    public static double getDouble(final RuntimePV pv)
    {
        final VType value = pv.read();
        return ValueUtil.getDouble(value);
    }

    /** Try to get an integer from the PV.
     *  @param pv PV
     *  @return Current value as int
     *  @throws NullPointerException if the PV has no value
     */
    public static int getInt(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getInt(getVType(pv));
    }

    /** Try to get a long integer from the PV.
     *  @param pv PV
     *  @return Current value as long
     *  @throws NullPointerException if the PV has no value
     */
    public static long getLong(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getLong(getVType(pv));
    }

    /** Get value of PV as string.
     *  @param pv PV
     *  @return Current value as string
     *  @throws NullPointerException if the PV has no value
     */
    public static String getString(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getString(getVType(pv));
    }

    /** Get value of PV as string.
     *
     *  <p>Optionally, byte arrays can be requested as a (long) string,
     *  instead of "[ 1, 2, 3, .. ]"
     *  @param pv PV
     *  @param byte_array_as_string Decode byte arrays as string?
     *  @return Current value as string
     *  @throws NullPointerException if the PV has no value
     */
    public static String getString(final RuntimePV pv, final boolean byte_array_as_string) throws NullPointerException
    {
        return ValueUtil.getString(getVType(pv), byte_array_as_string);
    }

    /** Get labels for a {@link VEnum} value, or headers for a {@link VTable}.
     *  @param pv the PV.
     *  @return Enum labels or empty array if not enum nor table
     *  @throws NullPointerException if the PV has no value
     */
    public static String[] getLabels(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getLabels(getVType(pv));
    }

    /** Try to get a 'double' type array from a PV.
     *  @param pv the PV.
     *  @return Current value as double[].
     *          Will return single-element array for scalar value,
     *          including <code>{ Double.NaN }</code> in case the value type
     *          does not decode into a number.
     */
    public static double[] getDoubleArray(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getDoubleArray(getVType(pv));
    }

    /** Get the timestamp string of the PV.
     *
     *  <p>Time stamp of the PV is formatted using
     *  the common TimestampFormats helper for
     *  the local time zone.
     *
     *  @param pv PV
     *  @return Timestamp string
     *
     *  @see ValueUtil#getTimestamp(VType) to fetch time stamp for custom formatting
     */
    public static String getTimeString(final RuntimePV pv)
    {
        final Instant time = ValueUtil.getTimestamp(getVType(pv));
        if (time == null)
            return "";
        return TimestampFormats.FULL_FORMAT.format(time);
    }

    /** Get milliseconds since POSIX Epoch, i.e. 1 January 1970 0:00 UTC.
     *  <p>
     *  Note that we always return milliseconds relative to this UTC epoch,
     *  even if the original control system data source might use a different
     *  epoch (example: EPICS uses 1990), because the 1970 epoch is most
     *  compatible with existing programming environments.
     *  @param pv PV
     *  @return milliseconds since 1970.
     */
    public final static long getTimeInMilliseconds(final RuntimePV pv)
    {
        final Instant time = ValueUtil.getTimestamp(getVType(pv));
        if (time == null)
            return 0;
        return time.toEpochMilli();
    }

    /** Get a table from PV
     *
     *  <p>Ideally, the PV holds a {@link VTable},
     *  and the returned data is then the table's data.
     *
     *  <p>If the PV is a scalar, a table with a single cell is returned.
     *  <p>If the PV is an array, a table with one column is returned.
     *
     *  @param pv PV
     *  @return List of rows, where each row contains either String or Number cells
     *  @throws NullPointerException if the PV has no value
     */
    public static List<List<Object>> getTable(final RuntimePV pv) throws NullPointerException
    {
        return ValueUtil.getTable(getVType(pv));
    }

    /** Get a table cell from PV
     *
     *  <p>PV must hold a VTable
     *
     *  @param pv PV
     *  @param row Row index, 0..
     *  @param column Column index, 0..
     *  @return Either String or Number for the cell's value, null if invalid row/column
     *  @throws NullPointerException if the PV has no value
     */
    public static Object getTableCell(final RuntimePV pv, final int row, final int column) throws NullPointerException
    {
        return ValueUtil.getTableCell(getVType(pv), row, column);
    }
}
