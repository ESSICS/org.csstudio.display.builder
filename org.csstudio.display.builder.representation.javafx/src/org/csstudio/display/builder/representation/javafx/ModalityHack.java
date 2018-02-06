/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;


import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;


/**
 * Less awful terrible no good hack than before for Modality
 * <p>
 * When hosted inside SWT, the modality of JavaFX Dialogs,
 * {@link Dialog#initModality()},
 * is ignored with respect to SWT windows.
 * The dialog can end up below the SWT window,
 * and the application then appears stuck because it is awaiting
 * a result from closing the dialog.
 * <p>
 * This hack move the dialog always in front.
 *
 * @author Kay Kasemir
 * @author Claudio Rosati
 */
public class ModalityHack {

    public static void forDialog ( final Dialog<?> dialog ) {

        final Window window = dialog.getDialogPane().getContent().getScene().getWindow();

        if ( window instanceof Stage ) {
            ( (Stage) window ).setAlwaysOnTop(true);
        }

    }

}
