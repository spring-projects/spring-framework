/*
 * Copyright 2002-2021 the original author or authors.
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

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.MBeanTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MBeanServerFactoryBean}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 */
class MBeanServerFactoryBeanTests {

	@BeforeEach
	@AfterEach
	void resetMBeanServers() throws Exception {
		MBeanTestUtils.resetMBeanServers();
	}

	@Test
	void defaultValues() {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.afterPropertiesSet();
		try {
			MBeanServer server = bean.getObject();
			assertThat(server).as("The MBeanServer should not be null").isNotNull();
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void defaultDomain() {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.setDefaultDomain("foo");
		bean.afterPropertiesSet();
		try {
			MBeanServer server = bean.getObject();
			assertThat(server.getDefaultDomain()).as("The default domain should be foo").isEqualTo("foo");
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void locateExistingServerIfPossibleWithExistingServer() {
		MBeanServer server = MBeanServerFactory.createMBeanServer();
		try {
			MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
			bean.setLocateExistingServerIfPossible(true);
			bean.afterPropertiesSet();
			try {
				MBeanServer otherServer = bean.getObject();
				assertThat(otherServer).as("Existing MBeanServer not located").isSameAs(server);
			}
			finally {
				bean.destroy();
			}
		}
		finally {
			MBeanServerFactory.releaseMBeanServer(server);
		}
	}

	@Test
	void locateExistingServerIfPossibleWithFallbackToPlatformServer() {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.setLocateExistingServerIfPossible(true);
		bean.afterPropertiesSet();
		try {
			assertThat(bean.getObject()).isSameAs(ManagementFactory.getPlatformMBeanServer());
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void withEmptyAgentIdAndFallbackToPlatformServer() {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.setAgentId("");
		bean.afterPropertiesSet();
		try {
			assertThat(bean.getObject()).isSameAs(ManagementFactory.getPlatformMBeanServer());
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void createMBeanServer() throws Exception {
		assertCreation(true, "The server should be available in the list");
	}

	@Test
	void newMBeanServer() throws Exception {
		assertCreation(false, "The server should not be available in the list");
	}


	private void assertCreation(boolean referenceShouldExist, String failMsg) {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.setRegisterWithFactory(referenceShouldExist);
		bean.afterPropertiesSet();
		try {
			MBeanServer server = bean.getObject();
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
			assertThat(hasInstance(servers, server)).as(failMsg).isEqualTo(referenceShouldExist);
		}
		finally {
			bean.destroy();
		}
	}

	private boolean hasInstance(List<MBeanServer> servers, MBeanServer server) {
		return servers.stream().anyMatch(current -> current == server);
	}

}
