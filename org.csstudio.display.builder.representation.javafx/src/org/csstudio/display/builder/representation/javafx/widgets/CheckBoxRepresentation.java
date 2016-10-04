/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.CheckBoxWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.diirt.vtype.VType;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class CheckBoxRepresentation extends JFXBaseRepresentation<CheckBox, CheckBoxWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final DirtyFlag dirty_label = new DirtyFlag();

    protected volatile int bit = 0;
    protected volatile int value = 0;
    protected volatile boolean state = false;
    protected volatile String label = "";

    @Override
    protected final CheckBox createJFXNode() throws Exception
    {
        final CheckBox checkbox = new CheckBox(label);
        checkbox.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);

        if (! toolkit.isEditMode())
            checkbox.setOnAction(event -> handlePress());
        return checkbox;
    }

    /** @param respond to button press */
    private void handlePress()
    {
        logger.log(Level.FINE, "{0} pressed", model_widget);
        int new_val = (bit < 0) ? (value == 0 ? 1 : 0) : (value ^ (1 << bit));
        toolkit.fireWrite(model_widget, new_val);
        // Ideally, PV will soon report the written value.
        // But for now restore the 'current' value of the PV
        // because PV may not change as desired,
        // so assert that widget always reflects the correct value.
        valueChanged(null, null, model_widget.runtimePropValue().getValue());
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(this::sizeChanged);
        model_widget.propHeight().addUntypedPropertyListener(this::sizeChanged);
        model_widget.propAutoSize().addUntypedPropertyListener(this::sizeChanged);

        labelChanged(model_widget.propLabel(), null, model_widget.propLabel().getValue());
        model_widget.propLabel().addPropertyListener(this::labelChanged);
        model_widget.propFont().addUntypedPropertyListener(this::fontChanged);

        bitChanged(model_widget.propBit(), null, model_widget.propBit().getValue());
        model_widget.propBit().addPropertyListener(this::bitChanged);
        model_widget.runtimePropValue().addPropertyListener(this::valueChanged);
   }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void labelChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {
        label = new_value != null ? new_value : model_widget.propLabel().getValue();
        dirty_label.mark();
        toolkit.scheduleUpdate(this);
    }

    private void fontChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_label.mark();
        toolkit.scheduleUpdate(this);
    }

    private void bitChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        bit = (new_value != null ? new_value : model_widget.propBit().getValue());
        stateChanged(bit, value);
    }

    private void valueChanged(final WidgetProperty<VType> property, final VType old_value, final VType new_value)
    {
        value = VTypeUtil.getValueNumber(new_value).intValue();
        stateChanged(bit, value);
    }

    private void stateChanged(final int new_bit, final int new_value)
    {
        state  = (new_bit < 0) ? new_value != 0 : ((new_value >> new_bit) & 1) == 1;
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
        {
            jfx_node.setPrefWidth(model_widget.propWidth().getValue());
            jfx_node.setPrefHeight(model_widget.propHeight().getValue());
            if (model_widget.propAutoSize().getValue())
                jfx_node.autosize();
        }
        if (dirty_content.checkAndClear())
            jfx_node.setSelected(state);
        if (dirty_label.checkAndClear())
        {
            jfx_node.setText(label);
            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
        }
    }
}
