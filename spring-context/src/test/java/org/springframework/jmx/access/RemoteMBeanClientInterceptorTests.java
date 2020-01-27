/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.junit.jupiter.api.AfterEach;

import org.springframework.util.SocketUtils;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 */
class RemoteMBeanClientInterceptorTests extends MBeanClientInterceptorTests {

	private final int servicePort = SocketUtils.findAvailableTcpPort();

	private final String serviceUrl = "service:jmx:jmxmp://localhost:" + servicePort;


	private JMXConnectorServer connectorServer;

	private JMXConnector connector;


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(getServiceUrl(), null, getServer());
		try {
			this.connectorServer.start();
		}
		catch (BindException ex) {
			System.out.println("Skipping remote JMX tests because binding to local port ["
					+ this.servicePort + "] failed: " + ex.getMessage());
			runTests = false;
		}
	}

	private JMXServiceURL getServiceUrl() throws MalformedURLException {
		return new JMXServiceURL(this.serviceUrl);
	}

	@Override
	protected MBeanServerConnection getServerConnection() throws Exception {
		this.connector = JMXConnectorFactory.connect(getServiceUrl());
		return this.connector.getMBeanServerConnection();
	}

	@AfterEach
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
