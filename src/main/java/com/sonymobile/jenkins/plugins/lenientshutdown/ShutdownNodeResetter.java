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

package com.sonymobile.jenkins.plugins.lenientshutdown;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;

/**
 * Class for resetting lenient offline statuses when nodes come back online.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@Extension
public class ShutdownNodeResetter extends ComputerListener {

    /**
     * Makes sure the lenient offline status is being reset
     * after a node is taken online again.
     * @param computer the computer to reset status for
     * @param listener task listener
     */
    @Override
    public void onOnline(Computer computer, TaskListener listener) {
        onTemporarilyOnline(computer);
    }

    /**
     * Makes sure the lenient offline status is being reset
     * after a node is taken temporarily online again.
     * @param computer the computer to reset status for
     */
    @Override
    public void onTemporarilyOnline(Computer computer) {
        PluginImpl plugin = PluginImpl.getInstance();
        if (plugin != null && plugin.isNodeShuttingDown(computer.getName())) {
            plugin.toggleNodeShuttingDown(computer.getName());
        }
    }

}
