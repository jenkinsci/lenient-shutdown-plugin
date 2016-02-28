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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;

import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Adds a link on the manage Jenkins page for lenient shutdown.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@Extension
public class ShutdownManageLink extends ManagementLink {

    private Set<String> permittedUpstreamProjectNames = Collections.synchronizedSet(new HashSet<String>());
    private Set<Integer> alreadyQueuedItemIds = Collections.synchronizedSet(new HashSet<Integer>());

    private boolean isGoingToShutdown;
    private static ShutdownManageLink instance;

    /**
     * URL to the plugin.
     */
    private static final String URL = "lenientshutdown";

    /**
     * Icon used by this plugin.
     */
    private static final String ICON = "system-log-out.png";

    /**
     * Returns the instance of ShutdownMangeLink.
     * 
     * @return instance the ShutdownMangeLink.
     */
    public static ShutdownManageLink getInstance() {
        List<ManagementLink> list = Hudson.getInstance().getManagementLinks();
        for (ManagementLink link : list) {
            if (link instanceof ShutdownManageLink) {
                instance = (ShutdownManageLink) link;
                break;
            }
        }
        return instance;
    }

    /**
     * Gets the icon for this plugin.
     * 
     * @return the icon
     */
    @Override
    public String getIconFileName() {
        return ICON;
    }

    /**
     * Gets the display name for this plugin on the management page.
     * Varies depending on if lenient shutdown is already enabled or not.
     * 
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
     * 
     * @return url name
     */
    @Override
    public String getUrlName() {
        return isGoingToShutdown ? "cancelLenientShutdown" : URL;
    }

    /**
     * Gets the description of this plugin.
     * Varies depending on if lenient shutdown is already enabled or not.
     * 
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
     * 
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
    }

    /**
     * Checks if Jenkins has been put to lenient shutdown mode.
     * 
     * @return true if Jenkins is in lenient shutdown mode, otherwise false
     */
    public boolean isGoingToShutdown() {
        return isGoingToShutdown;
    }

    public ShutdownConfiguration getConfiguration() {
        return ShutdownConfiguration.getInstance();
    }

    public synchronized void doCancelLenientShutdown(StaplerRequest req, StaplerResponse rsp)
            throws IOException, InterruptedException, ExecutionException {
        Jenkins.getInstance().checkPermission(getRequiredPermission());

        performToggleGoingToShutdown();
        rsp.sendRedirect2(req.getContextPath() + "/manage");
    }

    public synchronized void doLenientShutdown(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException, InterruptedException, ExecutionException {
        Jenkins.getInstance().checkPermission(getRequiredPermission());
        JSONObject src = req.getSubmittedForm();
        getConfiguration().setAllowAllQueuedItems(src.getBoolean("allowAllQueuedItems"));
        performToggleGoingToShutdown();
        rsp.sendRedirect2(req.getContextPath() + "/manage");
    }

    /**
     * Toggles the flag and prepares for lenient shutdown if needed.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void performToggleGoingToShutdown() {
        toggleGoingToShutdown();
        if (isGoingToShutdown()) {
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(new Runnable() {
                @Override
                public void run() {
                    alreadyQueuedItemIds.clear();
                    alreadyQueuedItemIds.addAll(QueueUtils.getPermittedQueueItemIds());

                    permittedUpstreamProjectNames.clear();
                    permittedUpstreamProjectNames.addAll(QueueUtils.getRunningProjectNames());
                    permittedUpstreamProjectNames.addAll(QueueUtils.getPermittedQueueProjectNames());
                }
            });
        }
    }

    /**
     * Checks if any of the project names in argument list are marked as white listed upstream projects.
     * 
     * @param projectNames
     *            the list of project names to check
     * @return true if at least one of the projects is white listed
     */
    public boolean isAnyPermittedUpstreamProject(Set<String> projectNames) {
        Collection intersection = CollectionUtils.intersection(projectNames, permittedUpstreamProjectNames);
        return !intersection.isEmpty();
    }

    /**
     * Adds argument project to the set of white listed upstream project names.
     * 
     * @param project
     *            the project to add to white list
     */
    public void addPermittedUpstreamProject(AbstractProject project) {
        permittedUpstreamProjectNames.add(project.getFullName());
    }

    /**
     * Returns true if argument queue item was queued when lenient shutdown was activated.
     * 
     * @param id
     *            the queue item id to check for
     * @return true if it was queued
     */
    public boolean wasAlreadyQueued(int id) {
        return alreadyQueuedItemIds.contains(id);
    }

}
