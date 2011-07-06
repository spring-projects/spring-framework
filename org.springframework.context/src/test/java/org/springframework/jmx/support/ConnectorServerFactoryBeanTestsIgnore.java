/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.support;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Ignore;
import org.springframework.jmx.AbstractMBeanServerTests;

/**
 * @author Rob Harrop
 */
// TODO [SPR-8089] Clean up ignored JMX tests.
//
// @Ignore should have no effect for JUnit 3.8 tests; however, it appears
// that tests on the CI server -- as well as those in Eclipse -- do in
// fact get ignored. So we leave @Ignore here so that developers can
// easily search for ignored tests.
@Ignore("Requires jmxremote_optional.jar; see comments in AbstractMBeanServerTests for details.")
public class ConnectorServerFactoryBeanTestsIgnore extends AbstractMBeanServerTests {

	private static final String OBJECT_NAME = "spring:type=connector,name=test";

	public void testStartupWithLocatedServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.afterPropertiesSet();

		try {
			checkServerConnection(getServer());
		} finally {
			bean.destroy();
		}
	}

	public void testStartupWithSuppliedServer() throws Exception {
		//Added a brief snooze here - seems to fix occasional
		//java.net.BindException: Address already in use errors
		Thread.sleep(1);

		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServer(getServer());
		bean.afterPropertiesSet();

		try {
			checkServerConnection(getServer());
		} finally {
			bean.destroy();
		}
	}

	public void testRegisterWithMBeanServer() throws Exception {
		//Added a brief snooze here - seems to fix occasional
		//java.net.BindException: Address already in use errors
		Thread.sleep(1);
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setObjectName(OBJECT_NAME);
		bean.afterPropertiesSet();

		try {
			// Try to get the connector bean.
			ObjectInstance instance = getServer().getObjectInstance(ObjectName.getInstance(OBJECT_NAME));
			assertNotNull("ObjectInstance should not be null", instance);
		} finally {
			bean.destroy();
		}
	}

	public void testNoRegisterWithMBeanServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.afterPropertiesSet();

		try {
			// Try to get the connector bean.
			getServer().getObjectInstance(ObjectName.getInstance(OBJECT_NAME));
			fail("Instance should not be found");
		} catch (InstanceNotFoundException ex) {
			// expected
		} finally {
			bean.destroy();
		}
	}

	private void checkServerConnection(MBeanServer hostedServer) throws IOException, MalformedURLException {
		// Try to connect using client.
		JMXServiceURL serviceURL = new JMXServiceURL(ConnectorServerFactoryBean.DEFAULT_SERVICE_URL);
		JMXConnector connector = JMXConnectorFactory.connect(serviceURL);

		assertNotNull("Client Connector should not be null", connector);

		// Get the MBean server connection.
		MBeanServerConnection connection = connector.getMBeanServerConnection();
		assertNotNull("MBeanServerConnection should not be null", connection);

		// Test for MBean server equality.
		assertEquals("Registered MBean count should be the same", hostedServer.getMBeanCount(),
				connection.getMBeanCount());
	}

}
