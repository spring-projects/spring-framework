/*
 * Copyright 2002-2024 the original author or authors.
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

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.testfixture.net.TestSocketUtils;
import org.springframework.jmx.AbstractMBeanServerTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Integration tests for {@link MBeanServerConnectionFactoryBean}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class MBeanServerConnectionFactoryBeanTests extends AbstractMBeanServerTests {

	@SuppressWarnings("deprecation")
	private final String serviceUrl = "service:jmx:jmxmp://localhost:" + TestSocketUtils.findAvailableTcpPort();


	@Test
	void noServiceUrl() {
		MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
		assertThatIllegalArgumentException()
			.isThrownBy(bean::afterPropertiesSet)
			.withMessage("Property 'serviceUrl' is required");
	}

	@Test
	void validConnection() throws Exception {
		JMXConnectorServer connectorServer = startConnectorServer();

		try {
			MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
			bean.setServiceUrl(this.serviceUrl);
			bean.afterPropertiesSet();

			try {
				MBeanServerConnection connection = bean.getObject();
				assertThat(connection).as("Connection should not be null").isNotNull();

				// perform simple MBean count test
				assertThat(connection.getMBeanCount()).as("MBean count should be the same").isEqualTo(getServer().getMBeanCount());
			}
			finally {
				bean.destroy();
			}
		}
		finally {
			connectorServer.stop();
		}
	}

	@Test
	void lazyConnection() throws Exception {
		MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.setConnectOnStartup(false);
		bean.afterPropertiesSet();

		MBeanServerConnection connection = bean.getObject();
		assertThat(AopUtils.isAopProxy(connection)).isTrue();

		JMXConnectorServer connector = null;
		try {
			connector = startConnectorServer();
			assertThat(connection.getMBeanCount()).as("Incorrect MBean count").isEqualTo(getServer().getMBeanCount());
		}
		finally {
			bean.destroy();
			if (connector != null) {
				connector.stop();
			}
		}
	}

	@Test
	void lazyConnectionAndNoAccess() throws Exception {
		MBeanServerConnectionFactoryBean bean = new MBeanServerConnectionFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.setConnectOnStartup(false);
		bean.afterPropertiesSet();

		MBeanServerConnection connection = bean.getObject();
		assertThat(AopUtils.isAopProxy(connection)).isTrue();
		bean.destroy();
	}

	private JMXConnectorServer startConnectorServer() throws Exception {
		JMXServiceURL jmxServiceUrl = new JMXServiceURL(this.serviceUrl);
		JMXConnectorServer connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceUrl, null, getServer());
		connectorServer.start();
		return connectorServer;
	}

}
