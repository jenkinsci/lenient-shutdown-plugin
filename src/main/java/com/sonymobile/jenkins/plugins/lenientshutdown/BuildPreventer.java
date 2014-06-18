/*
 *  The MIT License
 *
 *  Copyright 2014 Sony Mobile Communications AB. All rights reserved.
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
import hudson.model.AbstractProject;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Prevents builds from running when lenient shutdown mode is active.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@Extension
public class BuildPreventer extends QueueTaskDispatcher {

    private static final Logger logger = Logger.getLogger(BuildPreventer.class.getName());

    @Override
    public CauseOfBlockage canRun(Queue.Item item) {
        CauseOfBlockage blockage = null; //Allow to run by default

        ShutdownManageLink shutdownManageLink = ShutdownManageLink.getInstance();
        boolean isGoingToShutdown = shutdownManageLink.isGoingToShutdown();

        if (isGoingToShutdown
                && item.task instanceof AbstractProject
                && !shutdownManageLink.wasAlreadyQueued(item.id)) {
            Set<String> upstreamProjects = QueueUtils.getUpstreamProjectNames(item);

            if (!shutdownManageLink.isAnyAllowedUpstreamProject(upstreamProjects)) {
                logger.fine(String.format("Preventing project %s from running, since lenient shutdown is active",
                        item.getDisplayName()));
                blockage = new ShutdownBlockage();
            }
        }

        //Set the project as allowed upstream project if it was not blocked and shutdown enabled:
        if (blockage == null && isGoingToShutdown) {
            AbstractProject project = (AbstractProject)item.task;
            shutdownManageLink.addAllowedUpstreamProject(project);
        }

        return blockage;
    }

}
