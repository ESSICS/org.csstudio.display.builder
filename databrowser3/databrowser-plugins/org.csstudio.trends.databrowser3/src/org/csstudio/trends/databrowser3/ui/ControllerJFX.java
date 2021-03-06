/*******************************************************************************
 * Copyright (c) 2010-2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.csdata.ProcessVariable;
import org.csstudio.javafx.Activator;
import org.csstudio.trends.databrowser3.archive.ArchiveFetchJob;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import javafx.application.Platform;

/** Controller that interfaces the {@link Model} with the {@link ModelBasedPlotSWT}:
 *  <ul>
 *  <li>For each item in the Model, create a trace in the plot.
 *  <li>Perform scrolling of the time axis.
 *  <li>When the plot is interactively zoomed, update the Model's time range.
 *  <li>Get archived data whenever the time axis changes.
 *  </ul>
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ControllerJFX extends ControllerBase
{

    class JFXArchiveFetchJobListener extends BaseArchiveFetchJobListener
    {
        @Override
        protected void executeOnUIThread(Consumer<Void> consumer)
        {
            Platform.runLater(() ->
            {
                consumer.accept(null);
            });
        }

        @Override
        protected void displayError(String message, Exception error)
        {
            Activator.logger.log(Level.WARNING, message, error);
        }
    };
    final private JFXArchiveFetchJobListener archive_fetch_listener;

    class JFXPlotListener extends BasePlotListener
    {
        @Override
        protected void executeOnUIThread(Runnable func)
        {
            Platform.runLater(func);
        }

        @Override
        public void timeConfigRequested()
        {
            // There is no JavaFX time config dialog, so call the SWT version
            final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            StartEndTimeAction.run(shell, model, plot.getPlot().getUndoableActionManager());
        }

        @Override
        public void droppedNames(String[] name)
        {
            // Ignored
        }

        @Override
        public void droppedPVNames(ProcessVariable[] name,
                ArchiveDataSource[] archive)
        {
            // Ignored
        }

        @Override
        public void droppedFilename(String file_name)
        {
            // Ignored
        }
    };

    @Override
    protected ArchiveFetchJob makeArchiveFetchJob(PVItem pv_item, Instant start, Instant end)
    {
        return new ArchiveFetchJob(pv_item, start, end, archive_fetch_listener);
    }

    /** Initialize
     *  @param shell Shell
     *  @param model Model that has the data
     *  @param plot Plot for displaying the Model
     *  @throws Error when called from non-UI thread
     */
    public ControllerJFX(final Model model, final ModelBasedPlot plot)
    {
        super (model, plot);
        archive_fetch_listener = new JFXArchiveFetchJobListener();

        // Listen to user input from Plot UI, update model
        plot.addListener(new JFXPlotListener());
    }

}
