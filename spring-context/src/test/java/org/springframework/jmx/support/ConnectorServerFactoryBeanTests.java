/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.jmx.AbstractMBeanServerTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link ConnectorServerFactoryBean}.
 *
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 */
class ConnectorServerFactoryBeanTests extends AbstractMBeanServerTests {

	private static final String OBJECT_NAME = "spring:type=connector,name=test";

	@SuppressWarnings("deprecation")
	private final String serviceUrl = "service:jmx:jmxmp://localhost:" + org.springframework.util.SocketUtils.findAvailableTcpPort();


	@Test
	void startupWithLocatedServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.afterPropertiesSet();

		try {
			checkServerConnection(getServer());
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void startupWithSuppliedServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.setServer(getServer());
		bean.afterPropertiesSet();

		try {
			checkServerConnection(getServer());
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void registerWithMBeanServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.setObjectName(OBJECT_NAME);
		bean.afterPropertiesSet();

		try {
			// Try to get the connector bean.
			ObjectInstance instance = getServer().getObjectInstance(ObjectName.getInstance(OBJECT_NAME));
			assertThat(instance).as("ObjectInstance should not be null").isNotNull();
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void noRegisterWithMBeanServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.afterPropertiesSet();
		try {
			// Try to get the connector bean.
			assertThatExceptionOfType(InstanceNotFoundException.class).isThrownBy(() ->
				getServer().getObjectInstance(ObjectName.getInstance(OBJECT_NAME)));
		}
		finally {
			bean.destroy();
		}
	}

	private void checkServerConnection(MBeanServer hostedServer) throws IOException, MalformedURLException {
		// Try to connect using client.
		JMXServiceURL serviceURL = new JMXServiceURL(this.serviceUrl);
		JMXConnector connector = JMXConnectorFactory.connect(serviceURL);

		assertThat(connector).as("Client Connector should not be null").isNotNull();

		// Get the MBean server connection.
		MBeanServerConnection connection = connector.getMBeanServerConnection();
		assertThat(connection).as("MBeanServerConnection should not be null").isNotNull();

		// Test for MBean server equality.
		assertThat(connection.getMBeanCount()).as("Registered MBean count should be the same").isEqualTo(hostedServer.getMBeanCount());
	}

}
