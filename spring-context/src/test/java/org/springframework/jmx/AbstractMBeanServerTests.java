/*
 * Copyright 2002-2011 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.MBeanTestUtils;

/**
 * <strong>Note:</strong> the JMX test suite requires the presence of the
 * {@code jmxremote_optional.jar} in your classpath. Thus, if you
 * run into the <em>"Unsupported protocol: jmxmp"</em> error, you will
 * need to download the
 * <a href="http://www.oracle.com/technetwork/java/javase/tech/download-jsp-141676.html">JMX Remote API 1.0.1_04 Reference Implementation</a>
 * from Oracle and extract {@code jmxremote_optional.jar} into your
 * classpath, for example in the {@code lib/ext} folder of your JVM.
 * See also <a href="https://issuetracker.springsource.com/browse/EBR-349">EBR-349</a>.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public abstract class AbstractMBeanServerTests extends TestCase {

	protected MBeanServer server;

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

	protected void tearDown() throws Exception {
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
