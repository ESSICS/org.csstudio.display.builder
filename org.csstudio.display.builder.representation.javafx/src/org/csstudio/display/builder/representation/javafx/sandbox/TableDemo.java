/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.util.Arrays;
import java.util.List;

import org.csstudio.javafx.StringTable;
import org.csstudio.javafx.StringTableListener;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** Table demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TableDemo extends Application
{
    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage)
    {
        // Example data
        final List<String> headers = Arrays.asList("Left", "Middle", "Right");
        // Data lacks one element to demonstrate log message.
        // Table still "works"
        final List<List<String>> data = Arrays.asList(
                Arrays.asList("One", "Two" /*, "missing" */),
                Arrays.asList("Uno", "Due", "Tres"));

        // Table
        final StringTable table = new StringTable(true);
        table.setHeaders(headers);
        table.setColumnOptions(1, Arrays.asList("Two", "Due", "Zwo", "1+2-1"));
        table.setData(data);
        table.setBackgroundColor(Color.PINK);
        table.setTextColor(Color.GREEN);
        table.setFont(Font.font("Liberation Serif", 12.0));
        table.setCellColor(0, 1, Color.ORANGE);
        table.setCellColor(1, 2, Color.BLUEVIOLET);

        table.setListener(new StringTableListener()
        {
            @Override
            public void tableChanged(final StringTable table)
            {
                System.out.println("Table headers and data changed");
            }

            @Override
            public void dataChanged(final StringTable table)
            {
                System.out.println("Data changed");
            }

            @Override
            public void selectionChanged(final StringTable table, final int[] rows, final int[] cols)
            {
                System.out.println("Selection: rows " + Arrays.toString(rows) + ", cols " + Arrays.toString(cols));
            }
        });

        // Example scene
        final Label label = new Label("Demo:");
        final Button new_data = new Button("New Data");
        new_data.setOnAction(event ->
        {
            table.setHeaders(Arrays.asList("A", "B"));
            table.setData(Arrays.asList(
                    Arrays.asList("A 1", "B 1"),
                    Arrays.asList("A 2", "B 2")));
        });

        final BorderPane layout = new BorderPane();
        layout.setTop(label);
        layout.setCenter(table);
        layout.setRight(new_data);

        final Scene scene = new Scene(layout, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Table Demo");
        stage.setOnCloseRequest(event ->
        {   // Fetch data from table view
            System.out.println(table.getHeaders());
            for (List<String> row : table.getData())
                System.out.println(row);

            System.out.println("Original data:");
            for (List<String> row : data)
                System.out.println(row);
        });
        stage.show();
    }
}
