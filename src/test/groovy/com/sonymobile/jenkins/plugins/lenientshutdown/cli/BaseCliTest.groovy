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
package com.sonymobile.jenkins.plugins.lenientshutdown.cli

import jenkins.model.Jenkins
import org.junit.Before
import org.junit.Rule
import org.jvnet.hudson.test.GroovyJenkinsRule
import org.jvnet.hudson.test.WarExploder

/**
 * Base class for all CLI tests containing convenience methods.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
class BaseCliTest {
    @Rule
    public GroovyJenkinsRule jenkins = new GroovyJenkinsRule()

    File jenkinsCliJar
    File java

    @Before
    void before() {
        final String jenkinsVersion = Jenkins.VERSION
        jenkinsCliJar = new File(WarExploder.explodedDir, "WEB-INF/lib/cli-${jenkinsVersion}.jar")
        java = new File(System.getProperty("java.home"), "bin/java")
    }

    String[] cmd(String... cmd) {
        def prefix = [ java.path, "-jar" , jenkinsCliJar.path, "-s", jenkins.getURL()]
        return (prefix << (cmd as List)).flatten()
    }
}
