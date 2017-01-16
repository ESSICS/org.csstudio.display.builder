/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;

/** Widget that displays a static ellipse
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EllipseWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("ellipse", WidgetCategory.GRAPHIC,
            "Ellipse",
            "platform:/plugin/org.csstudio.display.builder.model/icons/ellipse.png",
            "An ellipse",
            Arrays.asList("org.csstudio.opibuilder.widgets.Ellipse"))
    {
        @Override
        public Widget createWidget()
        {
            return new EllipseWidget();
        }
    };

    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<Boolean> transparent;
    private volatile WidgetProperty<WidgetColor> line_color;
    private volatile WidgetProperty<Integer> line_width;

    public EllipseWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 100, 50);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(line_width = propLineWidth.createProperty(this, 3));
        properties.add(line_color = propLineColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(background = propBackgroundColor.createProperty(this, new WidgetColor(30, 144, 255)));
        properties.add(transparent = propTransparent.createProperty(this, false));
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'transparent' property */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
    }

    /** @return 'line_color' property */
    public WidgetProperty<WidgetColor> propLineColor()
    {
        return line_color;
    }

    /** @return 'line_width' property */
    public WidgetProperty<Integer> propLineWidth()
    {
        return line_width;
    }
}
