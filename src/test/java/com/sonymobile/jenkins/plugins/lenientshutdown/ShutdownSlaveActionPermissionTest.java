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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.security.ACL;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.slaves.DumbSlave;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;


/**
 * Security and Permissions tests for {@link ShutdownSlaveAction}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
@WithJenkins
class ShutdownSlaveActionPermissionTest {

    public static final int SLEEP_TIME = 100000;
    private JenkinsRule j;
    private DumbSlave slave;

    /**
     * The XPath to the left side icon
     */
    private static final String IMG_XPATH =
            "//a[contains(@class, 'task-link') and contains(@href, 'lenientshutdown')]"
            + "/span[@class='task-link-text' and text()[contains(., 'Take')]]";

    /**
     * The XPath to the left side cancel icon
     */
    private static final String IMG_XPATH_CANCEL =
            "//a[contains(@class, 'task-link') and contains(@href, 'lenientshutdown')]"
            + "/span[@class='task-link-text' and text()[contains(., 'Cancel')]]";

    /**
     * The XPath to the left side text link
     */
    private static final String LINK_TEXT_XPATH =
            "//a[contains(@class, 'task-link') and contains(@href, 'lenientshutdown')]";

    /**
     * Create a slave to test on.
     * @param rule the jenkins rule
     * @throws Exception if something goes wrong
     */
    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        // can be removed with plugin-pom 5.x - see https://github.com/jenkinsci/jenkins-test-harness/pull/910
        ACL.as2(ACL.SYSTEM2);
        slave = j.createOnlineSlave();
    }

    /**
     * Tests that the icon is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetIconFileName() throws Exception {
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH);
        assertNotNull(el, "No img found");
    }

    /**
     * Tests that the link is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetDisplayName() throws Exception {
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNotNull(el, "No link found");
    }

    /**
     * Tests that the icon is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetIconFileNameCancel() throws Exception {
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assertNotNull(el, "No img found");
    }

    /**
     * Tests that the link is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetDisplayNameCancel() throws Exception {
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNotNull(el, "No link found");
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoIndex() throws Exception {
        startBuild();
        j.createWebClient().getPage(slave, ShutdownSlaveAction.URL);
        assertTrue(PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()), "Unaffected");
    }

    //Tests below are with security set

    /**
     * Tests that the icon is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetIconFileNameNoPermission() throws Exception {
        setupSecurity();
        //As anonymous
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH);
        assertNull(el, "img found");

        //As alice
        page = j.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH);
        assertNotNull(el, "No img found");

        //As bobby
        page = j.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH);
        assertNull(el, "img found");
    }


    /**
     * Tests that the link is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetDisplayNameNoPermission() throws Exception {
        setupSecurity();
        //As anonymous
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNull(el, "link found");

        //As alice
        page = j.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNotNull(el, "No link found");

        //As bobby
        page = j.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNull(el, "link found");
    }

    /**
     * Tests that the icon is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetIconFileNameCancelNoPermission() throws Exception {
        setupSecurity();
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        //As anonymous
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assertNull(el, "img found");

        //As alice
        page = j.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assertNotNull(el, "No img found");

        //As bobby
        page = j.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assertNull(el, "img found");
    }

    /**
     * Tests that the link is visible when user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testGetDisplayNameCancelNoPermission() throws Exception {
        setupSecurity();
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        //As anonymous
        HtmlPage page = j.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNull(el, "link found");

        //As alice
        page = j.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNotNull(el, "No link found");

        //As bobby
        page = j.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assertNull(el, "link found");
    }

    /**
     * Tests that the link can't be clicked when the user has no permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoIndexNoPermissionAnonymous() throws Exception {
        setupSecurity();
        // should be assertThrows() but checkstyle makes life harder than it needs to be
        try {
            j.createWebClient().getPage(slave, ShutdownSlaveAction.URL);
            fail("Expected FailingHttpStatusCodeException");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
            assertNotNull(ex);
        }
    }

    /**
     * Tests that the link can't be clicked when the user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoIndexNoPermissionBobby() throws Exception {
        setupSecurity();
        // should be assertThrows() but checkstyle makes life harder than it needs to be
        try {
            j.createWebClient().login("bobby").getPage(slave, ShutdownSlaveAction.URL);
            fail("Expected FailingHttpStatusCodeException");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
            assertNotNull(ex);
        }
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoIndexPermissionAlice() throws Exception {
        setupSecurity();
        JenkinsRule.WebClient client = j.createWebClient().login("alice");
        startBuild();
        TimeUnit.SECONDS.sleep(1);

        client.getPage(slave, ShutdownSlaveAction.URL);

        assertTrue(PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()), "Unaffected");
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     * @throws Exception if something goes wrong
     */
    @Test
    void testDoIndexPermissionCancelAlice() throws Exception {
        setupSecurity();
        JenkinsRule.WebClient client = j.createWebClient().login("alice");
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        client.getPage(slave, ShutdownSlaveAction.URL);
        assertFalse(PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()), "Unaffected");
    }

    /**
     * Sets up Jenkins security
     */
    private void setupSecurity() {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
        strategy.add(Jenkins.READ, new PermissionEntry(AuthorizationType.EITHER, "anonymous"));
        strategy.add(Computer.CONNECT, new PermissionEntry(AuthorizationType.EITHER, "alice"));
        strategy.add(Computer.DISCONNECT, new PermissionEntry(AuthorizationType.EITHER, "alice"));
        strategy.add(Jenkins.READ, new PermissionEntry(AuthorizationType.EITHER, "bobby"));
        strategy.add(Jenkins.READ, new PermissionEntry(AuthorizationType.EITHER, "alice"));
        j.jenkins.setAuthorizationStrategy(strategy);
    }

    /**
     * Stats the build
     * @throws Exception if something goes wrong
     */
    private void startBuild() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new SleepBuilder(SLEEP_TIME));
        project.setAssignedLabel(slave.getSelfLabel());
        project.scheduleBuild2(0);
    }
}
