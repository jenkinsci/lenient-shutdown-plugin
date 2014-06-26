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

import hudson.Plugin;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.util.CopyOnWriteMap;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Plugin base class.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class PluginImpl extends Plugin {

    /**
     * Node name -> is in lenient offline mode
     */
    private transient Map<String, Boolean> lenientOfflineSlaves = new CopyOnWriteMap.Hash<String, Boolean>();

    /**
     * Node name -> triggered offline by user
     */
    private transient Map<String, User> userTriggers = new CopyOnWriteMap.Hash<String, User>();

    /**
     * Node name -> Set of project names that are allowed to trigger a downstream
     */
    private Map<String, Set<String>> permittedSlaveUpstreamProjects = Collections.synchronizedMap(
            new HashMap<String, Set<String>>());

    /**
     * Node name -> Set of queue item ids that are allowed to build
     */
    private Map<String, Set<Integer>> permittedSlaveQueuedItemIds = Collections.synchronizedMap(
            new HashMap<String, Set<Integer>>());

    /**
     * Returns this singleton instance.
     *
     * @return the singleton.
     */
    public static PluginImpl getInstance() {
        return Jenkins.getInstance().getPlugin(PluginImpl.class);
    }

    /**
     * Checks if argument node is shutting down leniently.
     * @param nodeName the slave name to check
     * @return true if node is shutting down, otherwise false
     */
    public boolean isNodeShuttingDown(String nodeName) {
        return lenientOfflineSlaves.containsKey(nodeName)
                && lenientOfflineSlaves.get(nodeName);
    }

    /**
     * Toggles lenient shutdown mode for argument node.
     * @param nodeName the node name to toggle for
     */
    public synchronized void toggleNodeShuttingDown(String nodeName) {
        Boolean nodeShuttingDown = lenientOfflineSlaves.get(nodeName);
        if (nodeShuttingDown == null) {
            lenientOfflineSlaves.put(nodeName, true);
        } else {
            lenientOfflineSlaves.put(nodeName, !nodeShuttingDown);
        }
    }

    /**
     * Checks if any of the project names in argument list are marked as white listed upstream
     * projects for a specific slave.
     * @param projectNames the list of project names to check
     * @param nodeName the specific slave name to check for
     * @return true if at least one of the projects is white listed
     */
    public boolean isAnyPermittedUpstreamProject(Set<String> projectNames, String nodeName) {
        boolean isPermitted = false;
        Set<String> permittedProjectNames = getPermittedUpstreamProjects(nodeName);
        if (permittedProjectNames != null) {
            Collection intersection = CollectionUtils.intersection(projectNames, permittedProjectNames);
            isPermitted = !intersection.isEmpty();
        }
        return isPermitted;
    }

    /**
     * Returns true if argument queue item was queued when lenient shutdown was activated
     * for a specific slave.
     * @param id the queue item id to check for
     * @param nodeName the specific slave name to check for
     * @return true if it was queued
     */
    public boolean wasAlreadyQueued(int id, String nodeName) {
        Set<Integer> alreadyQueuedItemIds = getAlreadyQueuedItemIds(nodeName);
        return alreadyQueuedItemIds.contains(id);
    }


    /**
     * Adds argument project as a permitted upstream project for a specific slave.
     * @param project the project to add
     * @param nodeName the slave name to add the permitted project for
     */
    public void addPermittedUpstreamProject(AbstractProject project, String nodeName) {
        Set<String> permittedUpstreamProjectNames = getPermittedUpstreamProjects(nodeName);
        permittedUpstreamProjectNames.add(project.getFullName());
    }

    /**
     * Sets the argument user as the user that put the argument node in lenient offline mode.
     * @param nodeName the node that was put in lenient offline mode
     * @param user the user that put the node into lenient offline mode
     */
    public void setOfflineByUser(String nodeName, User user) {
        userTriggers.put(nodeName, user);
    }

    /**
     * Gets the user that put the argument node in lenient offline mode.
     * @param nodeName the node to get user for
     * @return user that put the slave in lenient offline mode
     */
    public User getOfflineByUser(String nodeName) {
        User user = userTriggers.get(nodeName);
        if (user == null) {
            user = User.getUnknown();
        }
        return user;
    }

    /**
     * Gets the set of permitted upstream projects for a specific node.
     * @param nodeName node name to list permitted upstreams for
     * @return set of permitted upstream projects.
     */
    public synchronized Set<String> getPermittedUpstreamProjects(String nodeName) {
        Set<String> permittedUpstreamProjects = permittedSlaveUpstreamProjects.get(nodeName);
        if (permittedUpstreamProjects == null) {
            permittedUpstreamProjects = new HashSet<String>();
            permittedSlaveUpstreamProjects.put(nodeName, permittedUpstreamProjects);
        }
        return permittedUpstreamProjects;
    }

    /**
     * Gets all item ids that were queued and could only be build on
     * the argument specific slave when it was set to lenient offline mode.
     * @param nodeName the node to get specific queue items for
     * @return set of queued item ids
     */
    public synchronized Set<Integer> getAlreadyQueuedItemIds(String nodeName) {
        Set<Integer> alreadyQueuedItemIds = permittedSlaveQueuedItemIds.get(nodeName);
        if (alreadyQueuedItemIds == null) {
            alreadyQueuedItemIds = new HashSet<Integer>();
            permittedSlaveQueuedItemIds.put(nodeName, alreadyQueuedItemIds);
        }
        return alreadyQueuedItemIds;
    }


}
