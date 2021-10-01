/*
 *  The MIT License
 *
 *  Copyright (c) 2014 Sony Mobile Communications Inc. All rights reserved.
 *  Copyright (c) 2016 Markus Winter. All rights reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.Plugin;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.User;
import hudson.util.CopyOnWriteMap;
import jenkins.model.Jenkins;
import jenkins.security.SecurityContextExecutorService;

/**
 * Plugin base class.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class PluginImpl extends Plugin {

    /**
     * Node name -> is in lenient offline mode
     */
    private transient Map<String, Boolean> lenientOfflineNodes = new CopyOnWriteMap.Hash<String, Boolean>();

    /**
     * Node name -> triggered offline by user
     */
    private transient Map<String, User> userTriggers = new CopyOnWriteMap.Hash<String, User>();

    /**
     * Node name -> Set of queue item ids that are allowed to build
     */
    private Map<String, Set<Long>> permittedNodeQueuedItemIds = Collections.synchronizedMap(
            new HashMap<String, Set<Long>>());

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
     * @param nodeName the node name to check
     * @return true if node is shutting down, otherwise false
     */
    public boolean isNodeShuttingDown(String nodeName) {
        return lenientOfflineNodes.containsKey(nodeName)
                && lenientOfflineNodes.get(nodeName);
    }

    /**
     * Toggles lenient shutdown mode for argument node.
     * @param nodeName the node name to toggle for
     */
    public synchronized void toggleNodeShuttingDown(String nodeName) {
        Boolean nodeShuttingDown = lenientOfflineNodes.get(nodeName);
        if (nodeShuttingDown == null) {
            lenientOfflineNodes.put(nodeName, true);
        } else {
            lenientOfflineNodes.put(nodeName, !nodeShuttingDown);
        }
    }

    /**
     * Actually sets the node offline or prepares it to be leniently and then later offline.
     *
     * @param computer the computer.
     */
    public void setNodeOffline(final Computer computer) {
        if (computer == null) {
            return;
        }
        final Node node = computer.getNode();
        if (node == null) {
            return;
        }
        if (QueueUtils.isBuilding(computer) || QueueUtils.hasNodeExclusiveItemInQueue(computer)) {
            //Doing some work; we want to take offline leniently
            final String nodeName = node.getNodeName();
            toggleNodeShuttingDown(nodeName);
            setOfflineByUser(nodeName, User.current());

            ExecutorService service = new SecurityContextExecutorService(Executors.newSingleThreadExecutor());
            service.submit(new Runnable() {
                @Override
                public void run() {
                    Set<Long> permittedQueuedItemIds = getPermittedQueuedItemIds(nodeName);
                    permittedQueuedItemIds.clear();
                    permittedQueuedItemIds.addAll(QueueUtils.getPermittedQueueItemIds(nodeName));
                    permittedQueuedItemIds.addAll(QueueUtils.getRunninProjectsQueueIDs(nodeName));
                }
            });

        } else { //No builds; we can take offline directly
            User currentUser = User.current();
            if (currentUser == null) {
                currentUser = User.getUnknown();
            }
            computer.setTemporarilyOffline(true, new LenientOfflineCause(currentUser));
        }
    }

    /**
     * Checks if any of the project names in argument list are marked as white listed upstream
     * projects for a specific node.
     * @param queueItemsIds the list of project names to check
     * @param nodeName the specific node name to check for
     * @return true if at least one of the projects is white listed
     */
    @Restricted(NoExternalUse.class)
    public boolean isAnyPermittedUpstreamQueueId(Set<Long> queueItemsIds, String nodeName) {
        boolean isPermitted = false;
        Set<Long> permittedQueueItemIds = getPermittedQueuedItemIds(nodeName);
        if (permittedQueueItemIds != null) {
            Collection<?> intersection = CollectionUtils.intersection(queueItemsIds, permittedQueueItemIds);
            isPermitted = !intersection.isEmpty();
        }
        return isPermitted;
    }

    /**
     * Returns true if argument queue item was queued when lenient shutdown was activated
     * for a specific node.
     * @param id the queue item id to check for
     * @param nodeName the specific node name to check for
     * @return true if it was queued
     */
    @Restricted(NoExternalUse.class)
    public boolean wasAlreadyQueued(long id, String nodeName) {
        Set<Long> alreadyQueuedItemIds = getPermittedQueuedItemIds(nodeName);
        return alreadyQueuedItemIds.contains(id);
    }

    /**
     * Adds argument project as a permitted upstream project for a specific node.
     * @param id the queue id to add
     * @param nodeName the node name to add the permitted project for
     */
    @Restricted(NoExternalUse.class)
    public void addPermittedUpstreamQueueId(long id, String nodeName) {
        Set<Long> permittedUpstreamProjectNames = getPermittedQueuedItemIds(nodeName);
        permittedUpstreamProjectNames.add(id);
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
     * @return user that put the node in lenient offline mode
     */
    public User getOfflineByUser(String nodeName) {
        User user = userTriggers.get(nodeName);
        if (user == null) {
            user = User.getUnknown();
        }
        return user;
    }

    /**
     * Gets all item ids that were queued and could only be build on
     * the argument specific node when it was set to lenient offline mode.
     * @param nodeName the node to get specific queue items for
     * @return set of queued item ids
     */
    @Restricted(NoExternalUse.class)
    public synchronized Set<Long> getPermittedQueuedItemIds(String nodeName) {
        Set<Long> permittedQueuedItemIds = permittedNodeQueuedItemIds.get(nodeName);
        if (permittedQueuedItemIds == null) {
            permittedQueuedItemIds = new HashSet<Long>();
            permittedNodeQueuedItemIds.put(nodeName, permittedQueuedItemIds);
        }
        return permittedQueuedItemIds;
    }

}
