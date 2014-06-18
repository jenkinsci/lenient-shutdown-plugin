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

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for getting information about the build queue and ongoing builds.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public final class QueueUtils {

    /**
     * Hiding utility class constructor.
     */
    private QueueUtils() { }

    /**
     * Returns the set of project names that are in the build queue
     * AND have a completed upstream project.
     * Note: This method locks the queue; don't use excessively.
     * @return set of queued and allowed project names
     */
    public static Set<String> getAllowedQueueProjectNames() {
        Set<String> queuedProjects = new HashSet<String>();
        Queue queueInstance = Queue.getInstance();
        for (int id : getAllowedQueueItemIds()) {
            Queue.Item item = queueInstance.getItem(id);
            if (item.task instanceof AbstractProject) {
                AbstractProject project = (AbstractProject)item.task;
                queuedProjects.add(project.getFullName());
            }
        }
        return queuedProjects;
    }

    /**
     * Returns the set of item ids for items that are in the build queue
     * AND have a completed upstream project if they are a project build.
     * Note: This method locks the queue; don't use excessively.
     * @return set of item ids
     */
    public static Set<Integer> getAllowedQueueItemIds() {
        Set<Integer> queuedIds = new HashSet<Integer>();
        for (Queue.Item item : Queue.getInstance().getItems()) {
            if (item.task instanceof AbstractProject) {
                //Only add if it has a completed upstream build:
                for (AbstractBuild upstreamBuild : getUpstreamBuilds(item)) {
                    if (!upstreamBuild.isBuilding()) {
                        queuedIds.add(item.id);
                        break;
                    }
                }
            } else {
                queuedIds.add(item.id);
            }
        }
        return queuedIds;
    }

    /**
     * Returns the set of project names that have an ongoing build.
     * @return set of running project names
     */
    public static Set<String> getRunningProjectNames() {
        Set<String> runningProjects = new HashSet<String>();

        List<Node> allNodes = new ArrayList<Node>(Jenkins.getInstance().getNodes());
        allNodes.add(Jenkins.getInstance());

        for (Node node : allNodes) {
            Computer computer = node.toComputer();
            if (computer != null) {
                List<Executor> executors = new ArrayList<Executor>(computer.getExecutors());
                executors.addAll(computer.getOneOffExecutors());

                for (Executor executor : executors) {
                    Queue.Executable executable =  executor.getCurrentExecutable();
                    if (executable instanceof AbstractBuild) {
                        AbstractBuild build = (AbstractBuild)executable;
                        runningProjects.add(build.getProject().getFullName());
                    }
                }
            }
        }
        return runningProjects;
    }

    /**
     * Gets the names of all upstream projects that triggered argument queue item.
     * @param item the queue item to find upstream projects for
     * @return set of upstream project names
     */
    public static Set<String> getUpstreamProjectNames(Queue.Item item) {
        Set<String> upstreamProjects = new HashSet<String>();
        for (Cause cause : item.getCauses()) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause)cause;
                upstreamProjects.add(upstreamCause.getUpstreamProject());
            }
        }
        return upstreamProjects;
    }

    /**
     * Gets all upstream builds that triggered argument queue item.
     * @param item the queue item to find upstream builds for
     * @return set of upstream builds
     */
    public static Set<AbstractBuild> getUpstreamBuilds(Queue.Item item) {
        Set<AbstractBuild> upstreamBuilds = new HashSet<AbstractBuild>();
        for (Cause cause : item.getCauses()) {
            if (cause instanceof Cause.UpstreamCause) {
                Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause)cause;
                String upstreamProjectName = upstreamCause.getUpstreamProject();
                AbstractProject upstreamProject = (AbstractProject)Jenkins.getInstance()
                        .getItemByFullName(upstreamProjectName);

                if (upstreamProject != null) {
                    AbstractBuild upstreamBuild = upstreamProject
                            .getBuildByNumber(upstreamCause.getUpstreamBuild());
                    upstreamBuilds.add(upstreamBuild);
                }
            }
        }
        return upstreamBuilds;
    }

}
