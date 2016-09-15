/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.csstudio.display.builder.model.properties.FormatOption;
import org.diirt.util.array.ListNumber;
import org.diirt.vtype.Display;
import org.diirt.vtype.VDouble;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VImage;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VStringArray;
import org.diirt.vtype.VTable;
import org.diirt.vtype.VType;

/** Utility for formatting data as string.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormatOptionHandler
{
    /** Cached formats for DECIMAL by precision */
    private final static ConcurrentHashMap<Integer, NumberFormat> decimal_formats = new ConcurrentHashMap<>();

    /** Cached formats for EXPONENTIAL by precision */
    private final static ConcurrentHashMap<Integer, NumberFormat> exponential_formats = new ConcurrentHashMap<>();

    /** Format value as string
     *
     *  @param value Value to format
     *  @param option How to format the value
     *  @param precision Precision to use. -1 will try to fetch precision from VType
     *  @param show_units Include units?
     *  @return Formatted value
     */
    public static String format(final VType value, final FormatOption option,
                                int precision, final boolean show_units)
    {
        if (precision < 0)
        {
            if (value instanceof Display)
            {
                final NumberFormat format = ((Display) value).getFormat();
                if (format instanceof DecimalFormat)
                    precision = ((DecimalFormat)format).getMaximumFractionDigits();
            }
            if (precision < 0)
                precision = 2;
        }

        if (value == null)
            return "<null>";
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            final String text = formatNumber(number.getValue(), number, option, precision);
            if (show_units  &&  !number.getUnits().isEmpty())
                return text + " " + number.getUnits();
            return text;
        }
        else if (value instanceof VString)
            return ((VString)value).getValue();
        else if (value instanceof VEnum)
            return formatEnum((VEnum) value, option);
        else if (value instanceof VNumberArray)
        {
            final VNumberArray array = (VNumberArray) value;
            if (option == FormatOption.STRING)
                return getLongString(array);
            final ListNumber data = array.getData();
            if (data.size() <= 0)
                return "[]";
            final StringBuilder buf = new StringBuilder("[");
            buf.append(formatNumber(data.getDouble(0), array, option, precision));
            for (int i=1; i<data.size(); ++i)
            {
                buf.append(", ");
                buf.append(formatNumber(data.getDouble(i), array, option, precision));
            }
            buf.append("]");
            if (show_units  &&  !array.getUnits().isEmpty())
                buf.append(" ").append(array.getUnits());
            return buf.toString();
        }
        else if (value instanceof VEnumArray)
        {
            final List<String> labels = ((VEnumArray)value).getLabels();
            final StringBuilder buf = new StringBuilder("[");
            for (int i=0; i<labels.size(); ++i)
            {
                if (i > 0)
                    buf.append(", ");
                buf.append(labels.get(i));
            }
            buf.append("]");
            return buf.toString();
        }
        else if (value instanceof VStringArray)
            return StringList.join(((VStringArray)value).getData());
        else if (value instanceof VImage)
        {
            final VImage image = (VImage) value;
            return "VImage(" + image.getWidth() + " x " + image.getHeight() + ")";
        }
        else if (value instanceof VTable)
            return formatTable((VTable) value);

        return "<" + value.getClass().getName() + ">";
    }

    private static NumberFormat getDecimalFormat(final int precision)
    {
        return decimal_formats.computeIfAbsent(precision, FormatOptionHandler::createDecimalFormat);
    }

    private static NumberFormat createDecimalFormat(int precision)
    {
        final NumberFormat fmt = NumberFormat.getNumberInstance();
        fmt.setGroupingUsed(false);
        fmt.setMinimumFractionDigits(precision);
        fmt.setMaximumFractionDigits(precision);
        return fmt;
    }

    private static NumberFormat getExponentialFormat(final int precision)
    {
        return exponential_formats.computeIfAbsent(precision, FormatOptionHandler::createExponentialFormat);
    }

    private static NumberFormat createExponentialFormat(final int precision)
    {
        // DecimalFormat needs pattern for exponential notation,
        // there are no factory or configuration methods
        final StringBuilder pattern = new StringBuilder("0");
        if (precision > 0)
            pattern.append('.');
        for (int i=0; i<precision; ++i)
            pattern.append('0');
        pattern.append("E0");
        return new DecimalFormat(pattern.toString());
    }

    private static String formatNumber(final Number value, final Display display,
                                       final FormatOption option, final int precision)
    {
        // Handle invalid numbers
        if (Double.isNaN(value.doubleValue()))
            return "NaN";
        if (Double.isInfinite(value.doubleValue()))
            return Double.toString(value.doubleValue());

        if (option == FormatOption.EXPONENTIAL)
            return getExponentialFormat(precision).format(value);
        if (option == FormatOption.ENGINEERING)
        {   // DecimalFormat "##0." can create 'engineering' notation,
            // but then allows no control over the precision.
            // Using Nick Battam's idea from BOY simplepv.VTypeHelper
            final double num = value.doubleValue();
            if (num == 0.0)
                return formatNumber(value, display, FormatOption.EXPONENTIAL, precision);
            final double log10 = Math.log10(Math.abs(num));
            final int power = 3 * (int) Math.floor(log10 / 3);
            return String.format("%." + precision + "fE%d", num / Math.pow(10, power), power);
        }
        if (option == FormatOption.HEX)
        {
            final StringBuilder buf = new StringBuilder();
            if (precision <= 8)
                buf.append(Integer.toHexString(value.intValue()).toUpperCase());
            else
                buf.append(Long.toHexString(value.longValue()).toUpperCase());
            for (int i=buf.length(); i<precision; ++i)
                buf.insert(0, '0');
            buf.insert(0, "0x");
            return buf.toString();
        }
        if (option == FormatOption.STRING)
            return new String(new byte[] { value.byteValue() });
        if (option == FormatOption.COMPACT)
        {
            final double criteria = Math.abs(value.doubleValue());
            if (criteria > 0.0001  &&  criteria < 10000)
                return formatNumber(value, display, FormatOption.DECIMAL, precision);
            else
                return formatNumber(value, display, FormatOption.EXPONENTIAL, precision);
        }

        // DEFAULT, DECIMAL
        return getDecimalFormat(precision).format(value);
    }

    /** @param value {@link VEnum}
     *  @param option How to format it
     *  @return Either the enum label or the index
     */
    private static String formatEnum(final VEnum value, final FormatOption option)
    {
        if (option == FormatOption.DEFAULT  ||  option == FormatOption.STRING)
            return value.getValue();
        return Integer.toString(value.getIndex());
    }

    /** Format table as text
     *
     *  <p>A single-row table is formatted as "Col1: value, Col2: value, ...".
     *  Otherwise, table is formatted aligned by width of each column:
     *
     *  <pre>
     *  X     Column 2 Names
     *  3     On       Fred
     *  3.14  Off      Jane
     *  3.345 On       Alan
     *  </pre>
     *
     *  @param table {@link VTable}
     *  @return Textual dump of the table
     */
    private static String formatTable(final VTable table)
    {
        final int rows = table.getRowCount(),  cols = table.getColumnCount();
        final String[][] cell = new String[rows+1][cols];
        final int[] width = new int[cols];

        // Determine string for headers and each table cell
        for (int c=0; c<cols; ++c)
        {
            cell[0][c] = table.getColumnName(c);

            // Table columns use ListNumber for ListDouble or an integer-typed ListNumber
            // Otherwise it's a List<?> for String, Instant, alarm, ...
            final Object col = table.getColumnData(c);
            if (col instanceof ListNumber)
            {
                final ListNumber list = (ListNumber) col;
                if (table.getColumnType(c).equals(Integer.TYPE))
                    for (int r=0; r<list.size(); ++r)
                        cell[1+r][c] = Integer.toString(list.getInt(r));
                else
                    for (int r=0; r<list.size(); ++r)
                        cell[1+r][c] = Double.toString(list.getDouble(r));
                for (int r=list.size(); r<rows; ++r)
                    cell[1+r][c] = "";
            }
            else
            {
                final List<?> list = (List<?>) col;
                for (int r=0; r<list.size(); ++r)
                    cell[1+r][c] = Objects.toString(list.get(r)); // handle null
                for (int r=list.size(); r<rows; ++r)
                    cell[1+r][c] = "";
            }

            // Determine maximum width of all cells in this column
            for (int r=0; r<rows+1; ++r)
                width[c] = Math.max(width[c], cell[r][c].length());
        }

        // Format cells into one big string
        final StringBuilder buf = new StringBuilder();
        if (rows == 1)
        {   // Single-row
            for (int c=0; c<cols; ++c)
            {
                if (c > 0)
                    buf.append(", ");
                buf.append(cell[0][c]);
                buf.append(": ");
                buf.append(cell[1][c]);
            }
        }
        else
        {   // Table header followed by rows
            for (int c=0; c<cols; ++c)
            {
                if (c > 0)
                    buf.append(' ');
                buf.append(pad(cell[0][c], width[c]));
            }
            for (int r=1; r<rows+1; ++r)
            {
                buf.append('\n');
                for (int c=0; c<cols; ++c)
                {
                    if (c > 0)
                        buf.append(' ');
                    buf.append(pad(cell[r][c], width[c]));
                }
            }
        }

        return buf.toString();
    }

    /** @param text Text
     *  @param width Desired width
     *  @return Text padded to desired width
     */
    private static String pad(final String text, final int width)
    {
        final StringBuilder buf = new StringBuilder(width);
        buf.append(text);
        for (int p=text.length(); p<width; ++p)
            buf.append(' ');
        return buf.toString();
    }

    /** @param value Array of numbers
     *  @return String based on character for each array element
     */
    private static String getLongString(final VNumberArray value)
    {
        final ListNumber data = value.getData();
        final byte[] bytes = new byte[data.size()];
        // Copy bytes until end or '\0'
        int len = 0;
        while (len<bytes.length)
        {
            final byte b = data.getByte(len);
            if (b == 0)
                break;
            else
                bytes[len++] = b;
        }
        return new String(bytes, 0, len);
    }

    /** Parse a string, presumably as formatted by this class,
     *  into a value suitable for writing back to the PV
     *
     *  @param value Last known value of the PV
     *  @param text Formatted text
     *  @return Object to write to PV for the 'text'
     */
    public static Object parse(final VType value, String text)
    {
        try
        {
            if (value instanceof VNumber)
            {   // Remove trailing text (units or part of units)
                text = text.trim();
                final int sep = text.lastIndexOf(' ');
                if (sep > 0)
                    text = text.substring(0, sep).trim();
                if (value instanceof VDouble)
                    return Double.parseDouble(text);
                return Long.parseLong(text);
            }
            if (value instanceof VEnum)
            {   // Send index for valid enumeration string
                final List<String> labels = ((VEnum)value).getLabels();
                text = text.trim();
                for (int i=0; i<labels.size(); ++i)
                    if (labels.get(i).equals(text))
                        return i;
                // Otherwise write the string
                return text;
            }
            if (value instanceof VNumberArray)
            {
                text = text.trim();
                if (text.startsWith("["))
                    text = text.substring(1);
                if (text.endsWith("]"))
                    text = text.substring(0, text.length()-1);
                final String[] items = text.split(" *, *");
                final double[] array = new double[items.length];
                for (int i=0; i<array.length; ++i)
                    array[i] = Double.parseDouble(items[i].trim());
                return array;
            }
            if (value instanceof VStringArray)
            {
                final List<String> items = StringList.split(text);
                return items.toArray(new String[items.size()]);
            }
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Error parsing value from '" +  text + "', will use as is", ex);
        }
        return text;
    }
}
