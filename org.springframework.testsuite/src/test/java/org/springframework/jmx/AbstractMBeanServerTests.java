/*
 * Copyright 2002-2007 the original author or authors.
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

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractMBeanServerTests extends TestCase {

	protected MBeanServer server;

	public final void setUp() throws Exception {
		this.server = MBeanServerFactory.createMBeanServer();
		try {
			onSetUp();
		}
		catch (Exception ex) {
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

	private void releaseServer() {
		MBeanServerFactory.releaseMBeanServer(getServer());
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
