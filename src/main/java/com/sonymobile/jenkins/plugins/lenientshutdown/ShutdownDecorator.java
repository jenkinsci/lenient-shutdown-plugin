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
import hudson.model.PageDecorator;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Adds a header about the lenient shutdown mode when it's active.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@Extension
public class ShutdownDecorator extends PageDecorator {

    /**
     * Defines the default shutdown message to be displayed in header.
     */
    private String shutdownMessage = Messages.GoingToShutDown();

    /**
     * Constructor, loads persisted configuration.
     */
    public ShutdownDecorator() {
        load();
    }

    /**
     * Checks if Jenkins has been put to lenient shutdown mode.
     * @return true if Jenkins is in lenient shutdown mode, otherwise false
     */
    public boolean isGoingToShutdown() {
        return ShutdownManageLink.getInstance().isGoingToShutdown();
    }

    /**
     * Gets the shutdown message to be displayed in header.
     * @return message to display in header
     */
    public String getShutdownMessage() {
        return shutdownMessage;
    }

    /**
     * Called when an admin saves settings in the global configuration page.
     * Persists the current settings to disk.
     * @param staplerRequest the request
     * @param json form data
     * @return always true
     * @throws FormException if the form was malformed
     */
    @Override
    public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
        shutdownMessage = json.getString("shutdownMessage");
        save();
        return true;
    }


}
