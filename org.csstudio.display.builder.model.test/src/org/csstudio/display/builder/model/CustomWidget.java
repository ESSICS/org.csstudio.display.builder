/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.List;

import org.csstudio.display.builder.model.properties.IntegerWidgetProperty;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.widgets.BaseWidget;

/** Custom widget.
 *
 *  <p>Adds 'WIDGET' property that is listed with
 *  base properties of that category.
 *  <p>Has "zero_ten" property.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CustomWidget extends BaseWidget
{
    /** Property: Category Misc, name "zero_ten", Integer 0..10 */
    final public static WidgetPropertyDescriptor<Integer> miscZeroTen =
        new WidgetPropertyDescriptor<Integer>(WidgetPropertyCategory.MISC,
                "zero_ten", "Number 0..10", false)
        {
            @Override
            public WidgetProperty<Integer> createProperty(final Widget widget,
                    final Integer default_value)
            {
                return new IntegerWidgetProperty(this, widget, default_value, 0, 10);
            }
        };

    /** Property: Category Misc, name "zero_ten", Integer 0..10 */
    final public static WidgetPropertyDescriptor<String> widgetQuirk =
        new WidgetPropertyDescriptor<String>(WidgetPropertyCategory.WIDGET,
                "quirk", "Quirk", false)
        {
            @Override
            public WidgetProperty<String> createProperty(final Widget widget,
                    final String default_value)
            {
                return new StringWidgetProperty(this, widget, default_value);
            }
        };

    public CustomWidget()
    {
        super("custom");
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(miscZeroTen.createProperty(this, 5));
        properties.add(widgetQuirk.createProperty(this, "blink"));
    }
}