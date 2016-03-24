/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jmx.access;

import java.net.BindException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.After;

import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.util.SocketUtils;

/**
 * To run the tests in the class, set the following Java system property:
 * {@code -DtestGroups=jmxmp}.
 *
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 */
public class RemoteMBeanClientInterceptorTests extends MBeanClientInterceptorTests {

	private static final int SERVICE_PORT;
	private static final String SERVICE_URL;

	static {
		SERVICE_PORT = SocketUtils.findAvailableTcpPort();
		SERVICE_URL = "service:jmx:jmxmp://localhost:" + SERVICE_PORT;
	}

	private JMXConnectorServer connectorServer;

	private JMXConnector connector;

	@Override
	public void onSetUp() throws Exception {
		runTests = false;
		Assume.group(TestGroup.JMXMP);
		runTests = true;
		super.onSetUp();
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(getServiceUrl(), null, getServer());
		try {
			this.connectorServer.start();
		}
		catch (BindException ex) {
			System.out.println("Skipping remote JMX tests because binding to local port ["
					+ SERVICE_PORT + "] failed: " + ex.getMessage());
			runTests = false;
		}
	}

	private JMXServiceURL getServiceUrl() throws MalformedURLException {
		return new JMXServiceURL(SERVICE_URL);
	}

	@Override
	protected MBeanServerConnection getServerConnection() throws Exception {
		this.connector = JMXConnectorFactory.connect(getServiceUrl());
		return this.connector.getMBeanServerConnection();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		if (this.connector != null) {
			this.connector.close();
		}
		if (this.connectorServer != null) {
			this.connectorServer.stop();
		}
		if (runTests) {
			super.tearDown();
		}
	}

}
