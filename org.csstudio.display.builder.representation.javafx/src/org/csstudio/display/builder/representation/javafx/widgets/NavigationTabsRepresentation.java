/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.checkCompletion;
import static org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.loadDisplayModel;
import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.Direction;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget.TabProperty;
import org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.DisplayAndGroup;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/** Creates JavaFX item for model widget
 *
 *  <p>Different from widget representations in general,
 *  this one implements the loading of the embedded model,
 *  an operation that could be considered a runtime aspect.
 *  This was done to allow viewing the embedded content
 *  in the editor.
 *  The embedded model will be started by the EmbeddedDisplayRuntime.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
// TODO Runtime
public class NavigationTabsRepresentation extends RegionBaseRepresentation<NavigationTabs, NavigationTabsWidget>
{
    private final DirtyFlag dirty_sizes = new DirtyFlag();
    private final DirtyFlag dirty_tabs = new DirtyFlag();
    private final DirtyFlag dirty_tab_look = new DirtyFlag();
    private final DirtyFlag dirty_active_tab = new DirtyFlag();

    /** The display file (and optional group inside that display) to load */
    private final AtomicReference<DisplayAndGroup> pending_display_and_group = new AtomicReference<>();

    /** Inner pane that holds child widgets
     *
     *  <p>Set to null when representation is disposed,
     *  which is used as indicator to pending display updates.
     */
    private volatile Pane inner = new Pane();

    /** Track active model in a thread-safe way
     *  to assert that each one is represented and removed
     */
    private final AtomicReference<DisplayModel> active_content_model = new AtomicReference<>();

    private final WidgetPropertyListener<String> tab_name_listener = (property, old_value, new_value) ->
    {
        dirty_tabs.mark();
        toolkit.scheduleUpdate(this);
    };

    /** Handle changed display.
     *
     *  <p>Either the settings for a tab changed,
     *  or the active tab changed.
     *
     *  For details see {@link EmbeddedDisplayRepresentation#fileChanged}
     */
    private final UntypedWidgetPropertyListener tab_display_listener = (property, old_value, new_value) ->
    {
        final List<TabProperty> tabs = model_widget.propTabs().getValue();
        final int active = Math.min(tabs.size()-1, model_widget.propActiveTab().getValue());
        if (active < 0)
            return;

        final TabProperty active_tab = tabs.get(active);
        final DisplayAndGroup file_and_group =
            new DisplayAndGroup(active_tab.file().getValue(), active_tab.group().getValue());

        final DisplayAndGroup skipped = pending_display_and_group.getAndSet(file_and_group);
        if (skipped != null)
            logger.log(Level.FINE, "Skipped: {0}", skipped);

        // Load embedded display in background thread
        ModelThreadPool.getExecutor().execute(this::updatePendingDisplay);
    };

    @Override
    public NavigationTabs createJFXNode() throws Exception
    {
        final NavigationTabs tabs = new NavigationTabs();

        tabs.setContent(inner);

        tabs.addListener(index -> model_widget.propActiveTab().setValue(index));

        return tabs;
    }

    @Override
    protected Parent getChildParent(final Parent parent)
    {
        return inner;
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(this::sizesChanged);
        model_widget.propHeight().addUntypedPropertyListener(this::sizesChanged);

        model_widget.propTabWidth().addPropertyListener(this::tabLookChanged);
        model_widget.propTabHeight().addPropertyListener(this::tabLookChanged);
        model_widget.propTabSpacing().addPropertyListener(this::tabLookChanged);
        model_widget.propSelectedColor().addPropertyListener(this::tabLookChanged);
        model_widget.propDeselectedColor().addPropertyListener(this::tabLookChanged);

        model_widget.propActiveTab().addPropertyListener(this::activeTabChanged);

        model_widget.propTabs().addPropertyListener(this::tabsChanged);

        // Initial update
        tabsChanged(model_widget.propTabs(), null, model_widget.propTabs().getValue());
        activeTabChanged(null, null, model_widget.propActiveTab().getValue());
    }

    private void activeTabChanged(final WidgetProperty<Integer> property, final Integer old_index, final Integer tab_index)
    {
        dirty_active_tab.mark();
        toolkit.scheduleUpdate(this);
        tab_display_listener.propertyChanged(null, null, null);
    }

    /** Update to the next pending display
     *
     *  <p>Synchronized to serialize the background threads.
     *
     *  <p>Example: Displays A, B, C are requested in quick succession.
     *
     *  <p>pending_display_and_group=A is submitted to executor thread A.
     *
     *  <p>While handling A, pending_display_and_group=B is submitted to executor thread B.
     *  Thread B will be blocked in synchronized method.
     *
     *  <p>Then pending_display_and_group=C is submitted to executor thread C.
     *  As thread A finishes, thread B finds pending_display_and_group==C.
     *  As thread C finally continues, it finds pending_display_and_group empty.
     *  --> Showing A, then C, skipping B.
     */
    private synchronized void updatePendingDisplay()
    {
        final DisplayAndGroup handle = pending_display_and_group.getAndSet(null);
        if (handle == null)
            return;
        if (inner == null)
        {
            // System.out.println("Aborted: " + handle);
            return;
        }

        try
        {   // Load new model (potentially slow)
            final DisplayModel new_model = loadDisplayModel(model_widget, handle);

            // Atomically update the 'active' model
            final DisplayModel old_model = active_content_model.getAndSet(new_model);
            if (old_model != null)
            {   // Dispose old model
                final Future<Object> completion = toolkit.submit(() ->
                {
                    toolkit.disposeRepresentation(old_model);
                    return null;
                });
                checkCompletion(model_widget, completion, "timeout disposing old representation");
            }
            // Represent new model on UI thread
            final Future<Object> completion = toolkit.submit(() ->
            {
                representContent(new_model);
                return null;
            });
            checkCompletion(model_widget, completion, "timeout representing new content");
            model_widget.runtimePropEmbeddedModel().setValue(new_model);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to handle embedded display " + handle, ex);
        }
    }

    /** @param content_model Model to represent */
    private void representContent(final DisplayModel content_model)
    {
        try
        {
            toolkit.representModel(inner, content_model);

            // TODO Color. Remove 'inner', use the pane inside the NavigationTabs?
            inner.setBackground(new Background(new BackgroundFill(JFXUtil.convert(content_model.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
            inner.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Failed to represent embedded display", ex);
        }
    }

    private void sizesChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_sizes.mark();
        toolkit.scheduleUpdate(this);
    }

    private void tabLookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_tab_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private void tabsChanged(final WidgetProperty<List<TabProperty>> property, final List<TabProperty> removed, final List<TabProperty> added)
    {
        if (removed != null)
            removeTabs(removed);
        if (added != null)
            addTabs(added);

        dirty_tabs.mark();
        toolkit.scheduleUpdate(this);
    }

    private void removeTabs(final List<TabProperty> removed)
    {
        for (TabProperty tab : removed)
        {
            tab.name().removePropertyListener(tab_name_listener);
            tab.file().removePropertyListener(tab_display_listener);
            tab.macros().removePropertyListener(tab_display_listener);
            tab.group().removePropertyListener(tab_display_listener);
        }
    }

    private void addTabs(final List<TabProperty> added)
    {
        for (TabProperty tab : added)
        {
            tab.group().addUntypedPropertyListener(tab_display_listener);
            tab.macros().addUntypedPropertyListener(tab_display_listener);
            tab.file().addUntypedPropertyListener(tab_display_listener);
            tab.name().addPropertyListener(tab_name_listener);
        }
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_sizes.checkAndClear())
        {
            final Integer width = model_widget.propWidth().getValue();
            final Integer height = model_widget.propHeight().getValue();
            jfx_node.setPrefSize(width, height);
        }
        if (dirty_tab_look.checkAndClear())
        {
            jfx_node.setTabSize(model_widget.propTabWidth().getValue(),
                                model_widget.propTabHeight().getValue());
            jfx_node.setTabSpacing(model_widget.propTabSpacing().getValue());
            jfx_node.setSelectedColor(JFXUtil.convert(model_widget.propSelectedColor().getValue()));
            jfx_node.setDeselectedColor(JFXUtil.convert(model_widget.propDeselectedColor().getValue()));
            // TODO Direction property
            jfx_node.setDirection(Direction.VERTICAL);
        }
        if (dirty_tabs.checkAndClear())
        {
            final List<String> tabs = new ArrayList<>();
            model_widget.propTabs().getValue().forEach(tab -> tabs.add(tab.name().getValue()));
            jfx_node.setTabs(tabs);
        }
        if (dirty_active_tab.checkAndClear())
            jfx_node.selectTab(model_widget.propActiveTab().getValue());
    }

    @Override
    public void dispose()
    {
        inner = null;
        super.dispose();
    }
}
