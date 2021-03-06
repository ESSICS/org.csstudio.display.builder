/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.util.undo;

/** Listener for {@link UndoableActionManager}
 *  @author Xihui Chen original org.csstudio.swt.xygraph.undo.IOperationsManagerListener
 *  @author Kay Kasemir
 */
public interface UndoRedoListener
{
    /** @param to_undo Description of action to undo or <code>null</code>
     *  @param to_redoDescription of action to re-do or <code>null</code>
     */
    public void operationsHistoryChanged(final String to_undo, final String to_redo);
}
