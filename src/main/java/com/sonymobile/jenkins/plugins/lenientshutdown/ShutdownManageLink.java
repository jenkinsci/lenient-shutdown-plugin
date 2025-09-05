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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.security.SecurityContextExecutorService;

/**
 * Adds a link on the manage Jenkins page for lenient shutdown.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@Extension
public class ShutdownManageLink extends ManagementLink {

    /**
     * The list of queue ids, that belong to projects that where running at time of lenient shutdown
     * and any of the downstream builds.
     */
    private Set<Long> permittedQueueIds = Collections.synchronizedSet(new HashSet<Long>());

    /**
     * The list of queue ids that correspond to running builds of permitted queue ids
     */
    private Set<Long> activeQueueIds = Collections.synchronizedSet(new HashSet<Long>());

    /**
     * The list of queue ids belonging to white listed projects runs
     */
    private Set<Long> whiteListedQueueIds = Collections.synchronizedSet(new HashSet<Long>());

    private boolean isGoingToShutdown;
    private boolean analyzing;
    private static ShutdownManageLink instance;

    /**
     * URL to the plugin.
     */
    private static final String URL = "lenientshutdown";

    /**
     * Icon used by this plugin.
     */
    private static final String ICON = "symbol-power";

    /**
     * Returns the instance of ShutdownMangeLink.
     * @return instance the ShutdownMangeLink.
     */
    public static ShutdownManageLink getInstance() {
        List<ManagementLink> list = Jenkins.getInstance().getManagementLinks();
        for (ManagementLink link : list) {
            if (link instanceof ShutdownManageLink) {
                instance = (ShutdownManageLink)link;
                break;
            }
        }
        return instance;
    }

    /**
     * Gets the icon for this plugin.
     * @return the icon
     */
    @Override
    public String getIconFileName() {
        return ICON;
    }

    /**
     * Gets the display name for this plugin on the management page.
     * Varies depending on if lenient shutdown is already enabled or not.
     * @return display name
     */
    @Override
    public String getDisplayName() {
        if (isGoingToShutdown()) {
            return Messages.CancelShutdownTitle();
        }
        return Messages.ActivateShutdownTitle();
    }

    /**
     * Gets the url name for this plugin.
     * @return url name
     */
    @Override
    public String getUrlName() {
        if (isGoingToShutdown) {
            return "cancelLenientShutdown";
        } else {
            return URL;
        }
    }

    /**
     * Gets the description of this plugin.
     * Varies depending on if lenient shutdown is already enabled or not.
     * @return description
     */
    @Override
    public String getDescription() {
        String description = null;
        if (!isGoingToShutdown()) {
            description = Messages.Description();
        }
        return description;
    }

    /**
     * Returns required permission to change the global shutdown mode.
     * @return Jenkins administer permission.
     */
    @Override
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    /**
     * Toggle the lenient shutdown state.
     */
    public void toggleGoingToShutdown() {
        isGoingToShutdown = !isGoingToShutdown;
        analyzing = isGoingToShutdown;
    }

    /**
     * Checks if Jenkins has been put to lenient shutdown mode.
     * @return true if Jenkins is in lenient shutdown mode, otherwise false
     */
    public boolean isGoingToShutdown() {
        return isGoingToShutdown;
    }

    /**
     * The singleton instance registered in the Jenkins extension list.
     *
     * @return the instance
     */
    public ShutdownConfiguration getConfiguration() {
        return ShutdownConfiguration.getInstance();
    }


    /**
     * Method triggered when pressing the management link.
     * Toggles the lenient shutdown mode.
     *
     * @param req StaplerRequest
     * @param rsp StaplerResponse
     * @throws IOException if unable to redirect
     */
    public synchronized void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Jenkins.getInstance().checkPermission(getRequiredPermission());

        performToggleGoingToShutdown();
        rsp.sendRedirect2(req.getContextPath() + "/manage");
    }

    /**
     * Toggles the flag and prepares for lenient shutdown if needed.
     *
     */
    public void performToggleGoingToShutdown() {
        toggleGoingToShutdown();
        if (isGoingToShutdown()) {
            ExecutorService service = new SecurityContextExecutorService(Executors.newSingleThreadExecutor());
            service.submit(new Runnable() {
                @Override
                public void run() {
                    permittedQueueIds.clear();
                    activeQueueIds.clear();
                    whiteListedQueueIds.clear();
                    permittedQueueIds.addAll(QueueUtils.getPermittedQueueItemIds());
                    permittedQueueIds.addAll(QueueUtils.getRunningProjectQueueIds());
                    activeQueueIds.addAll(permittedQueueIds);
                    analyzing = false;
                }
            });
        }
    }

    /**
     * Helper method for testing.
     *
     * @return true if analysis, which projects are in the queue and running, is still ongoing, false
     *         otherwise
     */
    public boolean isAnalyzing() {
        return analyzing;
    }

    /**
     * Checks if any of the queue ids in argument list is in the list of permitted queue ids.
     *
     * @param queueIds the list of queue ids to check
     * @return true if at least one of the projects is white listed
     */
    public boolean isAnyPermittedUpstreamProject(Set<Long> queueIds) {
        Collection<?> intersection = CollectionUtils.intersection(queueIds, permittedQueueIds);
        return !intersection.isEmpty();
    }

    /**
     * Checks if any of the queue ids in argument list is coming from a white listed project run.
     *
     * @param queueIds the list of queue ids to check
     * @return true if at least one of the projects is white listed
     */
    public boolean isAnyWhiteListedUpstreamProject(Set<Long> queueIds) {
        Collection<?> intersection = CollectionUtils.intersection(queueIds, whiteListedQueueIds);
        return !intersection.isEmpty();
    }

    /**
     * Checks whether there are still any permitted builds running.
     *
     * @return true if a permitted build is still running
     */
    public boolean isActiveQueueIds() {
        return !activeQueueIds.isEmpty();
    }

    /**
     * Adds the queue id of a permitted build to the active queue ids.
     *
     * @param id the queue id to add
     */
    public void addActiveQueueId(long id) {
        activeQueueIds.add(id);
    }

    /**
     * Removes the queue id of a permitted build from the active queue ids.
     * This is triggered when the build is finished.
     *
     * @param id the queue id to remove
     */
    public void removeActiveQueueId(long id) {
        activeQueueIds.remove(id);
    }

    /**
     * Adds a queue id to the set of permitted upstream queue ids.
     *
     * @param id the queue id to add to white list
     */
    public void addPermittedUpstreamQueueId(long id) {
        permittedQueueIds.add(id);
    }

    /**
     * Returns true if id is a permitted queue id.
     *
     * @param id the queue item id to check for
     * @return true if it was queued
     */
    public boolean isPermittedQueueId(long id) {
        return permittedQueueIds.contains(id);
    }

    /**
     * Adds the queue id to the set of white listed queue ids.
     *
     * @param id the queue id to add
     */
    public void addWhiteListedQueueId(long id) {
        whiteListedQueueIds.add(id);
    }
}
