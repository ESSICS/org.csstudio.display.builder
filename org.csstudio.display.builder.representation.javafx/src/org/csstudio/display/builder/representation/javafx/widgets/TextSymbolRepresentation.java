/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx.widgets;


import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.TextSymbolWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.Styles;
import org.diirt.util.array.ListInt;
import org.diirt.util.array.ListNumber;
import org.diirt.vtype.VBoolean;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VType;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;


/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 3 Aug 2017
 */
public class TextSymbolRepresentation extends RegionBaseRepresentation<Label, TextSymbolWidget> {

    private int                                  arrayIndex             = 0;
    private final DirtyFlag                      dirtyContent           = new DirtyFlag();
    private final DirtyFlag                      dirtyGeometry          = new DirtyFlag();
    private final DirtyFlag                      dirtyStyle             = new DirtyFlag();
    private final DirtyFlag                      dirtyValue             = new DirtyFlag();
    private volatile boolean                     enabled                = true;
    private int                                  symbolIndex            = -1;
    private final WidgetPropertyListener<String> symbolPropertyListener = this::symbolChanged;
    private final AtomicBoolean                  updatingValue          = new AtomicBoolean(false);

    @Override
    public void updateChanges ( ) {

        super.updateChanges();

        Object value;

        if ( dirtyGeometry.checkAndClear() ) {

            value = model_widget.propVisible().getValue();

            if ( !Objects.equals(value, jfx_node.isVisible()) ) {
                jfx_node.setVisible((boolean) value);
            }

            jfx_node.setLayoutX(model_widget.propX().getValue());
            jfx_node.setLayoutY(model_widget.propY().getValue());
            jfx_node.setPrefWidth(model_widget.propWidth().getValue());
            jfx_node.setPrefHeight(model_widget.propHeight().getValue());

        }

        if ( dirtyContent.checkAndClear() ) {

            value = model_widget.propArrayIndex().getValue();

            if ( !Objects.equals(value, arrayIndex) ) {
                arrayIndex = Math.max(0, (int) value);
            }

            symbolIndex = Math.min(Math.max(symbolIndex, 0), model_widget.propSymbols().size() - 1);

            jfx_node.setText(( symbolIndex >= 0 ) ? model_widget.propSymbols().getElement(symbolIndex).getValue() : "\u263A");

        }

        if ( dirtyStyle.checkAndClear() ) {

            value = model_widget.propEnabled().getValue();

            if ( !Objects.equals(value, enabled) ) {

                enabled = (boolean) value;

                Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);

            }

            jfx_node.setAlignment(JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(), model_widget.propVerticalAlignment().getValue()));
            jfx_node.setBackground(model_widget.propTransparent().getValue()
                ? null
                : new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY))
            );
            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            jfx_node.setTextFill(JFXUtil.convert(model_widget.propForegroundColor().getValue()));

        }

        if ( dirtyValue.checkAndClear() && updatingValue.compareAndSet(false, true) ) {

            try {

                value = model_widget.runtimePropValue().getValue();

                if ( value != null ) {
                    if ( value instanceof VBoolean ) {
                        symbolIndex = ((VBoolean) value).getValue() ? 1 : 0;
                    } else if ( value instanceof VString ) {
                        try {
                            symbolIndex = Integer.parseInt(((VString) value).getValue());
                        } catch ( NumberFormatException nfex ) {
                            logger.log(Level.FINE, "Failure parsing the string value: {0} [{1}].", new Object[] { ((VString) value).getValue(), nfex.getMessage() });
                        }
                    } else if ( value instanceof VNumber ) {
                        symbolIndex = ((VNumber) value).getValue().intValue();
                    } else if ( value instanceof VEnum ) {
                        symbolIndex = ((VEnum) value).getIndex();
                    } else if ( value instanceof VNumberArray ) {

                        ListNumber array = ((VNumberArray) value).getData();

                        if ( array.size() > 0 ) {
                            symbolIndex = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    } else if ( value instanceof VEnumArray ) {

                        ListInt array = ((VEnumArray) value).getIndexes();

                        if ( array.size() > 0 ) {
                            symbolIndex = array.getInt(Math.min(arrayIndex, array.size() - 1));
                        }

                    }
                }

            } finally {
                updatingValue.set(false);
            }

            symbolIndex = Math.min(Math.max(symbolIndex, 0), model_widget.propSymbols().size() - 1);

            jfx_node.setText(( symbolIndex >= 0 ) ? model_widget.propSymbols().getElement(symbolIndex).getValue() : "\u263A");

        }

    }

    @Override
    protected Label createJFXNode ( ) throws Exception {

        Label symbol = new Label();

        symbol.setAlignment(JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(), model_widget.propVerticalAlignment().getValue()));
        symbol.setBackground(model_widget.propTransparent().getValue()
            ? null
            : new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY))
        );
        symbol.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
        symbol.setTextFill(JFXUtil.convert(model_widget.propForegroundColor().getValue()));
        symbol.setText("\u263A");

        enabled = model_widget.propEnabled().getValue();

        Styles.update(symbol, Styles.NOT_ENABLED, !enabled);

        return symbol;

    }

    @Override
    protected void registerListeners ( ) {

        super.registerListeners();

        model_widget.propPVName().addPropertyListener(this::contentChanged);
        model_widget.propArrayIndex().addUntypedPropertyListener(this::contentChanged);

        model_widget.propSymbols().addPropertyListener(this::symbolsChanged);

        model_widget.propVisible().addUntypedPropertyListener(this::geometryChanged);
        model_widget.propX().addUntypedPropertyListener(this::geometryChanged);
        model_widget.propY().addUntypedPropertyListener(this::geometryChanged);
        model_widget.propWidth().addUntypedPropertyListener(this::geometryChanged);
        model_widget.propHeight().addUntypedPropertyListener(this::geometryChanged);

        model_widget.propEnabled().addUntypedPropertyListener(this::styleChanged);
        model_widget.propForegroundColor().addUntypedPropertyListener(this::styleChanged);
        model_widget.propBackgroundColor().addUntypedPropertyListener(this::styleChanged);
        model_widget.propTransparent().addUntypedPropertyListener(this::styleChanged);
        model_widget.propFont().addUntypedPropertyListener(this::styleChanged);
        model_widget.propHorizontalAlignment().addUntypedPropertyListener(this::styleChanged);
        model_widget.propVerticalAlignment().addUntypedPropertyListener(this::styleChanged);

        if ( toolkit.isEditMode() ) {
            dirtyValue.checkAndClear();
        } else {
            model_widget.runtimePropValue().addPropertyListener(this::valueChanged);
        }

    }

    private void contentChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyContent.mark();
        toolkit.scheduleUpdate(this);
    }

    private void geometryChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyGeometry.mark();
        toolkit.scheduleUpdate(this);
    }

    private void styleChanged ( final WidgetProperty<?> property, final Object oldValue, final Object newValue ) {
        dirtyStyle.mark();
        toolkit.scheduleUpdate(this);
    }

    private void symbolChanged ( final WidgetProperty<String> property, final String oldValue, final String newValue ) {
        dirtyContent.mark();
        toolkit.scheduleUpdate(this);
    }

    private void symbolsChanged ( final WidgetProperty<List<WidgetProperty<String>>> property, final List<WidgetProperty<String>> oldValue, final List<WidgetProperty<String>> newValue ) {

        if ( oldValue != null ) {
            oldValue.stream().forEach(p -> p.removePropertyListener(symbolPropertyListener));
        }

        if ( newValue != null ) {
            newValue.stream().forEach(p -> p.addPropertyListener(symbolPropertyListener));
        }

        dirtyContent.mark();
        toolkit.scheduleUpdate(this);

    }

    private void valueChanged ( final WidgetProperty<? extends VType> property, final VType oldValue, final VType newValue ) {
        dirtyValue.mark();
        toolkit.scheduleUpdate(this);
    }

}
