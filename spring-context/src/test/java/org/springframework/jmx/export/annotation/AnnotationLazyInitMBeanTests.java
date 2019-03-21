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

package org.springframework.jmx.export.annotation;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.support.ObjectNameManager;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AnnotationLazyInitMBeanTests {

	@Test
	public void lazyNaming() throws Exception {
		ConfigurableApplicationContext ctx =
				new ClassPathXmlApplicationContext("org/springframework/jmx/export/annotation/lazyNaming.xml");
		try {
			MBeanServer server = (MBeanServer) ctx.getBean("server");
			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
			assertNotNull(server.getObjectInstance(oname));
			String name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "TEST", name);
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void lazyAssembling() throws Exception {
		System.setProperty("domain", "bean");
		ConfigurableApplicationContext ctx =
				new ClassPathXmlApplicationContext("org/springframework/jmx/export/annotation/lazyAssembling.xml");
		try {
			MBeanServer server = (MBeanServer) ctx.getBean("server");

			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
			assertNotNull(server.getObjectInstance(oname));
			String name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "TEST", name);

			oname = ObjectNameManager.getInstance("bean:name=testBean5");
			assertNotNull(server.getObjectInstance(oname));
			name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "FACTORY", name);

			oname = ObjectNameManager.getInstance("spring:mbean=true");
			assertNotNull(server.getObjectInstance(oname));
			name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "Rob Harrop", name);

			oname = ObjectNameManager.getInstance("spring:mbean=another");
			assertNotNull(server.getObjectInstance(oname));
			name = (String) server.getAttribute(oname, "Name");
			assertEquals("Invalid name returned", "Juergen Hoeller", name);
		}
		finally {
			System.clearProperty("domain");
			ctx.close();
		}
	}

	@Test
	public void componentScan() throws Exception {
		ConfigurableApplicationContext ctx =
				new ClassPathXmlApplicationContext("org/springframework/jmx/export/annotation/componentScan.xml");
		try {
			MBeanServer server = (MBeanServer) ctx.getBean("server");
			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
			assertNotNull(server.getObjectInstance(oname));
			String name = (String) server.getAttribute(oname, "Name");
			assertNull(name);
		}
		finally {
			ctx.close();
		}
	}

}
