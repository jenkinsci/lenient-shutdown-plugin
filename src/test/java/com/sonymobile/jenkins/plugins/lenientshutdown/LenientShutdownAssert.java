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

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Assert methods used by the tests.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public final class LenientShutdownAssert {

    private static final int TIMEOUT_SECONDS = 60;

    /**
     * Hiding constructor for utility class.
     */
    private LenientShutdownAssert() { }

    /**
     * Waits until the {@code maxDuration} or the {@code isDone} method returns
     * {@code true}, whichever is first.
     *
     * @param maxDuration a {@link Duration} instance representing how long to
     *                    wait.
     * @param isDone      a method that returns whether it's done.
     * @throws InterruptedException if interrupted while sleeping.
     */
    private static void waitFor(
        final Duration maxDuration,
        final BooleanSupplier isDone
    ) throws InterruptedException {
        final Instant start = Instant.now();
        final Instant end = start.plus(maxDuration);
        while (Instant.now().isBefore(end)) {
            final boolean allFinished = isDone.getAsBoolean();
            if (allFinished) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
        }
    }

    /**
     * Asserts that argument projects are successfully built within a timely manner.
     * @param argumentProjects the projects to assert for success
     * @throws InterruptedException if something goes wrong
     */
    public static void assertSuccessfulBuilds(AbstractProject... argumentProjects) throws InterruptedException {
        List<AbstractProject> projects = Arrays.asList(argumentProjects);
        List<AbstractBuild> builds = new ArrayList<>(
                Collections.nCopies(argumentProjects.length, null));

        waitFor(Duration.ofSeconds(TIMEOUT_SECONDS), () -> {
            boolean allFinished = true;
            for (int i = 0; i < argumentProjects.length; i++) {
                AbstractProject project = projects.get(i);
                AbstractBuild build = project.getBuildByNumber(1);
                builds.set(i, build);

                if (build == null || build.isBuilding()) {
                    allFinished = false;
                    break;
                }
            }

            return allFinished;
        });

        for (AbstractBuild build : builds) {
            assertNotNull(build);
            assertEquals(Result.SUCCESS, build.getResult());
        }
        assertEquals(argumentProjects.length, builds.size());
    }

    /**
     * Asserts that argument slave goes temporarily offline within a timely manner.
     * @param slave the slave to assert for
     * @throws InterruptedException if something goes wrong
     */
    public static void assertSlaveGoesOffline(final DumbSlave slave) throws InterruptedException {
        waitFor(Duration.ofSeconds(TIMEOUT_SECONDS), () ->
            slave.getComputer().isTemporarilyOffline()
        );
        assertTrue(
            slave.toComputer().isTemporarilyOffline(),
            "Node should shut down after builds are complete"
        );
    }
}
