/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.script;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.csstudio.display.builder.model.properties.FormatOption;
import org.csstudio.display.builder.model.util.FormatOptionHandler;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.diirt.util.array.CollectionNumbers;
import org.diirt.util.array.ListDouble;
import org.diirt.util.array.ListInt;
import org.diirt.util.array.ListNumber;
import org.diirt.vtype.Time;
import org.diirt.vtype.VByteArray;
import org.diirt.vtype.VDoubleArray;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VStringArray;
import org.diirt.vtype.VTable;
import org.diirt.vtype.VType;

/** Utility for handling Values of PVs in scripts.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ValueUtil
{
    /** Try to get a 'double' type number from a value.
     *  @param value Value of a PV
     *  @return Current value.
     *          <code>Double.NaN</code> in case the value type
     *          does not decode into a number.
     */
    public static double getDouble(final VType value)
    {
        return VTypeUtil.getValueNumber(value).doubleValue();
    }

    /** Try to get an integer from a value.
     *  @param value Value of a PV
     *  @return Current value as int
     */
    public static int getInt(final VType value)
    {
        return VTypeUtil.getValueNumber(value).intValue();
    }

    /** Try to get a long integer from a value.
     *  @param value Value of a PV
     *  @return Current value as long
     */
    public static long getLong(final VType value)
    {
        return VTypeUtil.getValueNumber(value).longValue();
    }

    /** Get value of PV as string.
     *  @param value Value of a PV
     *  @return Current value as string
     */
    public static String getString(final VType value)
    {
        return getString(value, false);
    }

    /** Get value of PV as string.
     *
     *  <p>Optionally, byte arrays can be requested as a (long) string,
     *  instead of "[ 1, 2, 3, .. ]"
     *  @param value Value of a PV
     *  @param byte_array_as_string Decode byte arrays as string?
     *  @return Current value as string
     */
    public static String getString(final VType value, final boolean byte_array_as_string) throws NullPointerException
    {
        final FormatOption option = value instanceof VByteArray
                                  ? FormatOption.STRING
                                  : FormatOption.DEFAULT;
        return FormatOptionHandler.format(value, option, 0, true);
    }

    /** Get labels for a {@link VEnum} value, or headers for a {@link VTable}.
     *  @param value Value of a PV
     *  @return Enum labels or empty array if not enum nor table
     */
    public static String[] getLabels(final VType value)
    {
        if (value instanceof VEnum)
        {
            final List<String> labels = ((VEnum) value).getLabels();
            return labels.toArray(new String[labels.size()]);
        }
        if (value instanceof VTable)
        {
            final VTable table = (VTable) value;
            final int num = table.getColumnCount();
            final String[] headers = new String[num];
            for (int i=0; i<num; ++i)
                headers[i] = table.getColumnName(i);
            return headers;
        }
        return new String[0];
    }

    /** Try to get a 'double' type array from a value.
     *  @param value Value of a PV
     *  @return Current value as double[].
     *          Will return single-element array for scalar value,
     *          including <code>{ Double.NaN }</code> in case the value type
     *          does not decode into a number.
     */
    public static double[] getDoubleArray(final VType value)
    {
        if (value instanceof VNumberArray)
        {
            final ListNumber list = ((VNumberArray) value).getData();
            final Object wrapped = CollectionNumbers.wrappedArray(list);
            if (wrapped instanceof double[])
                return (double[]) wrapped;

            final double[] result = new double[list.size()];
            for (int i = 0; i < result.length; i++)
                result[i] = list.getDouble(i);
            return result;
        }
        return new double[] { getDouble(value) };
    }

    /** Try to get a 'long' type array from a value.
     *  @param value Value of a PV
     *  @return Current value as long[].
     *          Will return single-element array for scalar value.
     */
    public static long[] getLongArray(final VType value)
    {
        if (value instanceof VNumberArray)
        {
            final ListNumber list = ((VNumberArray) value).getData();
            final Object wrapped = CollectionNumbers.wrappedArray(list);
            if (wrapped instanceof long[])
                return (long[]) wrapped;

            final long[] result = new long[list.size()];
            for (int i = 0; i < result.length; i++)
                result[i] = list.getLong(i);
            return result;
        }
        return new long[] { getLong(value) };
    }

    /** Get string array from pv.
     *  @param value Value of a PV
     *  @return String array.
     *          For string array, it's the actual strings.
     *          For numeric arrays, the numbers are formatted as strings.
     *          For enum array, the labels are returned.
     *          For scalar PVs, an array with a single string is returned.
     */
    public final static String[] getStringArray(final VType value)
    {
        if (value instanceof VStringArray)
        {
            final List<String> list = ((VStringArray)value).getData();
            return list.toArray(new String[list.size()]);
        }
        else if (value instanceof VDoubleArray)
        {
            final ListNumber list = ((VNumberArray)value).getData();
            final String[] text = new String[list.size()];
            for (int i=0; i<text.length; ++i)
                text[i] = Double.toString(list.getDouble(i));
            return text;
        }
        else if (value instanceof VNumberArray)
        {
            final ListNumber list = ((VNumberArray)value).getData();
            final String[] text = new String[list.size()];
            for (int i=0; i<text.length; ++i)
                text[i] = Long.toString(list.getLong(i));
            return text;
        }
        else if (value instanceof VEnumArray)
        {
            final List<String> labels = ((VEnumArray)value).getLabels();
            final ListInt list = ((VEnumArray)value).getIndexes();
            final String[] text = new String[list.size()];
            for (int i=0; i<text.length; ++i)
            {
                final int index = list.getInt(i);
                if (index >= 0  &&  index <= labels.size())
                    text[i] = labels.get(index);
                else
                    text[i] = "<" + index + ">";
            }
            return text;
        }
        return new String[] { getString(value) };
    }

    /** Get time stamp of a value.
     *
     *  @param value Value of a PV
     *  @return {@link Instant} or <code>null</code>
     */
    public static Instant getTimestamp(final VType value)
    {
        if (value instanceof Time)
            return ((Time) value).getTimestamp();
        return null;
    }

    /** Get a table from PV
     *
     *  <p>Ideally, the PV holds a {@link VTable},
     *  and the returned data is then the table's data.
     *
     *  <p>If the PV is a scalar, a table with a single cell is returned.
     *  <p>If the PV is an array, a table with one column is returned.
     *
     *  @param value Value of a PV
     *  @return List of rows, where each row contains either String or Number cells
     */
    @SuppressWarnings("rawtypes")
    public static List<List<Object>> getTable(final VType value)
    {
        final List<List<Object>> data = new ArrayList<>();
        if (value instanceof VTable)
        {
            final VTable table = (VTable) value;
            final int rows = table.getRowCount();
            final int cols = table.getColumnCount();
            // Extract 2D string matrix for data
            for (int r=0; r<rows; ++r)
            {
                final List<Object> row = new ArrayList<>(cols);
                for (int c=0; c<cols; ++c)
                {
                    final Object col_data = table.getColumnData(c);
                    if (col_data instanceof List)
                        row.add( Objects.toString(((List)col_data).get(r)) );
                    else if (col_data instanceof ListDouble)
                        row.add( ((ListDouble)col_data).getDouble(r) );
                    else if (col_data instanceof ListNumber)
                        row.add( ((ListNumber)col_data).getLong(r) );
                    else
                        row.add( Objects.toString(col_data) );
                }
                data.add(row);
            }
        }
        else if (value instanceof VNumberArray)
        {
            final ListNumber numbers = ((VNumberArray) value).getData();
            final int num = numbers.size();
            for (int i=0; i<num; ++i)
                data.add(Arrays.asList(numbers.getDouble(i)));
        }
        else if (value instanceof VNumber)
            data.add(Arrays.asList( ((VNumber)value).getValue() ));
        else
            data.add(Arrays.asList( Objects.toString(value) ));

        return data;
    }

    /** Get a table cell from PV
     *
     *  <p>PV must hold a VTable
     *
     *  @param value Value of a PV
     *  @param row Row index, 0..
     *  @param column Column index, 0..
     *  @return Either String or Number for the cell's value, null if invalid row/column
     */
    @SuppressWarnings("rawtypes")
    public static Object getTableCell(final VType value, final int row, final int column)
    {
        if (value instanceof VTable)
        {
            final VTable table = (VTable) value;
            if (column >= table.getColumnCount() ||
                row >= table.getRowCount())
                return null;
            final Object col_data = table.getColumnData(column);
            return getColumnCell(col_data, row);
        }
        else
            return Objects.toString(value);
    }
    
    private static Object getColumnCell(final Object col_data, final int row)
    {
        if (col_data instanceof List)
            return Objects.toString(((List)col_data).get(row));
        else if (col_data instanceof ListDouble)
            return ((ListDouble)col_data).getDouble(row);
        else if (col_data instanceof ListNumber)
            return ((ListNumber)col_data).getLong(row);
        else
            return Objects.toString(col_data);
    }
    
	/**
	* Get a structure element from a PV by field name.
	* <p>PV should hold a VTable which represents the structure.
	* <p> For nested structure elements represented
	* with slash-separated names, full and partial field names are accepted. For instance, a structure "value"
	* with the definition
	* <pre>
	* structure value
	* 	  structure Foo
	* 		  scalar_t[] a
	* 		  structure Bar
	* 			  scalar_t[] a
	* 			  scalar_t[] x
	* </pre>
	* has the field "x" with full name "Foo/Bar/x", which can be found with "Foo/Bar/x", "Bar/x", or "x".
	* Ambiguous names (like "a" in the example above) will find the first field with a matching name.
	*
	*  @param value Value of a PV (should be a VTable)
	*  @param name Structure element name
	*  @return (???) need doc
	*/
    public static List<Object> getStructureElement(final VType value, final String name)
    {
    	//TODO
    	return Arrays.asList(Objects.toString(value));
    }
    
    /**
     * Get an element from a PV structure by field name and array index. If index is valid,
     * this method is equivalent to getStructureElement(value, name).get(index).
	*  @param value Value of a PV (should be a VTable)
	*  @param name Structure element name
	*  @param index Element index in range [0, n-1], where n is the length of the structure element
	*  @return Either String or Number for the cell's value, null if invalid name/index
     */
    public static Object getStructureElement(final VType value, final String name, final int index)
    {
    	//List<String> names = Arrays.asList(name.split("/"));
        if (value instanceof VTable)
        {
            final VTable table = (VTable) value;
            if (index > table.getRowCount()) return null;
            for (int i = 0; i < table.getColumnCount(); ++i)
            {
            	//A column name "x/yy/z" must match "x/yy/z", "yy/z", and "z",
            	//but NOT "y/z".
            	String colName = table.getColumnName(i);
            	if (colName.endsWith(name) && (colName.length() - name.length() == 0 ||
            			colName.charAt(colName.length() - name.length() - 1) == '/'))
            	{
            		return getColumnCell(table.getColumnData(i), index);
            	}
            }
        }
        return Objects.toString(value);
    }

    /** Create a VTable for Strings
     *  @param headers Table headers
     *  @param columns List of columns, i.e. columns.get(N) is the Nth column
     *  @return VTable
     */
    public static VTable createStringTableFromColumns(final List<String> headers, final List<List<String>> columns)
    {
        return new StringVTable(headers, columns);
    }

    /** Create a VTable for Strings
     *  @param headers Table headers
     *  @param rows List of rows, i.e. rows.get(N) is the Nth row
     *  @return VTable
     */
    public static VTable createStringTableFromRows(final List<String> headers, final List<List<String>> rows)
    {
        final List<List<String>> columns = new ArrayList<>();
        for (int col=0; col<headers.size(); ++col)
        {
            final ArrayList<String> column = new ArrayList<>();
            columns.add(column);
            for (int row=0; row<rows.size(); ++row)
                column.add(rows.get(row).get(col));
        }
        return new StringVTable(headers, columns);
    }
}
