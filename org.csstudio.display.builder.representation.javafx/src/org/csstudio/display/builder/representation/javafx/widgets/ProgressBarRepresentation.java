/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.beans.PropertyChangeEvent;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.epics.vtype.Display;
import org.epics.vtype.VType;
import org.epics.vtype.ValueUtil;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class ProgressBarRepresentation extends JFXBaseRepresentation<ProgressBar, ProgressBarWidget>
{
    private final DirtyFlag dirty_position = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private volatile double percentage = 0.0;

    public ProgressBarRepresentation(final ToolkitRepresentation<Group, Node> toolkit,
                                     final ProgressBarWidget model_widget)
    {
        super(toolkit, model_widget);
    }

    @Override
    public ProgressBar createJFXNode() throws Exception
    {
        final ProgressBar bar = new ProgressBar();

        // Color is set via default, built-in CSS.
        // To change, could disable and then define background,
        // but result looks pretty plane
        // bar.getStyleClass().clear();
        // bar.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));

        return bar;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.positionWidth().addPropertyListener(this::positionChanged);
        model_widget.positionHeight().addPropertyListener(this::positionChanged);
        model_widget.behaviorLimitsFromPV().addPropertyListener(this::contentChanged);
        model_widget.behaviorMinimum().addPropertyListener(this::contentChanged);
        model_widget.behaviorMaximum().addPropertyListener(this::contentChanged);
        model_widget.runtimeValue().addPropertyListener(this::contentChanged);
    }

    private void positionChanged(final PropertyChangeEvent event)
    {
        dirty_position.mark();
        toolkit.scheduleUpdate(this);
    }

    private void contentChanged(final PropertyChangeEvent event)
    {
        final VType vtype = model_widget.runtimeValue().getValue();

        final boolean limits_from_pv = model_widget.behaviorLimitsFromPV().getValue();
        double min_val = model_widget.behaviorMinimum().getValue();
        double max_val = model_widget.behaviorMaximum().getValue();
        if (limits_from_pv)
        {
            // Try display range from PV
            final Display display_info = ValueUtil.displayOf(vtype);
            if (display_info != null)
            {
                min_val = display_info.getLowerDisplayLimit();
                max_val = display_info.getUpperDisplayLimit();
            }
        }
        // Fall back to 0..100 range
        if (min_val >= max_val)
        {
            min_val = 0.0;
            max_val = 100.0;
        }

        // Determine percentage of value within the min..max range
        final double value = VTypeUtil.getValueNumber(vtype).doubleValue();
        final double percentage = (value - min_val) / (max_val - min_val);
        // Limit to 0.0 .. 1.0
        if (percentage < 0.0)
            this.percentage = 0.0;
        else if (percentage > 1.0)
            this.percentage = 1.0;
        else
            this.percentage = percentage;
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_position.checkAndClear())
            jfx_node.setPrefSize(model_widget.positionWidth().getValue(),
                                 model_widget.positionHeight().getValue());
        if (dirty_content.checkAndClear())
            jfx_node.setProgress(percentage );
    }
}
