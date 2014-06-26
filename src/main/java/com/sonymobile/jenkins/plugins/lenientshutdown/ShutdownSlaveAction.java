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

import hudson.Functions;
import hudson.model.Computer;
import hudson.model.RootAction;
import hudson.model.User;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Action to be displayed on computer pages for turning slaves
 * offline leniently.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class ShutdownSlaveAction implements RootAction {

    /**
     * URL to this action.
     */
    public static final String URL = "lenientshutdown";

    /**
     * Icon visible when it's possible to enable lenient shutdown for a slave.
     */
    public static final String ENABLE_ICON = "/images/system-log-out-small.png";

    /**
     * Icon visible when it's possible to disable lenient shutdown for a slave.
     */
    public static final String DISABLE_ICON = "edit-delete.png";

    private final Computer computer;

    /**
     * Constructor.
     * @param computer the computer for which this action is created
     */
    public ShutdownSlaveAction(Computer computer) {
        this.computer = computer;
    }

    @Override
    public String getIconFileName() {
        String icon = null;

        User currentUser = User.current();

        if (computer != null && !computer.isTemporarilyOffline()
                && currentUser != null && currentUser.hasPermission(getRequiredPermission())) {
            PluginImpl plugin = PluginImpl.getInstance();
            if (plugin.isNodeShuttingDown(computer.getName())) {
                icon =  DISABLE_ICON;
            } else {
                icon = Functions.getResourcePath() + "/plugin/" + getUrlName() + ENABLE_ICON;
            }
        }
        return icon;
    }

    @Override
    public String getDisplayName() {
        PluginImpl plugin = PluginImpl.getInstance();
        if (plugin.isNodeShuttingDown(computer.getName())) {
            return Messages.CancelOfflineLeniently();
        }
        return Messages.TakeOfflineLeniently();
    }

    @Override
    public String getUrlName() {
        return URL;
    }

    /**
     * Called when lenient shutdown for a specific node is activated.
     * @return redirect response
     * @throws IOException if something goes wrong
     */
    public HttpResponse doIndex() throws IOException {
        Jenkins.getInstance().checkPermission(getRequiredPermission());

        final PluginImpl plugin = PluginImpl.getInstance();
        final String nodeName = computer.getName();

        if (plugin.isNodeShuttingDown(nodeName)) {
            plugin.toggleNodeShuttingDown(nodeName);
        } else {
            if (QueueUtils.isBuilding(computer) || QueueUtils.hasNodeExclusiveItemInQueue(computer)) {
                //Doing some work; we want to take offline leniently
                plugin.toggleNodeShuttingDown(nodeName);
                plugin.setOfflineByUser(nodeName, User.current());

                ExecutorService service = Executors.newSingleThreadExecutor();
                service.submit(new Runnable() {
                    @Override
                    public void run() {
                        Set<String> permittedUpstreamProjectNames = plugin.getPermittedUpstreamProjects(nodeName);
                        permittedUpstreamProjectNames.clear();
                        permittedUpstreamProjectNames.addAll(QueueUtils.getRunningProjectNames(nodeName));
                        permittedUpstreamProjectNames.addAll(QueueUtils.getPermittedQueueProjectNames(nodeName));

                        Set<Integer> alreadyQueuedItemIds = plugin.getAlreadyQueuedItemIds(nodeName);
                        alreadyQueuedItemIds.clear();
                        alreadyQueuedItemIds.addAll(QueueUtils.getPermittedQueueItemIds(nodeName));
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

        return HttpResponses.redirectTo("../");
    }

    /**
     * Returns required permission to change lenient disconnect mode for slaves.
     * @return Jenkins administer permission.
     */
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

}
