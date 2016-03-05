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

import com.sonymobile.jenkins.plugins.lenientshutdown.Messages;
import com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownManageLink;

import hudson.Extension;
import jenkins.model.Jenkins;

/**
 * Cli command <code>lenient-quiet-down</code>.
 * In contrast to the core version <code>quiet-down</code>.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
@Extension
public class LenientQuietDownCommand extends LenientQuietDownCommandBase {

    @Override
    public String getShortDescription() {
        return Messages.ActivateShutdownTitle();
    }

    @Override
    protected int run() throws Exception {
        ShutdownManageLink management = ShutdownManageLink.getInstance();
        Jenkins.getInstance().checkPermission(management.getRequiredPermission());

        if (!management.isGoingToShutdown()) {
            management.performToggleGoingToShutdown();
            configure();
            printStatus();
            return 0;
        } else {
            stderr.println(Messages.Err_AlreadyShuttingDown());
            return 1;
        }
    }
}
