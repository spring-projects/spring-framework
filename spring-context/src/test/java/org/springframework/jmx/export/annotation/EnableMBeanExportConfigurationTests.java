/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx.export.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jmx.export.MBeanExporterTests;
import org.springframework.jmx.export.TestDynamicMBean;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * Tests for {@link EnableMBeanExport} and {@link MBeanExportConfiguration}.
 *
 * @author Phillip Webb
 * @see AnnotationLazyInitMBeanTests
 */
public class EnableMBeanExportConfigurationTests {

	@Test
	public void testLazyNaming() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				LazyNamingConfiguration.class);
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
	public void testLazyAssembling() throws Exception {
		AnnotationConfigApplicationContext ctx =
				new AnnotationConfigApplicationContext(LazyAssemblingConfiguration.class);
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
			ctx.close();
		}
	}

	@Test
	public void testComponentScan() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ComponentScanConfiguration.class);
		try {
			MBeanServer server = (MBeanServer) ctx.getBean("server");
			ObjectName oname = ObjectNameManager.getInstance("bean:name=testBean4");
			assertNotNull(server.getObjectInstance(oname));
			String name = (String) server.getAttribute(oname, "Name");
			assertNull(name);
		} finally {
			ctx.close();
		}
	}

	@Configuration
	@EnableMBeanExport(server = "server")
	static class LazyNamingConfiguration {

		@Bean
		public MBeanServerFactoryBean server() throws Exception {
			return new MBeanServerFactoryBean();
		}

		@Bean
		@Lazy
		public AnnotationTestBean testBean() {
			AnnotationTestBean bean = new AnnotationTestBean();
			bean.setName("TEST");
			bean.setAge(100);
			return bean;
		}
	}

	@Configuration
	@EnableMBeanExport(server="server", registration=RegistrationPolicy.REPLACE_EXISTING)
	static class LazyAssemblingConfiguration {

		@Bean
		public MBeanServerFactoryBean server() throws Exception {
			return new MBeanServerFactoryBean();
		}

		@Bean(name="bean:name=testBean4")
		@Lazy
		public AnnotationTestBean testBean4() {
			AnnotationTestBean bean = new AnnotationTestBean();
			bean.setName("TEST");
			bean.setAge(100);
			return bean;
		}

		@Bean(name="bean:name=testBean5")
		public AnnotationTestBeanFactory testBean5() throws Exception {
			return new AnnotationTestBeanFactory();
		}

		@Bean(name="spring:mbean=true")
		@Lazy
		public TestDynamicMBean dynamic() {
			return new TestDynamicMBean();
		}

		@Bean(name="spring:mbean=another")
		@Lazy
		public MBeanExporterTests.Person person() {
			MBeanExporterTests.Person person = new MBeanExporterTests.Person();
			person.setName("Juergen Hoeller");
			return person;
		}

		@Bean
		@Lazy
		public Object notLoadable() throws Exception {
			return Class.forName("does.not.exist").newInstance();
		}
	}

	@Configuration
	@ComponentScan(excludeFilters={@ComponentScan.Filter(value=Configuration.class)})
	@EnableMBeanExport(server = "server")
	static class ComponentScanConfiguration {

		@Bean
		public MBeanServerFactoryBean server() throws Exception {
			return new MBeanServerFactoryBean();
		}
	}

}
