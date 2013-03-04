/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.tests.TestGroup;
import org.springframework.util.MBeanTestUtils;

import static org.junit.Assert.*;

/**
 * <strong>Note:</strong> certain tests throughout this hierarchy require the presence of
 * the {@code jmxremote_optional.jar} in your classpath. For this reason, these tests are
 * run only if {@link TestGroup#JMXMP} is enabled. If you wish to run these tests, follow
 * the instructions in the TestGroup class to enable JMXMP tests. If you run into the
 * <em>"Unsupported protocol: jmxmp"</em> error, you will need to download the
 * <a href="http://www.oracle.com/technetwork/java/javase/tech/download-jsp-141676.html">
 * JMX Remote API 1.0.1_04 Reference Implementation</a> from Oracle and extract
 * {@code jmxremote_optional.jar} into your classpath, for example in the {@code lib/ext}
 * folder of your JVM.
 * See also <a href="https://issuetracker.springsource.com/browse/EBR-349">EBR-349</a>.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 */
public abstract class AbstractMBeanServerTests {

	protected MBeanServer server;

	@Before
	public final void setUp() throws Exception {
		this.server = MBeanServerFactory.createMBeanServer();
		try {
			onSetUp();
		} catch (Exception ex) {
			releaseServer();
			throw ex;
		}
	}

	protected ConfigurableApplicationContext loadContext(String configLocation) {
		GenericApplicationContext ctx = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(configLocation);
		ctx.getDefaultListableBeanFactory().registerSingleton("server", this.server);
		ctx.refresh();
		return ctx;
	}

	@After
	public void tearDown() throws Exception {
		releaseServer();
		onTearDown();
	}

	private void releaseServer() throws Exception {
		MBeanServerFactory.releaseMBeanServer(getServer());
		MBeanTestUtils.resetMBeanServers();
	}

	protected void onTearDown() throws Exception {
	}

	protected void onSetUp() throws Exception {
	}

	public MBeanServer getServer() {
		return this.server;
	}

	protected void assertIsRegistered(String message, ObjectName objectName) {
		assertTrue(message, getServer().isRegistered(objectName));
	}

	protected void assertIsNotRegistered(String message, ObjectName objectName) {
		assertFalse(message, getServer().isRegistered(objectName));
	}

}
