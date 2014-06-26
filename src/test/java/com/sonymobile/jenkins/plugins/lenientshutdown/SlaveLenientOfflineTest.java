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
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import hudson.tasks.BuildTrigger;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Test class for taking individual slaves offline leniently.
 *
 * @author Fredrik Persson &lt;fredrik6.persson@sonymobile.com&gt;
 */
public class SlaveLenientOfflineTest {

    /**
     * Jenkins rule instance.
     */
    // CS IGNORE VisibilityModifier FOR NEXT 3 LINES. REASON: Mocks tests.
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private static final int TIMEOUT_SECONDS = 60;
    private static final int QUIET_PERIOD = 15;

    private PluginImpl plugin;

    private DumbSlave slave0;
    private DumbSlave slave1;

    /**
     * Prepares for test by creating slaves and disabling builds on master.
     * @throws Exception if something goes wrong
     */
    @Before
    public void setUp() throws Exception {
        plugin = PluginImpl.getInstance();
        slave0 = jenkinsRule.createOnlineSlave();
        slave1 = jenkinsRule.createOnlineSlave();

        jenkinsRule.jenkins.setMode(Node.Mode.EXCLUSIVE); //Don't build on master
    }

    /**
     * Tests that the URL for activating shutdown mode
     * for a specific slave works as expected.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testActivateShutdownNoBuilds() throws Exception {
        toggleLenientSlaveOffline(slave0);
        assertTrue(slave0.toComputer().isTemporarilyOffline());
        assertFalse(slave1.toComputer().isTemporarilyOffline());
    }


    /**
     * Tests that the current build completes when lenient offline is activated during build.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testActivateShutdownDuringBuild() throws Exception {
        FreeStyleBuild build = activateShutdownDuringBuild();

        assertTrue(slave0.toComputer().isTemporarilyOffline());
        assertEquals(Result.SUCCESS, build.getResult());
    }

    /**
     * Tests that the build is not completed when all slaves are offline before
     * the build is started.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testAllSlavesOfflineNothingBuilds() throws Exception {
        Queue jenkinsQueue = Jenkins.getInstance().getQueue();
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();

        toggleLenientSlaveOffline(slave0);
        toggleLenientSlaveOffline(slave1);

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
     * Tests that a chain of downstream builds continues to build on another slave
     * after the first has been put to lenient offline, allowing the first to go offline
     * more quickly.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testChainBuildsOnOtherNode() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");
        FreeStyleProject grandChild = jenkinsRule.createFreeStyleProject("grandchild");

        parent.getBuildersList().add(new Shell("sleep 10"));

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        Jenkins.getInstance().rebuildDependencyGraph();

        parent.scheduleBuild2(0).waitForStart();
        TimeUnit.SECONDS.sleep(1); //Waiting for the build to get assigned to a slave (not the master).

        Node buildingOn = parent.getLastBuiltOn();
        if (buildingOn == null || buildingOn == Jenkins.getInstance()) {
            fail("Project was not built correctly");
        }

        toggleLenientSlaveOffline(buildingOn);

        Node expectedNextBuildingOn = null;
        if (buildingOn.equals(slave0)) {
            expectedNextBuildingOn = slave1;
        } else if (buildingOn.equals(slave1)) {
            expectedNextBuildingOn = slave0;
        }

        AbstractBuild parentBuild = null;
        AbstractBuild childBuild = null;
        AbstractBuild grandchildBuild = null;

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            parentBuild = parent.getBuildByNumber(1);
            childBuild = child.getBuildByNumber(1);
            grandchildBuild = grandChild.getBuildByNumber(1);

            if (parentBuild != null && parentBuild.getResult() == Result.SUCCESS
                    && childBuild != null && childBuild.getResult() == Result.SUCCESS
                    && grandchildBuild != null && grandchildBuild.getResult() == Result.SUCCESS) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }

        assertNotNull(parentBuild);
        assertNotNull(childBuild);
        assertNotNull(grandchildBuild);

        assertEquals(Result.SUCCESS, parentBuild.getResult());
        assertEquals(Result.SUCCESS, childBuild.getResult());
        assertEquals(Result.SUCCESS, grandchildBuild.getResult());

        assertEquals(expectedNextBuildingOn.getNodeName(), childBuild.getBuiltOnStr());
        assertEquals(expectedNextBuildingOn.getNodeName(), grandchildBuild.getBuiltOnStr());
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

        slave0.getComputer().setTemporarilyOffline(true);
        //Everything will now build on slave1 since slave0 is offline.

        parent.getBuildersList().add(new Shell("sleep 10"));

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        child.getPublishersList().add(new BuildTrigger(grandChild.getName(), Result.SUCCESS));
        Jenkins.getInstance().rebuildDependencyGraph();

        parent.scheduleBuild2(0).waitForStart();
        toggleLenientSlaveOffline(slave1);


        AbstractBuild parentBuild = null;
        AbstractBuild childBuild = null;
        AbstractBuild grandchildBuild = null;

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            parentBuild = parent.getBuildByNumber(1);
            childBuild = child.getBuildByNumber(1);
            grandchildBuild = grandChild.getBuildByNumber(1);

            if (parentBuild != null && parentBuild.getResult() == Result.SUCCESS
                    && childBuild != null && childBuild.getResult() == Result.SUCCESS
                    && grandchildBuild != null && grandchildBuild.getResult() == Result.SUCCESS
                    && slave1.getComputer().isTemporarilyOffline()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }

        assertNotNull(parentBuild);
        assertNotNull(childBuild);
        assertNotNull(grandchildBuild);

        assertEquals(Result.SUCCESS, parentBuild.getResult());
        assertEquals(Result.SUCCESS, childBuild.getResult());
        assertEquals(Result.SUCCESS, grandchildBuild.getResult());

        assertTrue("Slave1 should shut down after builds are complete",
                slave1.toComputer().isTemporarilyOffline());
    }

    /**
     * Tests that lenient shutdown mode can be activated and deactivated during a build.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testActivateDeactivateShutdown() throws Exception {
        FreeStyleProject parent = jenkinsRule.createFreeStyleProject("parent");
        FreeStyleProject child = jenkinsRule.createFreeStyleProject("child");

        parent.getBuildersList().add(new Shell("sleep 10"));

        parent.getPublishersList().add(new BuildTrigger(child.getName(), Result.SUCCESS));
        Jenkins.getInstance().rebuildDependencyGraph();
        parent.setAssignedNode(slave0);
        child.setAssignedNode(slave0);

        parent.scheduleBuild2(0).waitForStart();

        toggleLenientSlaveOffline(slave0);
        if (!plugin.isNodeShuttingDown(slave0.getNodeName())) {
            fail("Node should be shutting down!");
        }
        //Now reactivate the slave
        toggleLenientSlaveOffline(slave0);

        AbstractBuild parentBuild = null;
        AbstractBuild childBuild = null;

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            parentBuild = parent.getBuildByNumber(1);
            childBuild = child.getBuildByNumber(1);

            if (parentBuild != null && parentBuild.getResult() == Result.SUCCESS
                    && childBuild != null && childBuild.getResult() == Result.SUCCESS) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }

        assertNotNull(parentBuild);
        assertNotNull(childBuild);

        assertEquals(Result.SUCCESS, parentBuild.getResult());
        assertEquals(Result.SUCCESS, childBuild.getResult());
    }

    /**
     * Makes sure that the lenient shutdown mode is reset after a slave is taken back online.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testShutdownSlaveResetter() throws Exception {
        activateShutdownDuringBuild();

        //Deactivates temporarily offline mode:
        slave0.toComputer().setTemporarilyOffline(false);

        assertFalse(plugin.isNodeShuttingDown(slave0.getNodeName()));
    }


    /**
     * Toggles the lenient offline mode for a specific slave.
     * @param node the node to toggle lenient offline mode for
     * @throws Exception if something goes wrong
     */
    private void toggleLenientSlaveOffline(Node node) throws Exception {
        String url = node.toComputer().getUrl() + ShutdownSlaveAction.URL;
        jenkinsRule.createWebClient().goTo(url);
    }

    /**
     * Triggers a build on a specific slave and puts it in lenient offline mode
     * while it's still building.
     * @return the build
     * @throws Exception if something goes wrong
     */
    private FreeStyleBuild activateShutdownDuringBuild() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getBuildersList().add(new Shell("sleep 10"));
        project.setAssignedNode(slave0);

        QueueTaskFuture<FreeStyleBuild> buildFuture = project.scheduleBuild2(0);
        buildFuture.waitForStart();

        toggleLenientSlaveOffline(slave0);
        if (!plugin.isNodeShuttingDown(slave0.getNodeName())) {
            fail("Node should be shutting down");
        }

        FreeStyleBuild build = buildFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS); //Wait for completion

        int elapsedSeconds = 0;
        while (elapsedSeconds <= TIMEOUT_SECONDS) {
            if (slave0.toComputer().isTemporarilyOffline()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            elapsedSeconds++;
        }
        return build;
    }

}
