/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHorizontalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propRotationStep;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propText;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propVerticalAlignment;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWrapWords;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.osgi.framework.Version;

/** Widget that displays a static text
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LabelWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("label", WidgetCategory.GRAPHIC,
            "Label",
            "platform:/plugin/org.csstudio.display.builder.model/icons/label.png",
            "Label displays one or more lines of text",
            Arrays.asList("org.csstudio.opibuilder.widgets.Label"))
    {
        @Override
        public Widget createWidget()
        {
            return new LabelWidget();
        }
    };

    private volatile WidgetProperty<String> text;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<Boolean> transparent;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<HorizontalAlignment> horizontal_alignment;
    private volatile WidgetProperty<VerticalAlignment> vertical_alignment;
    private volatile WidgetProperty<RotationStep> rotation_step;
    private volatile WidgetProperty<Boolean> wrap_words;

    public LabelWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(font = propFont.createProperty(this, NamedWidgetFonts.DEFAULT));
        properties.add(text = propText.createProperty(this, Messages.LabelWidget_Text));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(transparent = propTransparent.createProperty(this, true));
        properties.add(horizontal_alignment = propHorizontalAlignment.createProperty(this, HorizontalAlignment.LEFT));
        properties.add(vertical_alignment = propVerticalAlignment.createProperty(this, VerticalAlignment.TOP));
        properties.add(rotation_step = propRotationStep.createProperty(this, RotationStep.NONE));
        properties.add(wrap_words = propWrapWords.createProperty(this, true));
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        if (persisted_version.getMajor() < 2)
        {   // Default used to be 'middle'
            vertical_alignment.setValue(VerticalAlignment.MIDDLE);
        }
        return super.getConfigurator(persisted_version);
    }

    /** @return 'text' property */
    public WidgetProperty<String> propText()
    {
        return text;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return Display 'transparent' */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'horizontal_alignment' property */
    public WidgetProperty<HorizontalAlignment> propHorizontalAlignment()
    {
        return horizontal_alignment;
    }

    /** @return 'vertical_alignment' property */
    public WidgetProperty<VerticalAlignment> propVerticalAlignment()
    {
        return vertical_alignment;
    }

    /** @return 'rotation_step' property */
    public WidgetProperty<RotationStep> propRotationStep()
    {
        return rotation_step;
    }

    /** @return 'wrap_words' property */
    public WidgetProperty<Boolean> propWrapWords()
    {
        return wrap_words;
    }
}
