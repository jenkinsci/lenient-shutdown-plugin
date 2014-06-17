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
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.List;

/**
* Adds a link on the manage Jenkins page for lenient shutdown.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
*/
@Extension
public class ShutdownManageLink extends ManagementLink  {

    private boolean isGoingToShutdown;
    private static ShutdownManageLink instance;

    /**
     * URL to the plugin.
     */
    private static final String URL = "lenient-shutdown-plugin";

    /**
     * Icon used by this plugin.
     */
    private static final String ICON = "system-log-out.png";

    /**
     * Returns the instance of ShutdownMangeLink.
     * @return instance the ShutdownMangeLink.
     */
    public static ShutdownManageLink getInstance() {
        List<ManagementLink> list = Hudson.getInstance().getManagementLinks();
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
     * @return display name
     */
    @Override
    public String getDisplayName() {
        return Messages.LinkTitle();
    }

    /**
     * Gets the url name for this plugin.
     * @return url name
     */
    @Override
    public String getUrlName() {
        return URL;
    }

    /**
     * Toggle the lenient shutdown state.
     */
    public void toggleGoingToShutdown() {
        isGoingToShutdown = !isGoingToShutdown;
    }

    /**
     * Checks if Jenkins has been put to lenient shutdown mode.
     * @return true if Jenkins is in lenient shutdown mode, otherwise false
     */
    public boolean isGoingToShutdown() {
        return isGoingToShutdown;
    }

    /**
     * Method triggered when pressing the management link.
     * Toggles the lenient shutdown mode.
     * @param req StaplerRequest
     * @param rsp StaplerResponse
     * @throws IOException if unable to redirect
     */
    public synchronized void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        toggleGoingToShutdown();
        rsp.sendRedirect2(req.getContextPath() + "/manage");
    }

}
