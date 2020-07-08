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

import static com.sonymobile.jenkins.plugins.lenientshutdown.LenientShutdownAssert.assertNodeGoesOffline;
import static com.sonymobile.jenkins.plugins.lenientshutdown.LenientShutdownAssert.assertSuccessfulBuilds;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.SleepBuilder;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.BlockingBehaviour;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.slaves.DumbSlave;
import hudson.tasks.BuildTrigger;
import jenkins.model.Jenkins;

/**
 * Test class for taking individual nodes offline leniently.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class NodeLenientOfflineTest {

    private static final int JOB_SLEEP_TIME = 5000;

    /**
     * Jenkins rule instance.
     */
    // CS IGNORE VisibilityModifier FOR NEXT 3 LINES. REASON: Mocks tests.
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private static final int TIMEOUT_SECONDS = 60;
    private static final int QUIET_PERIOD = 15;

    private PluginImpl plugin;

    private DumbSlave node0;
    private DumbSlave node1;

    /**
     * Prepares for test by creating nodes and disabling builds on master.
     * @throws Exception if something goes wrong
     */
    @Before
    public void setUp() throws Exception {
        Jenkins jenkins = jenkinsRule.getInstance();
        GlobalMatrixAuthorizationStrategy authStategy = new GlobalMatrixAuthorizationStrategy();
        authStategy.add(Jenkins.ADMINISTER, "alice");
        jenkins.setAuthorizationStrategy(authStategy);
        jenkins.setSecurityRealm(jenkinsRule.createDummySecurityRealm());

        plugin = PluginImpl.getInstance();
        node0 = jenkinsRule.createOnlineSlave();
        node1 = jenkinsRule.createOnlineSlave();


        jenkinsRule.jenkins.setMode(Node.Mode.EXCLUSIVE); //Don't build on master
    }

    /**
     * Tests that the URL for activating shutdown mode
     * for a specific node works as expected.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testActivateShutdownNoBuilds() throws Exception {
        toggleLenientNodeOffline(node0);
        assertTrue(node0.toComputer().isTemporarilyOffline());
        assertFalse(node1.toComputer().isTemporarilyOffline());
    }


    /**
     * Tests that the current build completes when lenient offline is activated during build.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testActivateShutdownDuringBuild() throws Exception {
        FreeStyleBuild build = activateShutdownDuringBuild();

        assertTrue(node0.toComputer().isTemporarilyOffline());
        assertEquals(Result.SUCCESS, build.getResult());
    }

    /**
     * Tests that the build is not completed when all nodes are offline before
     * the build is started.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testAllNodesOfflineNothingBuilds() throws Exception {
        Queue jenkinsQueue = Jenkins.getInstance().getQueue();
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        toggleLenientNodeOffline(node0);
        toggleLenientNodeOffline(node1);

        project.scheduleBuild2(0);
        Queue.Item queueItem = jenkinsQueue.getItem(project);

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            queueItem = jenkinsQueue.getItem(project);
            if (queueItem.getCauseOfBlockage() != null) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }

        assertNotNull(queueItem.getCauseOfBlockage());
    }

    /**
     * Tests that a chain of downstream builds continues to build on another node
     * after the first has been put to lenient offline, allowing the first to go offline
     * more quickly.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testChainBuildsOnOtherNode() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        Jenkins.getInstance().rebuildDependencyGraph();

        parent.scheduleBuild2(0).waitForStart();
        TimeUnit.SECONDS.sleep(1); //Waiting for the build to get assigned to a node (not the master).

        Node buildingOn = parent.getLastBuiltOn();
        if (buildingOn == null || buildingOn == Jenkins.getInstance()) {
            fail("Project was not built correctly");
        }

        toggleLenientNodeOffline(buildingOn);

        Node expectedNextBuildingOn = null;
        if (buildingOn.equals(node0)) {
            expectedNextBuildingOn = node1;
        } else if (buildingOn.equals(node1)) {
            expectedNextBuildingOn = node0;
        }

        assertSuccessfulBuilds(parent, child, grandChild);

        assertEquals(expectedNextBuildingOn.getNodeName(), child.getBuildByNumber(1).getBuiltOnStr());
        assertEquals(expectedNextBuildingOn.getNodeName(), grandChild.getBuildByNumber(1).getBuiltOnStr());
    }

    /**
     * Tests that a chain of builds is allowed to complete when the only available
     * build node is the one that's shutting down.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testMustBuildOnSameNode() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        node0.getComputer().setTemporarilyOffline(true);
        //Everything will now build on node1 since node0 is offline.

        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        Jenkins.getInstance().rebuildDependencyGraph();

        parent.scheduleBuild2(0).waitForStart();
        toggleLenientNodeOffline(node1);

        assertSuccessfulBuilds(parent, child, grandChild);
        assertNodeGoesOffline(node1);
    }

    /**
     * Tests that lenient shutdown mode can be activated and deactivated during a build.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testActivateDeactivateShutdown() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");

        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        Jenkins.getInstance().rebuildDependencyGraph();
        parent.setAssignedNode(node0);
        child.setAssignedNode(node0);

        parent.scheduleBuild2(0).waitForStart();

        toggleLenientNodeOffline(node0);
        if (!plugin.isNodeShuttingDown(node0.getNodeName())) {
            fail("Node should be shutting down!");
        }
        //Now reactivate the node
        toggleLenientNodeOffline(node0);

        assertSuccessfulBuilds(parent, child);
    }

    /**
     * Makes sure that the lenient shutdown mode is reset after a node is taken back online.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testShutdownNodeResetter() throws Exception {
        activateShutdownDuringBuild();

        //Deactivates temporarily offline mode:
        node0.toComputer().setTemporarilyOffline(false);

        assertFalse(plugin.isNodeShuttingDown(node0.getNodeName()));
    }

    /**
     * Tests that build steps set up with Parameterized Trigger Plugin are
     * allowed to finish when a node is taken temp. offline leniently.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testParameterizedBuildTrigger() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");

        // Gives lenient shutdown mode time to activate while parent is still building:
        parent.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));

        BlockingBehaviour waitForDownstreamBehavior = new BlockingBehaviour(
                Result.FAILURE, Result.FAILURE, Result.UNSTABLE);

        BlockableBuildTriggerConfig childTrigger = new BlockableBuildTriggerConfig(child.getName(),
                waitForDownstreamBehavior, null);

        parent.getBuildersList().add(new TriggerBuilder(childTrigger));
        Jenkins.getInstance().rebuildDependencyGraph();

        parent.scheduleBuild2(0).waitForStart();
        TimeUnit.SECONDS.sleep(1); //Waiting for the build to get assigned to a node (not the master).

        Node buildingOn = parent.getLastBuiltOn();
        if (buildingOn == null || buildingOn == Jenkins.getInstance()) {
            fail("Project was not built correctly");
        }

        toggleLenientNodeOffline(buildingOn);

        Node expectedNextBuildingOn = null;
        if (buildingOn.equals(node0)) {
            expectedNextBuildingOn = node1;
        } else if (buildingOn.equals(node1)) {
            expectedNextBuildingOn = node0;
        }

        assertSuccessfulBuilds(parent, child);
        assertEquals(expectedNextBuildingOn.getNodeName(), child.getBuildByNumber(1).getBuiltOnStr());
        assertNodeGoesOffline((DumbSlave)buildingOn);
    }

    /**
     * Toggles the lenient offline mode for a specific node.
     * @param node the node to toggle lenient offline mode for
     * @throws Exception if something goes wrong
     */
    private void toggleLenientNodeOffline(Node node) throws Exception {
        String url = node.toComputer().getUrl() + ShutdownNodeAction.URL;
        WebClient client = jenkinsRule.createWebClient();
        client.login("alice");
        client.goTo(url);
    }

    /**
     * Triggers a build on a specific node and puts it in lenient offline mode
     * while it's still building.
     * @return the build
     * @throws Exception if something goes wrong
     */
    private FreeStyleBuild activateShutdownDuringBuild() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getBuildersList().add(new SleepBuilder(JOB_SLEEP_TIME));
        project.setAssignedNode(node0);

        QueueTaskFuture<FreeStyleBuild> buildFuture = project.scheduleBuild2(0);
        buildFuture.waitForStart();

        toggleLenientNodeOffline(node0);
        if (!plugin.isNodeShuttingDown(node0.getNodeName())) {
            fail("Node should be shutting down");
        }

        FreeStyleBuild build = buildFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS); //Wait for completion

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            if (node0.toComputer().isTemporarilyOffline()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        return build;
    }

}
