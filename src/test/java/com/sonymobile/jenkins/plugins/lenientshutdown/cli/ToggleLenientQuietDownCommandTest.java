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

import com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownDecorator;
import com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownManageLink;

import com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ToggleLenientQuietDownCommand}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
class ToggleLenientQuietDownCommandTest extends BaseCliTest {

    /**
     * Tests the command with the --message option
     * @throws Exception if something goes wrong
     */
    @Test
    void testWithMessage() throws Exception {
        assertEquals(0,
                new ProcessBuilder(cmd("toggle-lenient-quiet-down", "--message", "Bobby is Groovy")).start().waitFor(),
                "Command did not exit correctly");
        assertTrue(ShutdownManageLink.getInstance().isGoingToShutdown(), "Should be shutting down now");
        assertEquals("Bobby is Groovy", ShutdownDecorator.getInstance().getShutdownMessage(), "Not a Groovy message");

        assertEquals(0, new ProcessBuilder(cmd("toggle-lenient-quiet-down")).start().waitFor(),
                "Command did not exit correctly");
        assertFalse(ShutdownManageLink.getInstance().isGoingToShutdown(), "Should be back online");
    }

    /**
     * Tests the command without the --message option
     * @throws Exception if something goes wrong
     */
    @Test
    void testWithoutMessage() throws Exception {
        assertEquals(0, new ProcessBuilder(cmd("toggle-lenient-quiet-down")).start().waitFor(),
                "Command did not exit correctly");
        assertTrue(ShutdownManageLink.getInstance().isGoingToShutdown(), "Should be shutting down now");

        assertEquals(0, new ProcessBuilder(cmd("toggle-lenient-quiet-down")).start().waitFor(),
                "Command did not exit correctly");
        assertFalse(ShutdownManageLink.getInstance().isGoingToShutdown(), "Should be back online");
    }

    /**
     * Tests the command with the --allow-queued option set
     * @throws Exception if something goes wrong
     */
    @Test
    void testAllowAllQueuedItemsEnabled() throws Exception {
        assertEquals(0, new ProcessBuilder(cmd("toggle-lenient-quiet-down", "--allow-queued")).start().waitFor(),
                "Command did not exit correctly");
        assertTrue(ShutdownManageLink.getInstance().isGoingToShutdown(), "Should be shutting down now");
        assertTrue(ShutdownConfiguration.getInstance().isAllowAllQueuedItems(), "All queued items should be allowed");
    }

    /**
     * Tests the command with the --allow-whitelisted option
     * @throws Exception if something goes wrong
     */
    @Test
    void testAllowWhiteListedEnabled() throws Exception {
        assertEquals(0, new ProcessBuilder(cmd("toggle-lenient-quiet-down", "--allow-whitelisted")).start().waitFor(),
                "Command did not exit correctly");
        assertTrue(ShutdownManageLink.getInstance().isGoingToShutdown(), "Should be shutting down now");
        assertTrue(ShutdownConfiguration.getInstance().isAllowWhiteListedProjects(),
                "White listed projects should be allowed");
    }

    /**
     * Tests the command with the -p option
     * @throws Exception if something goes wrong
     */
    @Test
    void testWhiteListed() throws Exception {
        assertEquals(0,
                new ProcessBuilder(cmd("toggle-lenient-quiet-down", "-p", "whitelisted;somename;anotherone"))
                        .start().waitFor(),
                "Command did not exit correctly");
        assertTrue(ShutdownManageLink.getInstance().isGoingToShutdown(), "Should be shutting down now");
        // CS IGNORE MagicNumber FOR NEXT 1 LINES. REASON: No magic.
        assertEquals(3, ShutdownConfiguration.getInstance().getWhiteListedProjects().size(),
                "White listed projects size should be 3");
    }
}
