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
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import org.junit.Test;

/**
 * Tests for {@link LenientOnlineNodeCommand}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
public class LenientOnlineNodeCommandTest extends BaseCliTest {

    /**
     * Tests the command to bring it online from being lenient offline.
     */
    @Test
    public void testRunFromLenient() throws Exception {
        DumbSlave slave = jenkins.createOnlineSlave();
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        assert PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()) : "Should be offline";
        assert new ProcessBuilder(cmd("lenient-online-node", slave.getNodeName())).start().waitFor() == 0 : "Cmd error";
        assert !PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()) : "Should be online";
    }

    /**
     * Tests the command to bring it online from being temporary offline.
     */
    @Test
    public void testRunFromTemporary() throws Exception {
        DumbSlave slave = jenkins.createOnlineSlave();
        slave.toComputer().setTemporarilyOffline(true, new OfflineCause.ByCLI("Bomb"));
        assert new ProcessBuilder(cmd("lenient-online-node", slave.getNodeName())).start().waitFor() == 0 : "Cmd error";
        assert !PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()) : "Should be online";
        assert slave.toComputer().isOnline() : "Should be online";
    }
}
