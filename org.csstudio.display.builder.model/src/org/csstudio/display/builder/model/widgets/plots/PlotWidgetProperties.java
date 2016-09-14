/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets.plots;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty.Descriptor;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.diirt.vtype.VType;

/** Properties used by plot widgets
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotWidgetProperties
{
    // Custom property types
    public static final WidgetPropertyDescriptor<Boolean> propToolbar =
            CommonWidgetProperties.newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_toolbar", Messages.PlotWidget_ShowToolbar);

    public static final WidgetPropertyDescriptor<Boolean> propLegend =
        CommonWidgetProperties.newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_legend", Messages.PlotWidget_ShowLegend);

    public static final WidgetPropertyDescriptor<String> propTitle =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "title", Messages.PlotWidget_Title);

    public static final WidgetPropertyDescriptor<WidgetFont> propTitleFont =
            new WidgetPropertyDescriptor<WidgetFont>(
                    WidgetPropertyCategory.DISPLAY, "title_font", Messages.PlotWidget_TitleFont)
    {
        @Override
        public WidgetProperty<WidgetFont> createProperty(final Widget widget,
                final WidgetFont font)
        {
            return new FontWidgetProperty(this, widget, font);
        }
    };

    // Elements of the 'axis' structure
    public static final WidgetPropertyDescriptor<Boolean> propAutoscale =
        CommonWidgetProperties.newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "autoscale", Messages.PlotWidget_AutoScale);

    private static final WidgetPropertyDescriptor<Boolean> propLogscale =
        CommonWidgetProperties.newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "log_scale", Messages.PlotWidget_LogScale);

    public static final WidgetPropertyDescriptor<WidgetFont> propScaleFont =
        new WidgetPropertyDescriptor<WidgetFont>(
            WidgetPropertyCategory.DISPLAY, "scale_font", Messages.PlotWidget_ScaleFont)
    {
        @Override
        public WidgetProperty<WidgetFont> createProperty(final Widget widget,
                                                         final WidgetFont font)
        {
            return new FontWidgetProperty(this, widget, font);
        }
    };

    private final static StructuredWidgetProperty.Descriptor propXAxis =
        new Descriptor(WidgetPropertyCategory.BEHAVIOR, "x_axis", Messages.PlotWidget_XAxis);

    private final static StructuredWidgetProperty.Descriptor propYAxis =
        new Descriptor(WidgetPropertyCategory.BEHAVIOR, "y_axis", Messages.PlotWidget_YAxis);

    /** Structure for X axis */ // Also base for Y Axis
    public static class AxisWidgetProperty extends StructuredWidgetProperty
    {
        /** @param widget
         *  @param title_text
         */
        public static AxisWidgetProperty create(final Widget widget, final String title_text)
        {
            return new AxisWidgetProperty(propXAxis, widget,
                  Arrays.asList(propTitle.createProperty(widget, title_text),
                                propAutoscale.createProperty(widget, false),
                                CommonWidgetProperties.propMinimum.createProperty(widget, 0.0),
                                CommonWidgetProperties.propMaximum.createProperty(widget, 100.0),
                                propTitleFont.createProperty(widget, NamedWidgetFonts.DEFAULT_BOLD),
                                propScaleFont.createProperty(widget, NamedWidgetFonts.DEFAULT)));
        }

        protected AxisWidgetProperty(final StructuredWidgetProperty.Descriptor axis_descriptor,
                                     final Widget widget, final List<WidgetProperty<?>> elements)
        {
            super(axis_descriptor, widget, elements);
        }

        public WidgetProperty<String> title()           { return getElement(0); }
        public WidgetProperty<Boolean> autoscale()      { return getElement(1); }
        public WidgetProperty<Double> minimum()         { return getElement(2); }
        public WidgetProperty<Double> maximum()         { return getElement(3); }
        public WidgetProperty<WidgetFont> titleFont()   { return getElement(4); }
        public WidgetProperty<WidgetFont> scaleFont()   { return getElement(5); }
    };

    /** Structure for Y axis */
    public static class YAxisWidgetProperty extends AxisWidgetProperty
    {
        /** @param widget
         *  @param title_text
         */
        public static YAxisWidgetProperty create(final Widget widget, final String title_text)
        {
            return new YAxisWidgetProperty(propYAxis, widget,
                  Arrays.asList(propTitle.createProperty(widget, title_text),
                                propAutoscale.createProperty(widget, false),
                                propLogscale.createProperty(widget, false),
                                CommonWidgetProperties.propMinimum.createProperty(widget, 0.0),
                                CommonWidgetProperties.propMaximum.createProperty(widget, 100.0),
                                propTitleFont.createProperty(widget, NamedWidgetFonts.DEFAULT_BOLD),
                                propScaleFont.createProperty(widget, NamedWidgetFonts.DEFAULT)));
        }

        protected YAxisWidgetProperty(final StructuredWidgetProperty.Descriptor axis_descriptor,
                                      final Widget widget, final List<WidgetProperty<?>> elements)
        {
            super(axis_descriptor, widget, elements);
        }

        public WidgetProperty<Boolean> logscale()       { return getElement(2); }
        @Override
        public WidgetProperty<Double> minimum()         { return getElement(3); }
        @Override
        public WidgetProperty<Double> maximum()         { return getElement(4); }
        @Override
        public WidgetProperty<WidgetFont> titleFont()   { return getElement(5); }
        @Override
        public WidgetProperty<WidgetFont> scaleFont()   { return getElement(6); }
    };

    /** 'y_axes' array */
    public static final ArrayWidgetProperty.Descriptor<YAxisWidgetProperty> propYAxes =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.BEHAVIOR, "y_axes", Messages.PlotWidget_YAxes,
                                             (widget, index) ->
                                             YAxisWidgetProperty.create(widget,
                                                                        index > 0
                                                                        ? Messages.PlotWidget_Y + " " + index
                                                                        : Messages.PlotWidget_Y));

    // Elements of the 'trace' structure
    private static final WidgetPropertyDescriptor<String> traceX =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "x_pv", Messages.PlotWidget_XPV);
    private static final WidgetPropertyDescriptor<String> traceY =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "y_pv", Messages.PlotWidget_YPV);
    private static final WidgetPropertyDescriptor<Integer> traceYAxis =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "axis", Messages.PlotWidget_YAxis);
    private static final WidgetPropertyDescriptor<WidgetColor> traceColor =
        CommonWidgetProperties.newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "color", Messages.PlotWidget_Color);
    private static final WidgetPropertyDescriptor<PlotWidgetPointType> tracePointType =
        new WidgetPropertyDescriptor<PlotWidgetPointType>(
            WidgetPropertyCategory.BEHAVIOR, "point_type", Messages.PlotWidget_PointType)
        {
            @Override
            public WidgetProperty<PlotWidgetPointType> createProperty(final Widget widget,
                                                                      final PlotWidgetPointType default_value)
            {
                return new EnumWidgetProperty<PlotWidgetPointType>(this, widget, default_value);
            }
        };
    private static final WidgetPropertyDescriptor<Integer> tracePointSize =
        CommonWidgetProperties.newIntegerPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "point_size", Messages.PlotWidget_PointSize);
    private static final WidgetPropertyDescriptor<VType> traceXValue =
        CommonWidgetProperties.newRuntimeValue("x_value", Messages.PlotWidget_X);
    private static final WidgetPropertyDescriptor<VType> traceYValue =
        CommonWidgetProperties.newRuntimeValue("y_value", Messages.PlotWidget_Y);
    private final static StructuredWidgetProperty.Descriptor propTrace =
        new Descriptor(WidgetPropertyCategory.BEHAVIOR, "trace", Messages.PlotWidget_Trace);

    /** 'trace' structure */
    public static class TraceWidgetProperty extends StructuredWidgetProperty
    {
        public TraceWidgetProperty(final Widget widget)
        {
            super(propTrace, widget,
                  Arrays.asList(CommonWidgetProperties.propName.createProperty(widget, ""),
                                traceX.createProperty(widget, ""),
                                traceY.createProperty(widget, ""),
                                traceYAxis.createProperty(widget, 0),
                                traceColor.createProperty(widget, new WidgetColor(0, 0, 255)),
                                tracePointType.createProperty(widget, PlotWidgetPointType.NONE),
                                tracePointSize.createProperty(widget, 10),
                                traceXValue.createProperty(widget, null),
                                traceYValue.createProperty(widget, null)  ));
        }
        public WidgetProperty<String> traceName()                   { return getElement(0); }
        public WidgetProperty<String> traceXPV()                    { return getElement(1); }
        public WidgetProperty<String> traceYPV()                    { return getElement(2); }
        public WidgetProperty<Integer> traceYAxis()                 { return getElement(3); }
        public WidgetProperty<WidgetColor> traceColor()             { return getElement(4); }
        public WidgetProperty<PlotWidgetPointType> tracePointType() { return getElement(5); }
        public WidgetProperty<Integer> tracePointSize()             { return getElement(6); }
        public WidgetProperty<VType> traceXValue()                  { return getElement(7); }
        public WidgetProperty<VType> traceYValue()                  { return getElement(8); }
    };

    /** 'traces' array */
    public static final ArrayWidgetProperty.Descriptor<TraceWidgetProperty> propTraces =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.BEHAVIOR, "traces", Messages.PlotWidget_Traces,
                                             (widget, index) ->
                                             new TraceWidgetProperty(widget));
}
