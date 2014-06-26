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

import com.sonymobile.jenkins.plugins.lenientshutdown.blockcauses.GlobalShutdownBlockage;
import com.sonymobile.jenkins.plugins.lenientshutdown.blockcauses.NodeShutdownBlockage;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Prevents builds from running when lenient shutdown mode is active.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@Extension
public class BuildPreventer extends QueueTaskDispatcher {

    private static final Logger logger = Logger.getLogger(BuildPreventer.class.getName());

    /**
     * Handles prevention of builds for lenient shutdown on the Jenkins master.
     * @param item QueueItem to build
     * @return CauseOfBlockage if a build is prevented, otherwise null
     */
    @Override
    public CauseOfBlockage canRun(Queue.Item item) {
        CauseOfBlockage blockage = null; //Allow to run by default

        ShutdownManageLink shutdownManageLink = ShutdownManageLink.getInstance();
        boolean isGoingToShutdown = shutdownManageLink.isGoingToShutdown();

        if (isGoingToShutdown
                && item.task instanceof AbstractProject
                && !shutdownManageLink.wasAlreadyQueued(item.id)) {
            Set<String> upstreamProjects = QueueUtils.getUpstreamProjectNames(item);

            if (!shutdownManageLink.isAnyPermittedUpstreamProject(upstreamProjects)) {
                logger.log(Level.FINE, "Preventing project {0} from running, "
                        + "since lenient shutdown is active", item.getDisplayName());
                blockage = new GlobalShutdownBlockage();
            }
        }

        //Set the project as allowed upstream project if it was not blocked and shutdown enabled:
        if (blockage == null && isGoingToShutdown) {
            AbstractProject project = (AbstractProject)item.task;
            shutdownManageLink.addPermittedUpstreamProject(project);
        }

        return blockage;
    }

    /**
     * Handles prevention of builds specific for a node when taking specific nodes offline leniently.
     * @param node the node to check prevention for
     * @param item the buildable item to check prevention for
     * @return CauseOfBlockage if a build is prevented, otherwise null
     */
    @Override
    public CauseOfBlockage canTake(Node node, Queue.BuildableItem item) {
        CauseOfBlockage blockage = null; //Allow to run by default

        PluginImpl plugin = PluginImpl.getInstance();
        String nodeName = node.getNodeName();
        boolean nodeIsGoingToShutdown = plugin.isNodeShuttingDown(nodeName);

        if (nodeIsGoingToShutdown
                && item.task instanceof AbstractProject
                && !plugin.wasAlreadyQueued(item.id, nodeName)) {

            boolean otherNodeCanBuild = QueueUtils.canOtherNodeBuild(item, node);
            Set<String> upstreamProjects = QueueUtils.getUpstreamProjectNames(item);

            if (otherNodeCanBuild
                    || (!otherNodeCanBuild && !plugin.isAnyPermittedUpstreamProject(upstreamProjects, nodeName))) {
                logger.log(Level.FINE, "Preventing project {0} from running on node {1}, "
                        + "since lenient shutdown is active", new String[] {item.getDisplayName(), nodeName});
                blockage = new NodeShutdownBlockage();
            }
        }

        //Set the project as allowed upstream project if it was not blocked and node shutdown enabled:
        if (blockage == null && nodeIsGoingToShutdown) {
            AbstractProject project = (AbstractProject)item.task;
            plugin.addPermittedUpstreamProject(project, nodeName);
        }

        return blockage;
    }

}
