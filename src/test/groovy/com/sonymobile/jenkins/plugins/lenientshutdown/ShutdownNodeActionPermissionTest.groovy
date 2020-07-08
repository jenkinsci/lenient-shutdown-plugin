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
package com.sonymobile.jenkins.plugins.lenientshutdown

import java.util.concurrent.TimeUnit;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import hudson.model.Computer
import hudson.security.GlobalMatrixAuthorizationStrategy
import hudson.slaves.DumbSlave
import jenkins.model.Jenkins
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.GroovyJenkinsRule
import org.jvnet.hudson.test.SleepBuilder


/**
 * Security and Permissions tests for {@link ShutdownNodeAction}.
 *
 * @author &lt;robert.sandell@sonymobile.com&gt;
 */
class ShutdownNodeActionPermissionTest {

    @Rule
    public GroovyJenkinsRule jenkins = new GroovyJenkinsRule()
    private DumbSlave node

    /**
     * The XPath to the left side icon
     */
    static final String IMG_XPATH = "//a[@class='task-icon-link' and contains(@href, 'lenientshutdown')]/img[contains(@src,'system-log-out-small.png')]"

    /**
     * The XPath to the left side cancel icon
     */
    static final String IMG_XPATH_CANCEL = "//a[@class='task-icon-link' and contains(@href, 'lenientshutdown')]/img[contains(@src,'edit-delete.png')]"

    /**
     * The XPath to the left side text link
     */
    static final String LINK_TEXT_XPATH = "//a[@class='task-link' and contains(@href, 'lenientshutdown')]"

    /**
     * Create a node to test on.
     */
    @Before
    void before() {
        node = jenkins.createOnlineSlave()
    }

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    void testGetIconFileName() {
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(IMG_XPATH)
        assert el != null : "No img found"
    }

    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    void testGetDisplayName() {
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el != null : "No link found"
    }

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    void testGetIconFileNameCancel() {
        PluginImpl.instance.toggleNodeShuttingDown(node.nodeName)
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(IMG_XPATH_CANCEL)
        assert el != null : "No img found"
    }

    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    void testGetDisplayNameCancel() {
        PluginImpl.instance.toggleNodeShuttingDown(node.nodeName)
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el != null : "No link found"
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     */
    @Test
    void testDoIndex() {
        startBuild()
        jenkins.createWebClient().getPage(node, ShutdownNodeAction.URL)
        assert PluginImpl.instance.isNodeShuttingDown(node.nodeName) : "Unaffected"
    }

    //Tests below are with security set

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    void testGetIconFileNameNoPermission() {
        setupSecurity()
        //As anonymous
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(IMG_XPATH)
        assert el == null : "img found"

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(node)
        el = page.getFirstByXPath(IMG_XPATH)
        assert el != null : "No img found"

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(node)
        el = page.getFirstByXPath(IMG_XPATH)
        assert el == null : "img found"
    }


    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    void testGetDisplayNameNoPermission() {
        setupSecurity()
        //As anonymous
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el == null : "link found"

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(node)
        el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el != null : "No link found"

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(node)
        el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el == null : "link found"
    }

    /**
     * Tests that the icon is visible when user has permission.
     */
    @Test
    void testGetIconFileNameCancelNoPermission() {
        setupSecurity()
        PluginImpl.instance.toggleNodeShuttingDown(node.nodeName)
        //As anonymous
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(IMG_XPATH_CANCEL)
        assert el == null : "img found"

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(node)
        el = page.getFirstByXPath(IMG_XPATH_CANCEL)
        assert el != null : "No img found"

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(node)
        el = page.getFirstByXPath(IMG_XPATH_CANCEL)
        assert el == null : "img found"
    }

    /**
     * Tests that the link is visible when user has permission.
     */
    @Test
    void testGetDisplayNameCancelNoPermission() {
        setupSecurity()
        PluginImpl.instance.toggleNodeShuttingDown(node.nodeName)
        //As anonymous
        def page = jenkins.createWebClient().getPage(node)
        def el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el == null : "link found"

        //As alice
        page = jenkins.createWebClient().login("alice").getPage(node)
        el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el != null : "No link found"

        //As bobby
        page = jenkins.createWebClient().login("bobby").getPage(node)
        el = page.getFirstByXPath(LINK_TEXT_XPATH)
        assert el == null : "link found"
    }

    /**
     * Tests that the link can't be clicked when the user has no permission.
     */
    @Test(expected = FailingHttpStatusCodeException)
    void testDoIndexNoPermissionAnonymous() {
        setupSecurity()
        jenkins.createWebClient().getPage(node, ShutdownNodeAction.URL)
    }

    /**
     * Tests that the link can't be clicked when the user has permission.
     */
    @Test(expected = FailingHttpStatusCodeException)
    void testDoIndexNoPermissionBobby() {
        setupSecurity()
        jenkins.createWebClient().login("bobby").getPage(node, ShutdownNodeAction.URL)
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     */
    @Test
    void testDoIndexPermissionAlice() {
        setupSecurity()
        def client = jenkins.createWebClient().login("alice")
        startBuild()
		TimeUnit.SECONDS.sleep(1);
        
        client.getPage(node, ShutdownNodeAction.URL)
        
        assert PluginImpl.instance.isNodeShuttingDown(node.nodeName) : "Unaffected"
    }

    /**
     * Tests that the link can be clicked when the user has permission.
     */
    @Test
    void testDoIndexPermissionCancelAlice() {
        setupSecurity()
        def client = jenkins.createWebClient().login("alice")
        PluginImpl.instance.toggleNodeShuttingDown(node.nodeName)
        client.getPage(node, ShutdownNodeAction.URL)
        assert !PluginImpl.instance.isNodeShuttingDown(node.nodeName) : "Unaffected"
    }


    /**
     * Sets up Jenkins security
     */
    void setupSecurity() {
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm())
        GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
        strategy.add(Jenkins.READ, "anonymous");
        strategy.add(Computer.CONNECT, "alice");
        strategy.add(Computer.DISCONNECT, "alice");
        strategy.add(Jenkins.READ, "bobby");
        strategy.add(Jenkins.READ, "alice");
        jenkins.jenkins.setAuthorizationStrategy(strategy);
    }

    void startBuild() {
        def project = jenkins.createFreeStyleProject()
        project.buildersList.add(new SleepBuilder(100000))
        project.setAssignedLabel(node.selfLabel)
        project.scheduleBuild2(0)
    }
}
