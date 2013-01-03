/*
 * Copyright 2002-2012 the original author or authors.
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

import org.junit.Ignore;

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
public class RemoteMBeanClientInterceptorTestsIgnore extends MBeanClientInterceptorTests {

	private static final String SERVICE_URL = "service:jmx:jmxmp://localhost:9876";

	private JMXConnectorServer connectorServer;

	private JMXConnector connector;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(getServiceUrl(), null, getServer());
		try {
			this.connectorServer.start();
		} catch (BindException ex) {
			// skipping tests, server already running at this port
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

	@Override
	public void tearDown() throws Exception {
		if (this.connector != null) {
			this.connector.close();
		}
		this.connectorServer.stop();
		super.tearDown();
	}

}
