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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.Queue.Item;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.NodeParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.BuildTrigger;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.sonymobile.jenkins.plugins.lenientshutdown.LenientShutdownAssert.assertSuccessfulBuilds;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for global lenient shutdown mode.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class GlobalLenientShutdownTest {

    /**
     * Jenkins rule instance.
     */
    // CS IGNORE VisibilityModifier FOR NEXT 3 LINES. REASON: Mocks tests.
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private static final int TIMEOUT_SECONDS = 60;
    private static final int QUIET_PERIOD = 15;
    private static final int NUM_EXECUTORS = 4;

    /**
     * Changes the number of executors on the Jenkins master.
     * Runs before every test.
     * @throws IOException if something goes wrong
     */
    @Before
    public void setUp() throws IOException {
        Jenkins jenkins = jenkinsRule.getInstance();
        jenkins.setNumExecutors(NUM_EXECUTORS);
        //TODO https://github.com/jenkinsci/jenkins/pull/1596 renders this workaround unnecessary
        jenkins.setNodes(jenkins.getNodes());
    }

    /**
     * Tests that the URL for activating shutdown mode works as expected.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testActivateShutdown() throws Exception {
        toggleLenientShutdown();
        assertTrue(ShutdownManageLink.getInstance().isGoingToShutdown());
    }

    /**
     * Tests that the URL for deactivating shutdown mode works as expected.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDeactivateShutdown() throws Exception {
        toggleLenientShutdown();
        toggleLenientShutdown();
        assertFalse(ShutdownManageLink.getInstance().isGoingToShutdown());
    }

    /**
     * Tests that all builds are started as normal when the shutdown mode has
     * not been initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBuildsDirectlyWhenShutdownDisabled() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        //Builds and waits until completion (timeout 1 min):
        FreeStyleBuild build = project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);

        assertEquals(Result.SUCCESS, build.getResult());
    }

    /**
     * Tests that builds without white listed upstreams are blocked
     * after shutdown mode is initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBlocksBuildWhenShutdownEnabled() throws Exception {
        Queue jenkinsQueue = Jenkins.getInstance().getQueue();
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        toggleLenientShutdown();
        project.scheduleBuild2(0);
        Item queueItem = jenkinsQueue.getItem(project);

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            queueItem = jenkinsQueue.getItem(project);
            if (queueItem.isBlocked()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }

        assertTrue(queueItem.isBlocked());
        assertEquals(Messages.IsAboutToShutDown(), queueItem.getWhy());
    }

    /**
     * Tests that blocked builds are allowed to run after shutdown mode is deactivated again.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDoesNotBlockAfterShutdownDisabledAgain() throws Exception {
        Queue jenkinsQueue = Jenkins.getInstance().getQueue();
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        toggleLenientShutdown();
        QueueTaskFuture buildFuture = project.scheduleBuild2(0);
        Item queueItem = jenkinsQueue.getItem(project);

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            queueItem = jenkinsQueue.getItem(project);
            if (queueItem.isBlocked()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        if (!queueItem.isBlocked()) {
            fail("Project was not blocked within time limit");
        }

        //Disables shutdown mode
        toggleLenientShutdown();

        FreeStyleBuild build = (FreeStyleBuild)buildFuture.get(1, TimeUnit.MINUTES); //Wait for build finish

        assertEquals(Result.SUCCESS, build.getResult());
    }

    /**
     * Tests that builds with white listed upstreams are allowed,
     * even though lenient shutdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDoesNotBlockDownstream() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        Jenkins.getInstance().rebuildDependencyGraph();

        // Gives lenient shutdown mode time to activate while parent is still building:
        parent.getBuildersList().add(new Shell("sleep 5"));

        //Trigger build of the first project, which starts the chain:
        parent.scheduleBuild2(0).waitForStart();

        toggleLenientShutdown();

        assertSuccessfulBuilds(parent, child, grandChild);
    }

    /**
     * Tests that builds with white listed upstreams are allowed,
     * even though lenient shutdown mode is active. The upstreams are defined
     * with Parameterized Trigger Plugin.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDoesNotBlockParameterizedPluginDownstream() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        BuildTriggerConfig childTrigger = new BuildTriggerConfig(child.getName(),
                ResultCondition.ALWAYS, new NodeParameters());
        BuildTriggerConfig grandChildTrigger = new BuildTriggerConfig(grandChild.getName(),
                ResultCondition.ALWAYS, new NodeParameters());

        parent.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(childTrigger));
        child.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(grandChildTrigger));
        Jenkins.getInstance().rebuildDependencyGraph();

        // Gives lenient shutdown mode time to activate while parent is still building:
        parent.getBuildersList().add(new Shell("sleep 5"));

        //Trigger build of the first project, which starts the chain:
        parent.scheduleBuild2(0).waitForStart();

        toggleLenientShutdown();

        assertSuccessfulBuilds(parent, child, grandChild);
    }

    /**
     * Tests that downstream triggers that have been added as build steps in
     * Parameterized Trigger Plugin are allowed to finish when lenient shutdown
     * is activated during build.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testParameterizedPluginBuildStep() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        // Gives lenient shutdown mode time to activate while parent is still building:
        parent.getBuildersList().add(new Shell("sleep 5"));

        BlockingBehaviour waitForDownstreamBehavior = new BlockingBehaviour(
                Result.FAILURE, Result.FAILURE, Result.UNSTABLE);

        BlockableBuildTriggerConfig childTrigger = new BlockableBuildTriggerConfig(child.getName(),
                waitForDownstreamBehavior, null);
        BlockableBuildTriggerConfig grandChildTrigger = new BlockableBuildTriggerConfig(grandChild.getName(),
                waitForDownstreamBehavior, null);

        parent.getBuildersList().add(new TriggerBuilder(childTrigger));
        child.getBuildersList().add(new TriggerBuilder(grandChildTrigger));

        Jenkins.getInstance().rebuildDependencyGraph();

        //Trigger build of the first project, which starts the chain:
        parent.scheduleBuild2(0).waitForStart();

        toggleLenientShutdown();

        assertSuccessfulBuilds(parent, child, grandChild);
    }

    /**
     * Tests that the lenient shutdown link on the manage page is visible.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testLinkIsVisible() throws Exception {
        HtmlPage managePage = jenkinsRule.createWebClient().goTo("manage");
        assertTrue(managePage.asText().contains(ShutdownManageLink.getInstance().getDisplayName()));
    }

    /**
     * Tests that projects that are in queue when lenient shutdown is enabled
     * are allowed to build if they have a completed upstream project.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testDoesNotBlockQueuedWithCompletedUpstream() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);
        Jenkins.getInstance().rebuildDependencyGraph();

        //Trigger build of the parent project, and wait for it to finish:
        parent.scheduleBuild2(0).get();

        //Wait for the child project to queue up (in quiet period)
        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            AbstractProject firstQueued = (AbstractProject)Queue.getInstance().getItems()[0].task;
            if (firstQueued.equals(child)) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        if (elapsedSeconds >= TIMEOUT_SECONDS) {
            fail("Child project was not queued up within time limit");
        }

        toggleLenientShutdown();

        assertSuccessfulBuilds(parent, child, grandChild);
    }

    /**
     * Tests that projects that are in queue when lenient shutdown is enabled
     * are blocked if they don't have an upstream project.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testBlocksQueuedWithoutUpstream() throws Exception {
        Queue queue = Queue.getInstance();

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.scheduleBuild2(QUIET_PERIOD);

        //Wait for the project to queue up (in quiet period)
        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            if (queue.getItems().length > 0) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        if (elapsedSeconds >= TIMEOUT_SECONDS) {
            fail("Project was not queued up within time limit");
        }

        toggleLenientShutdown();

        Item queueItem = queue.getItem(project);
        elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            queueItem = queue.getItem(project);
            if (queueItem.isBlocked()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }

        assertTrue(queueItem.isBlocked());
        assertEquals(Messages.IsAboutToShutDown(), queueItem.getWhy());
    }

    /**
     * Toggles the lenient shutdown mode using the plugin URL.
     * @throws Exception if something goes wrong
     */
    private void toggleLenientShutdown() throws Exception {
        jenkinsRule.createWebClient().goTo(ShutdownManageLink.getInstance().getUrlName());
    }


}
