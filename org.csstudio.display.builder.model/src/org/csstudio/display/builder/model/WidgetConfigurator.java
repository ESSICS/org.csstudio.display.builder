/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static java.lang.Boolean.parseBoolean;
import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.osgi.framework.Version;
import org.w3c.dom.Element;

/** Configure a widget from XML
 *
 *  <p>Default implementation simply transfers value of XML elements
 *  into widget properties of same name.
 *  Derived classes can translate older XML content.
 *
 *  <p>If widget is registered for multiple alternate type IDs,
 *  each widget will be created and its configurator invoked.
 *  The first one which accepts the XML in
 *  <code>configureFromXML</code> will be used.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetConfigurator
{
    /** Custom {@link WidgetConfigurator} can throw this exception
     *  to trigger re-parsing of the XML config.
     *
     *  <p>{@link ModelReader} will read the XML from the widget's
     *  parent on down again.
     *  This allows a {@link WidgetConfigurator} to update the XML
     *  by for example by replacing the XML for the currently handled
     *  widget or by adding new widgets.
     *
     *  <p>Reconfiguration of the XML is limited to the widget and its siblings,
     *  one cannot change the XML from the parent of the widget on up,
     *  only from the widget and its siblings on and down to child widgets.
     *
     *  <p><b>NOTE:</b>
     *  A re-parsing should only be requested <u>after</u> the XML
     *  has been updated. The re-parse should not trigger another re-parse
     *  for the same reason, resulting in an infinite loop.
     */
    public static class ParseAgainException extends Exception
    {
        private static final long serialVersionUID = 1L;
    };

    /** Version of the XML.
     *
     *  <p>Derived class can use this to decide how to read older XML.
     */
    protected final Version xml_version;

    /**@param xml_version Version of the XML */
    public WidgetConfigurator(final Version xml_version)
    {
        this.xml_version = xml_version;
    }

    /** Configure widget based on data persisted in XML.
     *  @param model_reader {@link ModelReader}
     *  @param widget Widget to configure
     *  @param xml XML for this widget
     *  @return <code>true</code> if widget can be configured,
     *          <code>false</code> if XML indicates that an alternate widget should be used
     *  @throws ParseAgainException to trigger re-parsing the XML for this widget and its siblings,
     *                              i.e. for all widgets under this widget's parent.
     *  @throws Exception on error
     *
     */
    public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
            final Element xml) throws ParseAgainException, Exception
    {
        // System.out.println("Reading " + widget + " from saved V" + xml_version);
        configureAllPropertiesFromMatchingXML(model_reader, widget, xml);
        return true;
    }

    /** For each XML element, locate a property of that name and configure it.
     *  @param model_reader {@link ModelReader}
     *  @param widget Widget to configure
     *  @param xml XML for this widget
     *  @throws Exception on error
     */
    protected void configureAllPropertiesFromMatchingXML(final ModelReader model_reader, final Widget widget,
            final Element xml) throws Exception
    {
        for (final Element prop_xml : XMLUtil.getChildElements(xml))
        {
            final String prop_name = prop_xml.getNodeName();
            // Skip unknown properties
            final Optional<WidgetProperty<Object>> prop = widget.checkProperty(prop_name);
            if (! prop.isPresent())
                continue;
            final WidgetProperty<Object> property = prop.get();
            try
            {
                property.readFromXML(model_reader, prop_xml);
                property.useWidgetClass(parseBoolean(prop_xml.getAttribute(XMLTags.USE_CLASS)));
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE,
                           "Error reading widget " + widget + " property " + property.getName() +
                           ", line " + XMLUtil.getLineInfo(prop_xml), ex);
            }
        }
    }
}
