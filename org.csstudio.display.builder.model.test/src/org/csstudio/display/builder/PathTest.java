/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.junit.Test;

/** JUnit test of path handling
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PathTest
{
    @Test
    public void testNormalize() throws Exception
    {
        String path;

        path = ModelResourceUtil.normalize("C:\\some path\\subdir\\file.opi");
        assertThat(path, equalTo("C:/some path/subdir/file.opi"));

        path = ModelResourceUtil.normalize("/old/../new/path/../dir/file.opi");
        assertThat(path, equalTo("/new/dir/file.opi"));
    }

    @Test
    public void testDirectory() throws Exception
    {
        String loc;

        loc = ModelResourceUtil.getDirectory("https://webopi.sns.gov/webopi/opi/Instruments.opi");
        assertThat(loc, equalTo("https://webopi.sns.gov/webopi/opi"));

        loc = ModelResourceUtil.getDirectory("/usr/local/opi/Instruments.opi");
        assertThat(loc, equalTo("/usr/local/opi"));

        loc = ModelResourceUtil.getDirectory("Instruments.opi");
        assertThat(loc, equalTo("."));
    }

    @Test
    public void testCombine() throws Exception
    {
        String path;

        path = ModelResourceUtil.combineDisplayPaths(null, "example.opi");
        assertThat(path, equalTo("example.opi"));

        path = ModelResourceUtil.combineDisplayPaths("examples/dummy.opi", "example.opi");
        assertThat(path, equalTo("examples/example.opi"));

        path = ModelResourceUtil.combineDisplayPaths("examples/dummy.opi", "scripts/test.py");
        assertThat(path, equalTo("examples/scripts/test.py"));

        path = ModelResourceUtil.combineDisplayPaths("https://webopi.sns.gov/webopi/opi/Instruments.bob", "../../share/opi/Motors/motor.opi");
        assertThat(path, equalTo("https://webopi.sns.gov/share/opi/Motors/motor.opi"));

        path = ModelResourceUtil.combineDisplayPaths("https://webopi.sns.gov/webopi/opi/Instruments.opi", "/home/beamline/main.bob");
        assertThat(path, equalTo("/home/beamline/main.bob"));
    }
}
