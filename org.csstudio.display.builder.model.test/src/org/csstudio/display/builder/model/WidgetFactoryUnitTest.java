/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

/** JUnit test of the {@link WidgetFactory}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetFactoryUnitTest
{
    /** Initialize factory with test widget classes. */
    public static void initializeFactory()
    {
        final WidgetFactory factory = WidgetFactory.getInstance();

        if (factory.getWidgetDescriptior("custom").isPresent())
            return;

        factory.addWidgetType(new WidgetDescriptor("base",
                WidgetCategory.MISC, "Base", "dummy_icon.png",
                "Widget base class, only useful for tests")
        {
            @Override
            public Widget createWidget()
            {
                return new Widget(getType());
            }
        });
        factory.addWidgetType(new WidgetDescriptor("custom",
                WidgetCategory.GRAPHIC, "Custom", "dummy_icon.png",
                "Custom Widget, has a few additional properties",
                Arrays.asList("older_custom1", "old_custom2"))
        {
            @Override
            public Widget createWidget()
            {
                return new CustomWidget();
            }
        });
    }

    /** Initialize factory for tests */
    @BeforeClass
    public static void setup()
    {
        initializeFactory();
    }

    /** List widget descriptions */
    @Test
    public void testWidgetDescriptions()
    {
        final Set<WidgetDescriptor> descriptions = WidgetFactory.getInstance().getWidgetDescriptions();
        for (final WidgetDescriptor description : descriptions)
            System.out.println(description.getCategory() + ": " + description);
        assertThat(descriptions.size() > 2, equalTo(true));

        // Widgets should be ordered by category
        final List<WidgetCategory> categories =
            descriptions.stream().map(WidgetDescriptor::getCategory).collect(Collectors.toList());
        int last_ordinal = -1;
        for (final WidgetCategory category : categories)
        {
            if (category.ordinal() < last_ordinal)
                fail("Widgets are not ordered by category");
            last_ordinal = category.ordinal();
        }
    }

    /** Create widgets
     *  @throws Exception on error
     */
    @Test
    public void testWidgetCreation() throws Exception
    {
        Widget widget = WidgetFactory.getInstance().createWidget("base");
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("base"));

        widget = WidgetFactory.getInstance().createWidget("custom");
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("custom"));
    }

    /** Fail on unknown widget
     *  @throws Exception on error
     */
    @Test
    public void testUnknownWidgetType() throws Exception
    {
        try
        {
            WidgetFactory.getInstance().createWidget("bogus");
            fail("Created unknown widget?!");
        }
        catch (final Exception ex)
        {
            assertThat(ex.getMessage().toLowerCase(), containsString("unknown"));
        }
    }

    /** Create widgets via alternate type
     *  @throws Exception on error
     */
    @Test
    public void testAlternateWidgetTypes() throws Exception
    {
        Widget widget = WidgetFactory.getInstance().createWidget("older_custom1");
        assertThat(widget, not(nullValue()));
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("custom"));

        widget = WidgetFactory.getInstance().createWidget("old_custom2");
        assertThat(widget, not(nullValue()));
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("custom"));
    }
}
