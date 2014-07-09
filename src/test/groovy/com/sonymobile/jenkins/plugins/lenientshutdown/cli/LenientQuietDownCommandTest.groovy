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

import com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownDecorator
import com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownManageLink
import org.junit.Test

/**
 * Tests for {@link LenientQuietDownCommand}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
class LenientQuietDownCommandTest extends BaseCliTest {

    /**
     * Runs the command with a message (-m)
     */
    @Test
    void testRunWithMessage() {
        assert cmd("lenient-quiet-down", "-m", "Bobby is cool").execute().waitFor() == 0 :
                "Command did not exit correctly"
        assert ShutdownManageLink.instance.isGoingToShutdown() : "Shutdown flag not set to true"
        assert ShutdownDecorator.instance.shutdownMessage == "Bobby is cool" : "Non cool message"
    }

    /**
     * Runs the command without a message
     */
    @Test
    void testRunWithoutMessage() {
        assert cmd("lenient-quiet-down").execute().waitFor() == 0 : "Command did not exit correctly"
        assert ShutdownManageLink.instance.isGoingToShutdown() : "Shutdown flag not set to true"
    }
}
