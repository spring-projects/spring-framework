/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.util.MBeanTestUtils;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class MBeanServerFactoryBeanTests {


	@Before
	public void setUp() throws Exception {
		MBeanTestUtils.resetMBeanServers();
	}


	@After
	public void tearDown() throws Exception {
		MBeanTestUtils.resetMBeanServers();
	}

	@Test
	public void getObject() throws Exception {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.afterPropertiesSet();
		try {
			MBeanServer server = bean.getObject();
			assertNotNull("The MBeanServer should not be null", server);
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
			assertEquals("The default domain should be foo", "foo", server.getDefaultDomain());
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
				assertSame("Existing MBeanServer not located", server, otherServer);
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
			assertSame(ManagementFactory.getPlatformMBeanServer(), bean.getObject());
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
			assertSame(ManagementFactory.getPlatformMBeanServer(), bean.getObject());
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

			boolean found = false;
			for (MBeanServer candidate : servers) {
				if (candidate == server) {
					found = true;
					break;
				}
			}

			if (!(found == referenceShouldExist)) {
				fail(failMsg);
			}
		}
		finally {
			bean.destroy();
		}
	}

}
