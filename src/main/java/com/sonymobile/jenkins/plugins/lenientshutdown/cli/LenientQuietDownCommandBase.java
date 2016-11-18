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

package com.sonymobile.jenkins.plugins.lenientshutdown.cli;

import java.util.Arrays;

import org.kohsuke.args4j.Option;

import com.sonymobile.jenkins.plugins.lenientshutdown.Messages;
import com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownConfiguration;

import hudson.Util;
import hudson.cli.CLICommand;

/**
 * Base class for the lenient quiet down commands that accept parameters.
 *
 */
public abstract class LenientQuietDownCommandBase extends CLICommand {

    private static final String DELIMETER = ";";

    // CS IGNORE VisibilityModifier FOR NEXT 7 LINES. REASON: How it's meant to be done
    /**
     * The system message to show on every page.
     *
     * @see com.sonymobile.jenkins.plugins.lenientshutdown.ShutdownDecorator
     */
    @Option(name = "-m", aliases = { "--message" }, usage = "The system message to display on every page.",
            required = false)
    private String message;

    /**
     * Allow all queued items or only those triggered by an upstream project.
     *
     */
    @Option(name = "-a", aliases = { "--allow-queued" }, usage = "Allow all queued items to finish.",
            required = false)
    private boolean allowAllQueuedItemsOption;

    /**
     * Allow white listed projects to run when lenient shutdown is active.
     *
     */
    @Option(name = "-w", aliases = { "--allow-whitelisted" },
            usage = "Allow white listed projects to run while queue not empty.", required = false)
    private boolean allowWhiteListedProjectsOption;

    /**
     * The list of white listed projects.
     *
     */
    @Option(name = "-p", aliases = { "--whitelisted-projects" },
            usage = "A semicolon separated list of white listed projects.",
            required = false)
    private String whiteListedProjects;

    /**
     * transfer the options to the configuration.
     *
     */
    protected void configure() {
        ShutdownConfiguration config = ShutdownConfiguration.getInstance();
        if (Util.fixEmpty(message) != null) {
            config.setShutdownMessage(message);
            config.save();
        }
        config.setAllowAllQueuedItems(allowAllQueuedItemsOption);
        config.setAllowWhiteListedProjects(allowWhiteListedProjectsOption);
        if (whiteListedProjects != null) {
            config.getWhiteListedProjects().clear();
            config.getWhiteListedProjects()
                    .addAll(Arrays.asList(whiteListedProjects.split(DELIMETER)));
        }
    }

    /**
     * print the current shutdown configuration.
     *
     */
    protected void printShutdownConfiguration() {

        ShutdownConfiguration config = ShutdownConfiguration.getInstance();
        if (config.isAllowAllQueuedItems()) {
            stdout.println(Messages.AllQueuedItemsAllowed());
        } else {
            stdout.println(Messages.OnlyUpstreamItemsAllowed());
        }

        if (config.isAllowWhiteListedProjects()) {
            stdout.println(Messages.WhiteListedProjectsAllowed());
            stdout.println(config.getWhiteListedProjectsText());
        } else {
            stdout.println(Messages.WhiteListedProjectsNotAllowed());
        }
    }
}
