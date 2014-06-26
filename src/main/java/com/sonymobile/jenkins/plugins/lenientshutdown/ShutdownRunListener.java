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
import hudson.model.Executor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.model.listeners.RunListener;
import jenkins.util.Timer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for completed builds and sets nodes as offline when they are
 * finished if they have "set as temp. offline leniently" activated.
 * @param <R> run type
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@Extension(ordinal = Double.MAX_VALUE)
public class ShutdownRunListener<R extends Run> extends RunListener<R> {

    private static final int TASK_DELAY_SECONDS = 10;

    private static final Logger logger = Logger.getLogger(ShutdownRunListener.class.getName());

    @Override
    public void onCompleted(final R r, TaskListener listener) {
        final PluginImpl plugin = PluginImpl.getInstance();

        Executor executor = r.getExecutor();
        if (executor != null) {
            final Computer computer = executor.getOwner();
            if (computer != null) {
                final String nodeName = computer.getName();

                if (plugin.isNodeShuttingDown(nodeName)) {
                    //Schedule checking if all builds are completed on the build node after a delay:
                    Runnable isNodeIdleTask = new Runnable() {
                        @Override
                        public void run() {
                            if (plugin.isNodeShuttingDown(nodeName) && !computer.isTemporarilyOffline()
                                    && !QueueUtils.isBuilding(computer)
                                    && !QueueUtils.hasNodeExclusiveItemInQueue(computer)) {
                                logger.log(Level.INFO, "Node {0} idle; setting offline since lenient "
                                        + "shutdown was active for this node", nodeName);

                                User user = plugin.getOfflineByUser(nodeName);
                                computer.setTemporarilyOffline(true, new LenientOfflineCause(user));
                            }
                        }
                    };
                    Timer.get().schedule(isNodeIdleTask, TASK_DELAY_SECONDS, TimeUnit.SECONDS);
                }
            }
        }
    }

}
