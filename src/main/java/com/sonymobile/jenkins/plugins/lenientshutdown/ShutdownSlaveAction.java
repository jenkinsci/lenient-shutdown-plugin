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

import hudson.model.Computer;
import hudson.model.RootAction;
import hudson.util.HttpResponses;
import org.kohsuke.stapler.HttpResponse;

import java.io.IOException;

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
    public static final String ENABLE_ICON = "symbol-power icon-md";

    /**
     * Icon visible when it's possible to disable lenient shutdown for a slave.
     */
    public static final String DISABLE_ICON = "symbol-none icon-md";

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

        if (computer != null && !computer.isTemporarilyOffline()) {
            PluginImpl plugin = PluginImpl.getInstance();
            if (plugin.isNodeShuttingDown(computer.getName()) && computer.hasPermission(Computer.CONNECT)) {
                icon =  DISABLE_ICON;
            } else if (computer.hasPermission(Computer.DISCONNECT)) {
                icon = ENABLE_ICON;
            }
        }
        return icon;
    }

    @Override
    public String getDisplayName() {
        PluginImpl plugin = PluginImpl.getInstance();
        if (plugin.isNodeShuttingDown(computer.getName()) && computer.hasPermission(Computer.CONNECT)) {
            return Messages.CancelOfflineLeniently();
        } else if (computer.hasPermission(Computer.DISCONNECT)) {
            return Messages.TakeOfflineLeniently();
        }
        return null;
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
        final PluginImpl plugin = PluginImpl.getInstance();
        final String nodeName = computer.getName();

        if (plugin.isNodeShuttingDown(nodeName)) {
            computer.checkPermission(Computer.CONNECT);
            plugin.toggleNodeShuttingDown(nodeName);
        } else {
            computer.checkPermission(Computer.DISCONNECT);
            PluginImpl.getInstance().setNodeOffline(computer);
        }

        return HttpResponses.redirectTo("../");
    }


}
