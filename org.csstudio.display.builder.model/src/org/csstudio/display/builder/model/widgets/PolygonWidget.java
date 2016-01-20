/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayLineWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayPoints;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.osgi.framework.Version;
import org.w3c.dom.Element;

/** Widget that displays a static area defined by points
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PolygonWidget extends Widget
{
    /** Legacy polygon used 1.0.0 */
    private static final Version version = new Version(2, 0, 0);

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("polygon", WidgetCategory.GRAPHIC,
            "Polygon",
            "platform:/plugin/org.csstudio.display.builder.model/icons/polygon.png",
            "Area defined by points",
            Arrays.asList("org.csstudio.opibuilder.widgets.polygon"))
    {
        @Override
        public Widget createWidget()
        {
            return new PolygonWidget();
        }
    };

    /** Adjust "&lt;points>" in the XML
     *
     *  <p>Legacy coordinates were relative to display or parent group.
     *  New coords. are relative to this widget's x/y position.
     *  @param widget_xml XML for widget where "points" will be adjusted
     *  @throws Exception on error
     */
    static void adjustXMLPoints(final Element widget_xml) throws Exception
    {
        final int x0 = Integer.parseInt(XMLUtil.getChildString(widget_xml, "x").orElse("0"));
        final int y0 = Integer.parseInt(XMLUtil.getChildString(widget_xml, "y").orElse("0"));
        Element xml = XMLUtil.getChildElement(widget_xml, "points");
        if (xml != null)
        {
            for (Element p_xml : XMLUtil.getChildElements(xml, "point"))
            {   // Fetch legacy x, y attributes
                final int x = Integer.parseInt(p_xml.getAttribute("x"));
                final int y = Integer.parseInt(p_xml.getAttribute("y"));
                // Adjust to be relative to x0/y0
                p_xml.setAttribute("x", Integer.toString(x - x0));
                p_xml.setAttribute("y", Integer.toString(y - y0));
            }
        }
    }

    /** Handle legacy XML format */
    static class LegacyWidgetConfigurator extends WidgetConfigurator
    {
        public LegacyWidgetConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public void configureFromXML(final Widget widget, final Element widget_xml) throws Exception
        {
            adjustXMLPoints(widget_xml);
            // Parse updated XML
            super.configureFromXML(widget, widget_xml);
        }
    };

    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<WidgetColor> line_color;
    private volatile WidgetProperty<Integer> line_width;
    private volatile WidgetProperty<Points> points;

    public PolygonWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(background_color = displayBackgroundColor.createProperty(this, new WidgetColor(50, 50, 255)));
        properties.add(line_color = displayLineColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(line_width = displayLineWidth.createProperty(this, 3));
        properties.add(points = displayPoints.createProperty(this, new Points()));
    }

    @Override
    public Version getVersion()
    {
        return version;
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        if (persisted_version.compareTo(version) < 0)
            return new LegacyWidgetConfigurator(persisted_version);
        else
            return super.getConfigurator(persisted_version);
    }

    /** @return Display 'background_color' */
    public WidgetProperty<WidgetColor> displayBackgroundColor()
    {
        return background_color;
    }

    /** @return Display 'line_color' */
    public WidgetProperty<WidgetColor> displayLineColor()
    {
        return line_color;
    }

    /** @return Display 'line_width' */
    public WidgetProperty<Integer> displayLineWidth()
    {
        return line_width;
    }

    /** @return Display 'points' */
    public WidgetProperty<Points> displayPoints()
    {
        return points;
    }
}
