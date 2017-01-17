/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.bobwidget;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMacros;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.macros.Macros;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.osgi.framework.Version;
import org.w3c.dom.Element;

/** Model for persisting data browser widget configuration.
 *
 *  For the OPI, it holds the Data Browser config file name.
 *  For the Data Browser, it holds the {@link DataBrowserModel}.
 *
 *  @author Jaka Bobnar - Original selection value PV support
 *  @author Kay Kasemir
 *  @author Megan Grodowitz - Databrowser 3 ported from 2
 */
@SuppressWarnings("nls")
public class DataBrowserWidget extends VisibleWidget
{
    /** Model with data to display */
    private volatile Model model = new Model();

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("databrowser", WidgetCategory.PLOT,
                    "Data Browser",
                    "platform:/plugin/org.csstudio.trends.databrowser3.bobwidget/icons/databrowser.png",
                    "Embedded Data Brower",
                    Arrays.asList("org.csstudio.trends.databrowser.opiwidget"))
    {
        @Override
        public Widget createWidget()
        {
            return new DataBrowserWidget();
        }
    };

    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            final DataBrowserWidget dbwidget = (DataBrowserWidget) widget;

            // Legacy used 'filename' instead of 'name'
            final Element el = XMLUtil.getChildElement(xml, "filename");
            if (el != null)
                dbwidget.filename.setValue(XMLUtil.getString(el));

            return true;
        }
    }

    public static final WidgetPropertyDescriptor<Boolean> propShowToolbar =
        CommonWidgetProperties.newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_toolbar", Messages.PlotWidget_ShowToolbar);

    public static final WidgetPropertyDescriptor<String> propSelectionValuePV =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "selection_value_pv", Messages.PlotWidget_SelectionValuePV);

    private volatile WidgetProperty<Boolean> show_toolbar;
    private volatile WidgetProperty<String> filename;
    private volatile WidgetProperty<Macros> macros;
    private volatile WidgetProperty<String> selection_value_pv;

    public DataBrowserWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 200, 200);
        model.setMacros(this.getMacrosOrProperties());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(filename = propFile.createProperty(this, ""));
        properties.add(show_toolbar = propShowToolbar.createProperty(this, true));
        properties.add(macros = propMacros.createProperty(this, new Macros()));
        properties.add(selection_value_pv = propSelectionValuePV.createProperty(this, ""));
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    /**
     * Databrowser widget extends parent macros
     *
     * @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        final Macros my_macros = propMacros().getValue();
        return Macros.merge(base, my_macros);
    }


    /** @return 'macros' property */
    public WidgetProperty<Macros> propMacros()
    {
        return macros;
    }

    /** @return 'file' property */
    public WidgetProperty<String> propFile()
    {
        return filename;
    }

    /** @return 'show_toolbar' property */
    public WidgetProperty<Boolean> propShowToolbar()
    {
        return show_toolbar;
    }

    /** @return 'selection_value_pv' property */
    public WidgetProperty<String> propSelectionValuePVName()
    {
        return selection_value_pv;
    }

    /** @return {@link Model} of the data browser (samples, ...) */
    public Model getDataBrowserModel()
    {   // TODO Move this to the DataBrowserWidgetRuntime since it's really a runtime aspect
        return model;
    }

    public Model cloneModel()
    {
        // TODO Move to DataBrowserWidgetRuntime
        // XXX: see about copying over live samples from old to new model
        final Model model = new Model();
        model.setMacros(this.getMacrosOrProperties());
        try
        {
            final InputStream input = this.getFileInputStream();
            new XMLPersistence().load(model, input);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create copy of data browser", ex);
        }
        return model;
    }

    public InputStream getFileInputStream(final String base_path)
    {
        final String file_path;
        try
        {
            file_path = this.getExpandedFilename(base_path);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failure resolving image path from base path: " + base_path, ex);
            return null;
        }

        try
        {
            return ModelResourceUtil.openResourceStream(file_path);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failure loading plot file: " + file_path, ex);
            return null;
        }
    }

    public InputStream getFileInputStream()
    {
        return this.getFileInputStream(this.filename.getValue());
    }

    public String getExpandedFilename(String base_path) throws Exception
    {
        // expand macros in the file name
        final String expanded_path = MacroHandler.replace(this.getMacrosOrProperties(), base_path);
        // Resolve new image file relative to the source widget model (not 'top'!)
        // Get the display model from the widget tied to this representation
        final DisplayModel widget_model = this.getDisplayModel();
        // Resolve the path using the parent model file path
        return ModelResourceUtil.resolveResource(widget_model, expanded_path);
    }

    public String getExpandedFilename() throws Exception
    {
        return getExpandedFilename(this.filename.getValue());
    }

    @Override
    public String toString()
    {
        return "DataBrowserWidgetModel: " + filename;
    }
}
