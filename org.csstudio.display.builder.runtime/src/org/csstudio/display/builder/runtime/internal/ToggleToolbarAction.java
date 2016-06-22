/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.displayToolbar;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.Messages;
import org.csstudio.display.builder.runtime.RuntimeAction;

/** Action for runtime of Image or XY plot widget
 *
 *  <p>Hides/shows the toolbar
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ToggleToolbarAction extends RuntimeAction
{
    private final Widget widget;

    public ToggleToolbarAction(final Widget widget)
    {
        super(Messages.Toolbar_Hide,
              "platform:/plugin/org.csstudio.javafx.rtplot/icons/toolbar.png");
        this.widget = widget;
        updateDescription();
    }

    private void updateDescription()
    {
        description = widget.getPropertyValue(displayToolbar)
                    ? Messages.Toolbar_Hide
                    : Messages.Toolbar_Show;
    }

    @Override
    public void run()
    {
        widget.setPropertyValue(displayToolbar, ! widget.getPropertyValue(displayToolbar));
        updateDescription();
    }
}
