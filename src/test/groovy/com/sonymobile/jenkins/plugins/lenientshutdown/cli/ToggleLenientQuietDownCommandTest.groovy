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
 * Tests for {@link ToggleLenientQuietDownCommand}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
class ToggleLenientQuietDownCommandTest extends BaseCliTest {

    /**
     * Tests the command with the --message option
     */
    @Test
    void testWithMessage() {
        assert cmd("toggle-lenient-quiet-down", "--message", "Bobby is Groovy").execute().waitFor() == 0 :
                "Command did not exit correctly"
        assert ShutdownManageLink.instance.isGoingToShutdown() : "Should be shutting down now"
        assert ShutdownDecorator.instance.shutdownMessage == "Bobby is Groovy" : "Not a Groovy message"

        assert cmd("toggle-lenient-quiet-down").execute().waitFor() == 0 :
                "Command did not exit correctly"
        assert !ShutdownManageLink.instance.isGoingToShutdown() : "Should be back online"
    }

    /**
     * Tests the command without the --message option
     */
    @Test
    void testWithoutMessage() {

        assert cmd("toggle-lenient-quiet-down").execute().waitFor() == 0 :
                "Command did not exit correctly"
        assert ShutdownManageLink.instance.isGoingToShutdown() : "Should be shutting down now"

        assert cmd("toggle-lenient-quiet-down").execute().waitFor() == 0 :
                "Command did not exit correctly"
        assert !ShutdownManageLink.instance.isGoingToShutdown() : "Should be back online"

    }
}
