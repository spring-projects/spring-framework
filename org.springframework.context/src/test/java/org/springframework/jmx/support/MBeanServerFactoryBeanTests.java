/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.support;

import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import junit.framework.TestCase;

/**
 * @author Rob Harrop
 */
public class MBeanServerFactoryBeanTests extends TestCase {

	public void testGetObject() throws Exception {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.afterPropertiesSet();
		try {
			MBeanServer server = (MBeanServer) bean.getObject();
			assertNotNull("The MBeanServer should not be null", server);
		}
		finally {
			bean.destroy();
		}
	}

	public void testDefaultDomain() throws Exception {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.setDefaultDomain("foo");
		bean.afterPropertiesSet();
		try {
			MBeanServer server = (MBeanServer) bean.getObject();
			assertEquals("The default domain should be foo", "foo", server.getDefaultDomain());
		}
		finally {
			bean.destroy();
		}
	}

	public void testWithLocateExistingAndExistingServer() {
		MBeanServer server = MBeanServerFactory.createMBeanServer();
		try {
			MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
			bean.setLocateExistingServerIfPossible(true);
			bean.afterPropertiesSet();
			try {
				MBeanServer otherServer = (MBeanServer) bean.getObject();
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

	public void testWithLocateExistingAndNoExistingServer() {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.setLocateExistingServerIfPossible(true);
		bean.afterPropertiesSet();
		try {
			assertNotNull("MBeanServer not created", bean.getObject());
		}
		finally {
			bean.destroy();
		}
	}

	public void testCreateMBeanServer() throws Exception {
		testCreation(true, "The server should be available in the list");
	}

	public void testNewMBeanServer() throws Exception {
		testCreation(false, "The server should not be available in the list");
	}

	private void testCreation(boolean referenceShouldExist, String failMsg) throws Exception {
		MBeanServerFactoryBean bean = new MBeanServerFactoryBean();
		bean.setRegisterWithFactory(referenceShouldExist);
		bean.afterPropertiesSet();

		try {
			MBeanServer server = (MBeanServer) bean.getObject();
			List servers = MBeanServerFactory.findMBeanServer(null);

			boolean found = false;
			for (int x = 0; x < servers.size(); x++) {
				if (servers.get(x) == server) {
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
