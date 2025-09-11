/*
 *  The MIT License
 *
 *  Copyright (c) 2014 Sony Mobile Communications Inc. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonymobile.jenkins.plugins.lenientshutdown.cli;

import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WarExploder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for all CLI tests containing convenience methods.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
abstract class BaseCliTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private File jenkinsCliJar;
    private File java;

    @Before
    public void before() throws Exception {
        jenkinsCliJar = new File(WarExploder.getExplodedDir(), "WEB-INF/lib/cli-" + Jenkins.VERSION + ".jar");
        java = new File(System.getProperty("java.home"), "bin/java");
    }

    protected String[] cmd(String... cmd) throws Exception {
        List<String> prefix = new ArrayList<>();
        prefix.add(java.getPath());
        prefix.add("-jar");
        prefix.add(jenkinsCliJar.getPath());
        prefix.add("-s");
        prefix.add(jenkins.getURL().toString());

        prefix.addAll(Arrays.asList(cmd));

        return prefix.toArray(new String[0]);
    }
}
