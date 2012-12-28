/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jmx.support;

import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Ignore;
import org.springframework.aop.support.AopUtils;
import org.springframework.jmx.AbstractMBeanServerTests;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class MBeanServerConnectionFactoryBeanTests extends AbstractMBeanServerTests {

	private static final String SERVICE_URL = "service:jmx:jmxmp://localhost:9876";

	private JMXServiceURL getServiceUrl() throws MalformedURLException {
		return new JMXServiceURL(SERVICE_URL);
	}

	private JMXConnectorServer getConnectorServer() throws Exception {
		return JMXConnectorServerFactory.newJMXConnectorServer(getServiceUrl(), null, getServer());
	}

	// TODO [SPR-8089] Clean up ignored JMX tests.
	//
	// @Ignore should have no effect for JUnit 3.8 tests; however, it appears
	// that tests on the CI server -- as well as those in Eclipse -- do in
	// fact get ignored. So we leave @Ignore here so that developers can
	// easily search for ignored tests.
	//
	// Once fixed, renamed to test* instead of ignore*.
	@Ignore("Requires jmxremote_optional.jar; see comments in AbstractMBeanServerTests for details.")
	public void ignoreTestValidConnection() throws Exception {
		JMXConnectorServer connectorServer = getConnectorServer();
		connectorServer.start();

		try {
			MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
			bean.setServiceUrl(SERVICE_URL);
			bean.afterPropertiesSet();

			try {
				MBeanServerConnection connection = bean.getObject();
				assertNotNull("Connection should not be null", connection);

				// perform simple MBean count test
				assertEquals("MBean count should be the same", getServer().getMBeanCount(), connection.getMBeanCount());
			} finally {
				bean.destroy();
			}
		} finally {
			connectorServer.stop();
		}
	}

	public void testWithNoServiceUrl() throws Exception {
		MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
		try {
			bean.afterPropertiesSet();
			fail("IllegalArgumentException should be raised when no service url is provided");
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	// TODO [SPR-8089] Clean up ignored JMX tests.
	//
	// @Ignore should have no effect for JUnit 3.8 tests; however, it appears
	// that tests on the CI server -- as well as those in Eclipse -- do in
	// fact get ignored. So we leave @Ignore here so that developers can
	// easily search for ignored tests.
	//
	// Once fixed, renamed to test* instead of ignore*.
	@Ignore("Requires jmxremote_optional.jar; see comments in AbstractMBeanServerTests for details.")
	public void ignoreTestWithLazyConnection() throws Exception {
		MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
		bean.setServiceUrl(SERVICE_URL);
		bean.setConnectOnStartup(false);
		bean.afterPropertiesSet();

		MBeanServerConnection connection = bean.getObject();
		assertTrue(AopUtils.isAopProxy(connection));

		JMXConnectorServer connector = null;
		try {
			connector = getConnectorServer();
			connector.start();
			assertEquals("Incorrect MBean count", getServer().getMBeanCount(), connection.getMBeanCount());
		} finally {
			bean.destroy();
			if (connector != null) {
				connector.stop();
			}
		}
	}

	public void testWithLazyConnectionAndNoAccess() throws Exception {
		MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
		bean.setServiceUrl(SERVICE_URL);
		bean.setConnectOnStartup(false);
		bean.afterPropertiesSet();

		MBeanServerConnection connection = bean.getObject();
		assertTrue(AopUtils.isAopProxy(connection));
		bean.destroy();
	}

}
