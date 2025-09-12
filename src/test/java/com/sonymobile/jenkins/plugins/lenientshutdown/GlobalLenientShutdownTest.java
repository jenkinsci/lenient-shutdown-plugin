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

import static com.sonymobile.jenkins.plugins.lenientshutdown.LenientShutdownAssert.assertSuccessfulBuilds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import hudson.security.ACL;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.SleepBuilder;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Queue.Item;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.NodeParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.tasks.BuildTrigger;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Test class for global lenient shutdown mode.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
@WithJenkins
class GlobalLenientShutdownTest {

    private static final int JOB_SLEEP_TIME = 5000;

    /**
     * Jenkins rule instance.
     */
    private JenkinsRule j;

    private static final int TIMEOUT_SECONDS = 60;
    private static final int QUIET_PERIOD = 5;
    private static final int NUM_EXECUTORS = 4;

    /**
     * Changes the number of executors on the Jenkins master.
     * Runs before every test.
     * @param rule the jenkins rule
     * @throws Exception if something goes wrong
     */
    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        // can be removed with plugin-pom 5.x - see https://github.com/jenkinsci/jenkins-test-harness/pull/910
        ACL.as2(ACL.SYSTEM2);
        GlobalMatrixAuthorizationStrategy authStrategy = new GlobalMatrixAuthorizationStrategy();
        authStrategy.add(Jenkins.ADMINISTER, new PermissionEntry(AuthorizationType.EITHER, "alice"));
        j.jenkins.setAuthorizationStrategy(authStrategy);
        j.jenkins.setSecurityRealm(this.j.createDummySecurityRealm());
        j.jenkins.setNumExecutors(NUM_EXECUTORS);
        //TODO https://github.com/jenkinsci/jenkins/pull/1596 renders this workaround unnecessary
        j.jenkins.setNodes(j.jenkins.getNodes());
    }

    /**
     * Tests that the URL for activating shutdown mode works as expected.
     */
    @Test
    void testActivateShutdown() {
        toggleLenientShutdown();
        assertThat(ShutdownManageLink.getInstance().isGoingToShutdown(), is(true));
    }

    /**
     * Tests that the URL for deactivating shutdown mode works as expected.
     */
    @Test
    void testDeactivateShutdown() {
        toggleLenientShutdown();
        toggleLenientShutdown();
        assertThat(ShutdownManageLink.getInstance().isGoingToShutdown(), is(false));
    }

    /**
     * Tests that all builds are started as normal when the shutdown mode has
     * not been initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    void testBuildsDirectlyWhenShutdownDisabled() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();

        //Builds and waits until completion (timeout 1 min):
        FreeStyleBuild build = project.scheduleBuild2(0).get(1, TimeUnit.MINUTES);

        assertThat(build.getResult(), is(equalTo(Result.SUCCESS)));
    }

    /**
     * Tests that builds without white listed upstreams are blocked
     * after shutdown mode is initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    void testBlocksBuildWhenShutdownEnabled() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();

        toggleLenientShutdown();
        project.scheduleBuild2(0);
        Item queueItem = waitForBlockedItem(project, TIMEOUT_SECONDS);

        assertThat(queueItem.isBlocked(), is(true));
        assertThat(Messages.IsAboutToShutDown(), is(queueItem.getWhy()));
    }

    /**
     * Tests that blocked builds are allowed to run after shutdown mode is deactivated again.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoesNotBlockAfterShutdownDisabledAgain() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();

        toggleLenientShutdown();
        QueueTaskFuture buildFuture = project.scheduleBuild2(0);
        Item queueItem = waitForBlockedItem(project, TIMEOUT_SECONDS);
        assertTrue(queueItem.isBlocked(), "Project was not blocked within time limit");

        //Disables shutdown mode
        toggleLenientShutdown();

        FreeStyleBuild build = (FreeStyleBuild)buildFuture.get(1, TimeUnit.MINUTES); // Wait for
                                                                                     // build finish

        assertThat(build.getResult(), is(equalTo(Result.SUCCESS)));
    }


    /**
     * Tests that builds with white listed upstreams are allowed,
     * even though lenient shutdown mode is active.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoesNotBlockDownstream() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");
        FreeStyleProject grandChild = j.createFreeStyleProject("grandchild");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        Jenkins.get().rebuildDependencyGraph();

        //Gives lenient shutdown mode time to activate while parent is still building:
        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));

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
    void testDoesNotBlockParameterizedPluginDownstream() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");
        FreeStyleProject grandChild = j.createFreeStyleProject("grandchild");

        BuildTriggerConfig childTrigger = new BuildTriggerConfig(child.getName(),
                ResultCondition.ALWAYS, new NodeParameters());
        BuildTriggerConfig grandChildTrigger = new BuildTriggerConfig(grandChild.getName(),
                ResultCondition.ALWAYS, new NodeParameters());

        parent.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(childTrigger));
        child.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(grandChildTrigger));
        Jenkins.get().rebuildDependencyGraph();

        //Gives lenient shutdown mode time to activate while parent is still building:
        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));

        //Trigger build of the first project, which starts the chain:
        parent.scheduleBuild2(0).waitForStart();

        toggleLenientShutdown();

        assertSuccessfulBuilds(parent, child, grandChild);
    }

    /**
     * Tests that downstream triggers that have been added as build steps in
     * Parameterized Trigger Plugin are allowed to finish when lenient shutdown
     * is activated during build.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    void testParameterizedPluginBuildStep() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");
        FreeStyleProject grandChild = j.createFreeStyleProject("grandchild");

        //Gives lenient shutdown mode time to activate while parent is still building:
        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));

        BlockingBehaviour waitForDownstreamBehavior = new BlockingBehaviour(
                Result.FAILURE, Result.FAILURE, Result.UNSTABLE);

        BlockableBuildTriggerConfig childTrigger = new BlockableBuildTriggerConfig(child.getName(),
                waitForDownstreamBehavior, null);
        BlockableBuildTriggerConfig grandChildTrigger = new BlockableBuildTriggerConfig(grandChild.getName(),
                waitForDownstreamBehavior, null);

        parent.getBuildersList().add(new TriggerBuilder(childTrigger));
        child.getBuildersList().add(new TriggerBuilder(grandChildTrigger));

        Jenkins.get().rebuildDependencyGraph();

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
    void testLinkIsVisible() throws Exception {
        WebClient w = j.createWebClient();
        w.login("alice");
        HtmlPage managePage = w.goTo("manage");
        assertTrue(managePage.asNormalizedText().contains(ShutdownManageLink.getInstance().getDisplayName()));
    }

    /**
     * Tests that projects that are in queue when lenient shutdown is enabled
     * are allowed to build if they have a completed upstream project.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoesNotBlockQueuedWithCompletedUpstream() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");
        FreeStyleProject grandChild = j.createFreeStyleProject("grandchild");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);
        Jenkins.get().rebuildDependencyGraph();

        //Trigger build of the parent project, and wait for it to finish:
        parent.scheduleBuild2(0).get();

        //Wait for the child project to queue up (in quiet period)
        waitForProjectInQueue(child);

        toggleLenientShutdown();

        assertSuccessfulBuilds(parent, child, grandChild);
    }

    /**
     * Waits for the given project to appear in the queue.
     *
     * @param project the project to wait for
     * @throws InterruptedException if interupted
     */
    private void waitForProjectInQueue(final FreeStyleProject project) throws InterruptedException {
        final Queue queue = Queue.getInstance();
        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            final Item[] items = queue.getItems();
            if (items.length > 0) {
                AbstractProject firstQueued = (AbstractProject)items[0].task;
                if (firstQueued.equals(project)) {
                    break;
                }
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        if (elapsedSeconds >= TIMEOUT_SECONDS) {
            fail("Child project was not queued up within time limit");
        }
    }

    /**
     * Tests that projects that are in queue when lenient shutdown is enabled
     * are blocked if they don't have an upstream project and allow all queued items was not set.
     * @throws Exception if something goes wrong
     */
    @Test
    void testBlocksQueuedWithoutUpstream() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.scheduleBuild2(QUIET_PERIOD);

        //Wait for the project to queue up (in quiet period)
        waitForItemInQueue();

        toggleLenientShutdown();

        Item queueItem = waitForBlockedItem(project, TIMEOUT_SECONDS);

        assertTrue(queueItem.isBlocked());
        assertEquals(Messages.IsAboutToShutDown(), queueItem.getWhy());
    }

    /**
     * Waits that the given project shows up in the queue and that it is blocked.
     *
     * @param project The project to wait for
     * @param timeout seconds to wait before aborting
     * @return the found item, null if the item didn't show up in the queue until timeout
     * @throws InterruptedException if interrupted
     */
    private Item waitForBlockedItem(FreeStyleProject project, int timeout) throws InterruptedException {
        Queue jenkinsQueue = Jenkins.get().getQueue();
        Item queueItem = jenkinsQueue.getItem(project);

        int elapsedSeconds = 0;
        while (elapsedSeconds <= timeout) {
            queueItem = jenkinsQueue.getItem(project);
            if (queueItem != null && queueItem.isBlocked()) {
                return queueItem;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        return queueItem;
    }

    /**
     * Waits until an item shows up in the queue.
     *
     * @throws InterruptedException if interrupted
     */
    private void waitForItemInQueue() throws InterruptedException {
        Queue queue = Queue.getInstance();
        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            if (queue.getItems().length > 0) {
                return;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        fail("Project was not queued up within time limit");
    }

    /**
     * Tests that projects that are in queue when lenient shutdown is enabled
     * are not blocked if they don't have an upstream project and allow all queued items was set.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoesntBlockQueuedWithoutUpstreamWhenAllowAllQueuedEnabled() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.scheduleBuild2(QUIET_PERIOD);

        //Wait for the project to queue up (in quiet period)
        waitForItemInQueue();

        ShutdownManageLink.getInstance().getConfiguration().setAllowAllQueuedItems(true);
        toggleLenientShutdown();

        assertSuccessfulBuilds(project);
    }

    /**
     * Wait that after enabling lenient shutdown, the analysis which projects are allowed is
     * finished.
     * The asynchronous call of the analysis leads to race conditions in the tests.
     *
     * @throws InterruptedException if interrupted
     */
    private void waitForAnalysisToFinish() throws InterruptedException {
        ShutdownManageLink shutdownManage = ShutdownManageLink.getInstance();
        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            if (!shutdownManage.isAnalyzing()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        if (elapsedSeconds >= TIMEOUT_SECONDS) {
            fail("Shutdown Analysis didn't finish in time.");
        }
    }

    /**
     * Tests that a white listed projects is blocked when the queue was empty
     * when lenient shutdown was activated.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    void testWhiteListedProjectsAreBlockedWhenQueueEmpty() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("whitelisted");

        ShutdownConfiguration configuration = ShutdownConfiguration.getInstance();
        configuration.setAllowWhiteListedProjects(true);
        configuration.getWhiteListedProjects().add("whitelisted");
        toggleLenientShutdown();

        waitForAnalysisToFinish();

        project.scheduleBuild2(QUIET_PERIOD);

        Item queueItem = waitForBlockedItem(project, TIMEOUT_SECONDS);

        assertTrue(queueItem.isBlocked());
    }

    /**
     * Tests that a white listed project is not blocked when the queue was not empty
     * when lenient shutdown was activated and builds are still running.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    void testWhiteListedProjectsAreNotBlockedWhenQueueNotEmpty() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);
        Jenkins.get().rebuildDependencyGraph();

        FreeStyleProject whiteListedProject = j.createFreeStyleProject("whitelisted");

        ShutdownConfiguration configuration = ShutdownConfiguration.getInstance();
        configuration.setAllowWhiteListedProjects(true);
        configuration.getWhiteListedProjects().add("whitelisted");

        parent.scheduleBuild2(0);
        waitForProjectInQueue(child);

        toggleLenientShutdown();
        waitForAnalysisToFinish();

        whiteListedProject.scheduleBuild2(0);

        assertSuccessfulBuilds(parent, child, whiteListedProject);

    }

    /**
     * Tests that a non white listed project is blocked when the queue was not empty when
     * lenient shutdown was activated, white listed projects are allowed and builds are still
     * running.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    void testNonWhiteListedProjectsAreBlockedWhenQueueNotEmpty() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);

        FreeStyleProject nonWhiteListedProject = j.createFreeStyleProject("nonwhitelisted");

        Jenkins.get().rebuildDependencyGraph();

        ShutdownConfiguration configuration = ShutdownConfiguration.getInstance();
        configuration.setAllowWhiteListedProjects(true);
        configuration.getWhiteListedProjects().add("whitelisted");

        // Trigger build of the parent project, and wait for it to finish:
        parent.scheduleBuild2(0);
        waitForProjectInQueue(child);

        toggleLenientShutdown();
        waitForAnalysisToFinish();

        nonWhiteListedProject.scheduleBuild2(QUIET_PERIOD);
        Item queueItem = waitForBlockedItem(nonWhiteListedProject, TIMEOUT_SECONDS);

        assertSuccessfulBuilds(parent, child);
        assertThat(queueItem.isBlocked(), is(true));
    }

    /**
     * Tests that a white listed project is blocked when the queue was not empty
     * when lenient shutdown was activated and builds are finished.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    void testWhiteListedProjectsAreBlockedWhenQueueNotEmptyAllBuildsFinished() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);

        FreeStyleProject whiteListedProject = j.createFreeStyleProject("whitelisted");

        Jenkins.get().rebuildDependencyGraph();

        ShutdownConfiguration configuration = ShutdownConfiguration.getInstance();
        configuration.setAllowWhiteListedProjects(true);
        configuration.getWhiteListedProjects().add("whitelisted");

        parent.scheduleBuild2(0);
        waitForProjectInQueue(child);

        toggleLenientShutdown();

        assertSuccessfulBuilds(parent, child);

        whiteListedProject.scheduleBuild2(0);

        Item queueItem = waitForBlockedItem(whiteListedProject, TIMEOUT_SECONDS);

        assertThat(queueItem.isBlocked(), is(true));
    }

    /**
     * Tests that a downstream build of a white listed project is not blocked when the queue was not
     * empty when lenient shutdown was activated and builds are still running.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    void testDownstreamOfWhiteListedProjectsAreNotBlockedWhenQueueNotEmpty() throws Exception {
        FreeStyleProject parent = j.createFreeStyleProject("parent");
        FreeStyleProject child = j.createFreeStyleProject("child");

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.setQuietPeriod(QUIET_PERIOD);

        FreeStyleProject whiteListed = j.createFreeStyleProject("whitelisted");
        FreeStyleProject whiteListedChild = j.createFreeStyleProject("whitelistedchild");
        FreeStyleProject whiteListedGrandChild = j.createFreeStyleProject("whitelistedgrandchild");
        whiteListed.getPublishersList().add(new BuildTrigger(whiteListedChild.getName(), Result.SUCCESS));
        whiteListedChild.getPublishersList().add(new BuildTrigger(whiteListedGrandChild.getName(), Result.SUCCESS));

        Jenkins.get().rebuildDependencyGraph();

        ShutdownConfiguration configuration = ShutdownConfiguration.getInstance();
        configuration.setAllowWhiteListedProjects(true);
        configuration.getWhiteListedProjects().add("whitelisted");

        parent.scheduleBuild2(0);
        waitForProjectInQueue(child);

        toggleLenientShutdown();
        waitForAnalysisToFinish();

        whiteListed.scheduleBuild2(0);

        assertSuccessfulBuilds(parent, child, whiteListed, whiteListedChild, whiteListedGrandChild);
    }

    /**
     * Toggles the lenient shutdown mode.
     */
    private void toggleLenientShutdown() {
        ShutdownManageLink.getInstance().performToggleGoingToShutdown();
    }

    /**
     * Tests that all builds are started as normal when the shutdown mode has
     * not been initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    void testIsApplicableWhenFalse() throws Exception {
        ShutdownConfiguration configuration = ShutdownConfiguration.getInstance();

        configuration.setAllowAllJobs(false);
        AbstractProject project = j.createFreeStyleProject();
        assertTrue(QueueUtils.isApplicable(project));

        WorkflowJobMock job = mock(WorkflowJobMock.class);
        assertFalse(QueueUtils.isApplicable(job));
    }

    /**
     * Tests that all builds are started as normal when the shutdown mode has
     * not been initiated.
     * @throws Exception if something goes wrong
     */
    @Test
    void testIsApplicableWhenTrue() throws Exception {
        ShutdownConfiguration configuration = ShutdownConfiguration.getInstance();

        configuration.setAllowAllJobs(true);
        AbstractProject project = j.createFreeStyleProject();
        assertTrue(QueueUtils.isApplicable(project));

        WorkflowJobMock job = mock(WorkflowJobMock.class);
        assertTrue(QueueUtils.isApplicable(job));
    }
}
