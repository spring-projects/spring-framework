/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.opentest4j.TestAbortedException;

import org.springframework.core.testfixture.net.TestSocketUtils;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 */
class RemoteMBeanClientInterceptorTests extends MBeanClientInterceptorTests {

	@SuppressWarnings("deprecation")
	private final int servicePort = TestSocketUtils.findAvailableTcpPort();

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
			runTests = false;
			throw new TestAbortedException("Skipping remote JMX tests because binding to local port [%s] failed: %s"
					.formatted(this.servicePort, ex.getMessage()));
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
	protected void tearDown() throws Exception {
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
