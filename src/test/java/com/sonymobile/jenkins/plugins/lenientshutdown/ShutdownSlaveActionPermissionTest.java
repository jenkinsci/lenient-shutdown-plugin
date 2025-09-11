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

import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.slaves.DumbSlave;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;


/**
 * Security and Permissions tests for {@link ShutdownSlaveAction}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
public class ShutdownSlaveActionPermissionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private DumbSlave slave;

    /**
     * The XPath to the left side icon
     */
    private static final String IMG_XPATH = "//a[contains(@class, 'task-link') and contains(@href, 'lenientshutdown')]" +
        "/span[@class='task-link-text' and text()[contains(., 'Take')]]";

    /**
     * The XPath to the left side cancel icon
     */
    private static final String IMG_XPATH_CANCEL = "//a[contains(@class, 'task-link') and contains(@href, 'lenientshutdown')]" +
        "/span[@class='task-link-text' and text()[contains(., 'Cancel')]]";

    /**
     * The XPath to the left side text link
     */
    private static final String LINK_TEXT_XPATH = "//a[contains(@class, 'task-link') and contains(@href, 'lenientshutdown')]";

    /**
     * Create a slave to test on.
     */
    @Before
    public void before() throws Exception {
        slave = jenkins.createOnlineSlave();
    }

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    public void testGetIconFileName() throws Exception {
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH);
        assert el != null : "No img found";
    }

    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    public void testGetDisplayName() throws Exception {
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el != null : "No link found";
    }

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    public void testGetIconFileNameCancel() throws Exception {
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assert el != null : "No img found";
    }

    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    public void testGetDisplayNameCancel() throws Exception {
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el != null : "No link found";
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     */
    @Test
    public void testDoIndex() throws Exception {
        startBuild();
        jenkins.createWebClient().getPage(slave, ShutdownSlaveAction.URL);
        assert PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()) : "Unaffected";
    }

    //Tests below are with security set

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    public void testGetIconFileNameNoPermission() throws Exception {
        setupSecurity();
        //As anonymous
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH);
        assert el == null : "img found";

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH);
        assert el != null : "No img found";

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH);
        assert el == null : "img found";
    }


    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    public void testGetDisplayNameNoPermission() throws Exception {
        setupSecurity();
        //As anonymous
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el == null : "link found";

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el != null : "No link found";

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el == null : "link found";
    }

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    public void testGetIconFileNameCancelNoPermission() throws Exception {
        setupSecurity();
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        //As anonymous
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assert el == null : "img found";

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assert el != null : "No img found";

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(IMG_XPATH_CANCEL);
        assert el == null : "img found";
    }

    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    public void testGetDisplayNameCancelNoPermission() throws Exception {
        setupSecurity();
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        //As anonymous
        HtmlPage page = jenkins.createWebClient().getPage(slave);
        HtmlElement el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el == null : "link found";

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el != null : "No link found";

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(slave);
        el = page.getFirstByXPath(LINK_TEXT_XPATH);
        assert el == null : "link found";
    }

    /**
     * Tests that the link can't be clicked when the user has no permission.
     */
    @Test(expected = FailingHttpStatusCodeException.class)
    public void testDoIndexNoPermissionAnonymous() throws Exception {
        setupSecurity();
        jenkins.createWebClient().getPage(slave, ShutdownSlaveAction.URL);
    }

    /**
     * Tests that the link can't be clicked when the user has permission.
     */
    @Test(expected = FailingHttpStatusCodeException.class)
    public void testDoIndexNoPermissionBobby() throws Exception {
        setupSecurity();
        jenkins.createWebClient().login("bobby").getPage(slave, ShutdownSlaveAction.URL);
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     */
    @Test
    public void testDoIndexPermissionAlice() throws Exception {
        setupSecurity();
        JenkinsRule.WebClient client = jenkins.createWebClient().login("alice");
        startBuild();
		TimeUnit.SECONDS.sleep(1);
        
        client.getPage(slave, ShutdownSlaveAction.URL);
        
        assert PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()) : "Unaffected";
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     */
    @Test
    public void testDoIndexPermissionCancelAlice() throws Exception {
        setupSecurity();
        JenkinsRule.WebClient client = jenkins.createWebClient().login("alice");
        PluginImpl.getInstance().toggleNodeShuttingDown(slave.getNodeName());
        client.getPage(slave, ShutdownSlaveAction.URL);
        assert !PluginImpl.getInstance().isNodeShuttingDown(slave.getNodeName()) : "Unaffected";
    }


    /**
     * Sets up Jenkins security
     */
    private void setupSecurity() {
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
        strategy.add(Jenkins.READ, "anonymous");
        strategy.add(Computer.CONNECT, "alice");
        strategy.add(Computer.DISCONNECT, "alice");
        strategy.add(Jenkins.READ, "bobby");
        strategy.add(Jenkins.READ, "alice");
        jenkins.jenkins.setAuthorizationStrategy(strategy);
    }

    private void startBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new SleepBuilder(100000));
        project.setAssignedLabel(slave.getSelfLabel());
        project.scheduleBuild2(0);
    }
}
