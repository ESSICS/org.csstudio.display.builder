/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.model.widgets;


import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPrecision;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.osgi.framework.Version;
import org.w3c.dom.Element;


/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 21 Aug 2017
 */
public class KnobWidget extends WritablePVWidget {

    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor(
        "knob",
        WidgetCategory.CONTROL,
        "Knob",
        "platform:/plugin/org.csstudio.display.builder.model/icons/knob.png",
        "Knob controller that can read/write a numeric PV",
        Arrays.asList(
            "org.csstudio.opibuilder.widgets.knob"
        )
    ) {
        @Override
        public Widget createWidget ( ) {
            return new KnobWidget();
        }
    };

    public static final WidgetPropertyDescriptor<Boolean>     propSyncedKnob     = newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "synced_knob",      Messages.WidgetProperties_SyncedKnob);
    public static final WidgetPropertyDescriptor<Boolean>     propUnitFromPV     = newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "unit_from_pv",     Messages.WidgetProperties_UnitFromPV);
    public static final WidgetPropertyDescriptor<Boolean>     propWriteOnRelease = newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "write_on_release", Messages.WidgetProperties_WriteOnRelease);

    public static final WidgetPropertyDescriptor<WidgetColor> propColorHiHi      = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "color_hihi",       Messages.WidgetProperties_ColorHiHi);
    public static final WidgetPropertyDescriptor<WidgetColor> propColorHigh      = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "color_high",       Messages.WidgetProperties_ColorHigh);
    public static final WidgetPropertyDescriptor<WidgetColor> propColorLoLo      = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "color_lolo",       Messages.WidgetProperties_ColorLoLo);
    public static final WidgetPropertyDescriptor<WidgetColor> propColorLow       = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "color_low",        Messages.WidgetProperties_ColorLow);
    public static final WidgetPropertyDescriptor<WidgetColor> propColorOK        = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "color_ok",         Messages.WidgetProperties_ColorOK);
    public static final WidgetPropertyDescriptor<Boolean>     propExtremaVisible = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "extrema_visible",  Messages.WidgetProperties_ExtremaVisible);
    public static final WidgetPropertyDescriptor<Double>      propLevelHiHi      = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_hihi",       Messages.WidgetProperties_LevelHiHi);
    public static final WidgetPropertyDescriptor<Double>      propLevelHigh      = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_high",       Messages.WidgetProperties_LevelHigh);
    public static final WidgetPropertyDescriptor<Double>      propLevelLoLo      = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_lolo",       Messages.WidgetProperties_LevelLoLo);
    public static final WidgetPropertyDescriptor<Double>      propLevelLow       = newDoublePropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "level_low",        Messages.WidgetProperties_LevelLow);
    public static final WidgetPropertyDescriptor<Boolean>     propShowHiHi       = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_hihi",        Messages.WidgetProperties_ShowHiHi);
    public static final WidgetPropertyDescriptor<Boolean>     propShowHigh       = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_high",        Messages.WidgetProperties_ShowHigh);
    public static final WidgetPropertyDescriptor<Boolean>     propShowLoLo       = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_lolo",        Messages.WidgetProperties_ShowLoLo);
    public static final WidgetPropertyDescriptor<Boolean>     propShowLow        = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "show_low",         Messages.WidgetProperties_ShowLow);
    public static final WidgetPropertyDescriptor<WidgetColor> propTagColor       = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "tag_color",        Messages.WidgetProperties_TagColor);
    public static final WidgetPropertyDescriptor<Boolean>     propTagVisible     = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "tag_visible",      Messages.WidgetProperties_TagVisible);
    public static final WidgetPropertyDescriptor<WidgetColor> propTextColor      = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "text_color",       Messages.WidgetProperties_TextColor);
    public static final WidgetPropertyDescriptor<WidgetColor> propThumbColor     = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "thumb_color",      Messages.WidgetProperties_ThumbColor);
    public static final WidgetPropertyDescriptor<String>      propUnit           = newStringPropertyDescriptor (WidgetPropertyCategory.DISPLAY,  "unit",             Messages.WidgetProperties_Unit);
    public static final WidgetPropertyDescriptor<WidgetColor> propValueColor     = newColorPropertyDescriptor  (WidgetPropertyCategory.DISPLAY,  "value_color",      Messages.WidgetProperties_ValueColor);
    public static final WidgetPropertyDescriptor<Boolean>     propValueVisible   = newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY,  "value_visible",    Messages.WidgetProperties_ValueVisible);

    private volatile WidgetProperty<WidgetColor> background_color;
    private volatile WidgetProperty<WidgetColor> color;
    private volatile WidgetProperty<WidgetColor> color_hihi;
    private volatile WidgetProperty<WidgetColor> color_high;
    private volatile WidgetProperty<WidgetColor> color_lolo;
    private volatile WidgetProperty<WidgetColor> color_low;
    private volatile WidgetProperty<WidgetColor> color_ok;
    private volatile WidgetProperty<Boolean>     enabled;
    private volatile WidgetProperty<Boolean>     extrema_visible;
    private volatile WidgetProperty<Double>      level_high;
    private volatile WidgetProperty<Double>      level_hihi;
    private volatile WidgetProperty<Double>      level_lolo;
    private volatile WidgetProperty<Double>      level_low;
    private volatile WidgetProperty<Boolean>     limits_from_pv;
    private volatile WidgetProperty<Double>      maximum;
    private volatile WidgetProperty<Double>      minimum;
    private volatile WidgetProperty<Integer>     precision;
    private volatile WidgetProperty<Boolean>     show_high;
    private volatile WidgetProperty<Boolean>     show_hihi;
    private volatile WidgetProperty<Boolean>     show_lolo;
    private volatile WidgetProperty<Boolean>     show_low;
    private volatile WidgetProperty<Boolean>     synced_knob;
    private volatile WidgetProperty<WidgetColor> tag_color;
    private volatile WidgetProperty<Boolean>     tag_visible;
    private volatile WidgetProperty<WidgetColor> text_color;
    private volatile WidgetProperty<WidgetColor> thumb_color;
    private volatile WidgetProperty<Boolean>     transparent;
    private volatile WidgetProperty<String>      unit;
    private volatile WidgetProperty<Boolean>     unit_from_pv;
    private volatile WidgetProperty<WidgetColor> value_color;
    private volatile WidgetProperty<Boolean>     value_visible;
    private volatile WidgetProperty<Boolean>     write_on_release;

    public KnobWidget ( ) {
        super(WIDGET_DESCRIPTOR.getType(), 220, 220);
    }

    @Override
    public WidgetConfigurator getConfigurator ( final Version persistedVersion ) throws Exception {
        return new KnobConfigurator(persistedVersion);
    }

    public WidgetProperty<WidgetColor> propBackgroundColor ( ) {
        return background_color;
    }

    public WidgetProperty<WidgetColor> propColor ( ) {
        return color;
    }

    public WidgetProperty<WidgetColor> propColorHiHi ( ) {
        return color_hihi;
    }

    public WidgetProperty<WidgetColor> propColorHigh ( ) {
        return color_high;
    }

    public WidgetProperty<WidgetColor> propColorLoLo ( ) {
        return color_lolo;
    }

    public WidgetProperty<WidgetColor> propColorLow ( ) {
        return color_low;
    }

    public WidgetProperty<WidgetColor> propColorOK ( ) {
        return color_ok;
    }

    public WidgetProperty<Boolean> propEnabled ( ) {
        return enabled;
    }

    public WidgetProperty<Boolean> propExtremaVisible ( ) {
        return extrema_visible;
    }

    public WidgetProperty<Double> propLevelHiHi ( ) {
        return level_hihi;
    }

    public WidgetProperty<Double> propLevelHigh ( ) {
        return level_high;
    }

    public WidgetProperty<Double> propLevelLoLo ( ) {
        return level_lolo;
    }

    public WidgetProperty<Double> propLevelLow ( ) {
        return level_low;
    }

    public WidgetProperty<Boolean> propLimitsFromPV ( ) {
        return limits_from_pv;
    }

    public WidgetProperty<Double> propMaximum ( ) {
        return maximum;
    }

    public WidgetProperty<Double> propMinimum ( ) {
        return minimum;
    }

    public WidgetProperty<Integer> propPrecision ( ) {
        return precision;
    }

    public WidgetProperty<Boolean> propShowHiHi ( ) {
        return show_hihi;
    }

    public WidgetProperty<Boolean> propShowHigh ( ) {
        return show_high;
    }

    public WidgetProperty<Boolean> propShowLoLo ( ) {
        return show_lolo;
    }

    public WidgetProperty<Boolean> propShowLow ( ) {
        return show_low;
    }

    public WidgetProperty<Boolean> propSyncedKnob ( ) {
        return synced_knob;
    }

    public WidgetProperty<WidgetColor> propTagColor ( ) {
        return tag_color;
    }

    public WidgetProperty<Boolean> propTagVisible ( ) {
        return tag_visible;
    }

    public WidgetProperty<WidgetColor> propTextColor ( ) {
        return text_color;
    }

    public WidgetProperty<WidgetColor> propThumbColor ( ) {
        return thumb_color;
    }

    public WidgetProperty<Boolean> propTransparent ( ) {
        return transparent;
    }

    public WidgetProperty<String> propUnit ( ) {
        return unit;
    }

    public WidgetProperty<Boolean> propUnitFromPV ( ) {
        return unit_from_pv;
    }

    public WidgetProperty<WidgetColor> propValueColor ( ) {
        return value_color;
    }

    public WidgetProperty<Boolean> propValueVisible ( ) {
        return value_visible;
    }

    public WidgetProperty<Boolean> propWriteOnRelease ( ) {
        return write_on_release;
    }

    @Override
    protected void defineProperties ( final List<WidgetProperty<?>> properties ) {

        super.defineProperties(properties);

        properties.add(background_color = propBackgroundColor.createProperty(this, new WidgetColor(255, 254, 253)));
        properties.add(color            = propColor.createProperty(this, new WidgetColor(66, 71, 79)));
        properties.add(precision        = propPrecision.createProperty(this, -1));
        properties.add(color_hihi       = propColorHiHi.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR)));
        properties.add(color_high       = propColorHigh.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR)));
        properties.add(color_lolo       = propColorLoLo.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR)));
        properties.add(color_low        = propColorLow.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR)));
        properties.add(color_ok         = propColorOK.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.ALARM_OK)));
        properties.add(extrema_visible  = propExtremaVisible.createProperty(this, false));
        properties.add(level_hihi       = propLevelHiHi.createProperty(this, 90.0));
        properties.add(level_high       = propLevelHigh.createProperty(this, 80.0));
        properties.add(level_lolo       = propLevelLoLo.createProperty(this, 10.0));
        properties.add(level_low        = propLevelLow.createProperty(this, 20.0));
        properties.add(show_hihi        = propShowHiHi.createProperty(this, true));
        properties.add(show_high        = propShowHigh.createProperty(this, true));
        properties.add(show_low         = propShowLow.createProperty(this, true));
        properties.add(show_lolo        = propShowLoLo.createProperty(this, true));
        properties.add(tag_color        = propTagColor.createProperty(this, new WidgetColor(204, 102, 80)));
        properties.add(tag_visible      = propTagVisible.createProperty(this, false));
        properties.add(text_color       = propTextColor.createProperty(this, new WidgetColor(255, 255, 255)));
        properties.add(thumb_color      = propThumbColor.createProperty(this, new WidgetColor(46, 50, 55)));
        properties.add(transparent      = propTransparent.createProperty(this, true));
        properties.add(unit             = propUnit.createProperty(this, ""));
        properties.add(value_color      = propValueColor.createProperty(this, new WidgetColor(0, 22, 0, 153)));
        properties.add(value_visible    = propValueVisible.createProperty(this, true));

        properties.add(enabled          = propEnabled.createProperty(this, true));
        properties.add(limits_from_pv   = propLimitsFromPV.createProperty(this, true));
        properties.add(minimum          = propMinimum.createProperty(this, 0.0));
        properties.add(maximum          = propMaximum.createProperty(this, 100.0));
        properties.add(synced_knob      = propSyncedKnob.createProperty(this, false));
        properties.add(unit_from_pv     = propUnitFromPV.createProperty(this, true));
        properties.add(write_on_release = propWriteOnRelease.createProperty(this, true));

    }

    /**
     * Custom configurator to read legacy *.opi files.
     */
    protected static class KnobConfigurator extends WidgetConfigurator {

        public KnobConfigurator ( Version xmlVersion ) {
            super(xmlVersion);
        }

        @Override
        public boolean configureFromXML ( final ModelReader reader, final Widget widget, final Element xml ) throws Exception {

            if ( !super.configureFromXML(reader, widget, xml) ) {
                return false;
            }

            if ( xml_version.getMajor() < 2 ) {

                KnobWidget knob = (KnobWidget) widget;

                XMLUtil.getChildColor(xml, "color_hi").ifPresent(value -> knob.propColorHigh().setValue(value));
                XMLUtil.getChildColor(xml, "color_lo").ifPresent(value -> knob.propColorLow().setValue(value));
                XMLUtil.getChildColor(xml, "knob_color").ifPresent(value -> knob.propColor().setValue(value));
                XMLUtil.getChildDouble(xml, "level_hi").ifPresent(value -> knob.propLevelHigh().setValue(value));
                XMLUtil.getChildDouble(xml, "level_lo").ifPresent(value -> knob.propLevelLow().setValue(value));
                XMLUtil.getChildBoolean(xml, "show_hi").ifPresent(show -> knob.propShowHigh().setValue(show));
                XMLUtil.getChildBoolean(xml, "show_lo").ifPresent(show -> knob.propShowLow().setValue(show));
                XMLUtil.getChildBoolean(xml, "transparent_background").ifPresent(trans -> knob.propTransparent().setValue(trans));

            }

            return true;

        }

    }

}
