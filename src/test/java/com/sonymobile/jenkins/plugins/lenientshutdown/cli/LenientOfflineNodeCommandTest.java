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

import com.sonymobile.jenkins.plugins.lenientshutdown.PluginImpl;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import org.jvnet.hudson.test.SleepBuilder;
import org.junit.Test;

/**
 * Tests for {@link LenientOfflineNodeCommand}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
public class LenientOfflineNodeCommandTest extends BaseCliTest {

    /**
     * Tests the command to bring it offline temporary i.e. while no build is running.
     */
    @Test
    public void testRunTemporary() throws Exception {
        DumbSlave slave = jenkins.createOnlineSlave();
        assert new ProcessBuilder(cmd("lenient-offline-node", slave.getNodeName())).start().waitFor() == 0 : "Cmd Error";
        assert slave.toComputer().isOffline() : "Should be offline";
    }


    /**
     * Tests the command to bring it offline leniently i.e. while a build is running.
     */
    @Test
    public void testRunLenient() throws Exception {
        DumbSlave slave = jenkins.createOnlineSlave();

        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new SleepBuilder(100000));
        project.setAssignedLabel(slave.getSelfLabel());
        project.scheduleBuild2(0);

        assert new ProcessBuilder(cmd("lenient-offline-node", slave.getNodeName())).start().waitFor() == 0 : "Cmd Error";
        assert PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()) : "Should be lenient offline";
    }

}
