/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.eclipse.osgi.util.NLS;

/** Externalized Strings
 *  @author Kay Kasemir
 */
public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "org.csstudio.display.builder.representation.javafx.messages"; //$NON-NLS-1$

    // Keep in alphabetical order, synchronized with messages.properties
    public static String ActionsDialog_Actions;
    public static String ActionsDialog_Description;
    public static String ActionsDialog_Detail;
    public static String ActionsDialog_DisplayPath;
    public static String ActionsDialog_Info;
    public static String ActionsDialog_PVName;
    public static String ActionsDialog_ScriptPath;
    public static String ActionsDialog_ScriptText;
    public static String ActionsDialog_Title;
    public static String ActionsDialog_Value;
    public static String Add;
    public static String Blue;
    public static String ColorDialog_Custom;
    public static String ColorDialog_Info;
    public static String ColorDialog_Predefined;
    public static String ColorDialog_Title;
    public static String FontDialog_ExampleText;
    public static String FontDialog_Family;
    public static String FontDialog_Info;
    public static String FontDialog_Predefined;
    public static String FontDialog_Preview;
    public static String FontDialog_Size;
    public static String FontDialog_Style;
    public static String FontDialog_Title;
    public static String Green;
    public static String MacrosDialog_Info;
    public static String MacrosDialog_NameCol;
    public static String MacrosDialog_Title;
    public static String MacrosDialog_ValueCol;
    public static String MacrosTable_NameHint;
    public static String MacrosTable_ToolTip;
    public static String MacrosTable_ValueHint;
    public static String MoveDown;
    public static String MoveUp;
    public static String PointsDialog_Info;
    public static String PointsDialog_Title;
    public static String PointsTable_Empty;
    public static String PointsTable_X;
    public static String PointsTable_Y;
    public static String Red;
    public static String Remove;
    public static String RulesDialog_ColName;
    public static String RulesDialog_DefaultRuleName;
    public static String RulesDialog_Info;
    public static String RulesDialog_PVsTT;
    public static String RulesDialog_RulesTT;
    public static String RulesDialog_SelectRule;
    public static String RulesDialog_Title;
    public static String ScriptsDialog_BtnEmbedJS;
    public static String ScriptsDialog_BtnEmbedPy;
    public static String ScriptsDialog_BtnFile;
    public static String ScriptsDialog_ColPV;
    public static String ScriptsDialog_ColScript;
    public static String ScriptsDialog_ColTrigger;
    public static String ScriptsDialog_DefaultEmbeddedJavaScript;
    public static String ScriptsDialog_DefaultEmbeddedPython;
    public static String ScriptsDialog_DefaultScriptFile;
    public static String ScriptsDialog_FileBrowser_Title;
    public static String ScriptsDialog_FileType_All;
    public static String ScriptsDialog_FileType_JS;
    public static String ScriptsDialog_FileType_Py;
    public static String ScriptsDialog_Info;
    public static String ScriptsDialog_PVsTT;
    public static String ScriptsDialog_ScriptsTT;
    public static String ScriptsDialog_SelectScript;
    public static String ScriptsDialog_Title;
    public static String WidgetInfoDialog_Category;
    public static String WidgetInfoDialog_Disconnected;
    public static String WidgetInfoDialog_Info_Fmt;
    public static String WidgetInfoDialog_Name;
    public static String WidgetInfoDialog_Property;
    public static String WidgetInfoDialog_TabProperties;
    public static String WidgetInfoDialog_TabPVs;
    public static String WidgetInfoDialog_Title;
    public static String WidgetInfoDialog_Value;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
