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
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class MBeanServerFactoryBeanTests {


	@BeforeEach
	public void setUp() throws Exception {
		MBeanTestUtils.resetMBeanServers();
	}


	@AfterEach
	public void tearDown() throws Exception {
		MBeanTestUtils.resetMBeanServers();
	}

	@Test
	public void getObject() throws Exception {
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
	public void defaultDomain() throws Exception {
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
	public void withLocateExistingAndExistingServer() {
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
	public void withLocateExistingAndFallbackToPlatformServer() {
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
	public void withEmptyAgentIdAndFallbackToPlatformServer() {
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
	public void createMBeanServer() throws Exception {
		testCreation(true, "The server should be available in the list");
	}

	@Test
	public void newMBeanServer() throws Exception {
		testCreation(false, "The server should not be available in the list");
	}

	private void testCreation(boolean referenceShouldExist, String failMsg) throws Exception {
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
		for (MBeanServer candidate : servers) {
			if (candidate == server) {
				return true;
			}
		}
		return false;
	}

}
