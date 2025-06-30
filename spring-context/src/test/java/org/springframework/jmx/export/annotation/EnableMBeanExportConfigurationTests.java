/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.MBeanExportConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.testfixture.jmx.export.Person;
import org.springframework.jmx.export.TestDynamicMBean;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link EnableMBeanExport} and {@link MBeanExportConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see AnnotationLazyInitMBeanTests
 */
class EnableMBeanExportConfigurationTests {

	private AnnotationConfigApplicationContext ctx;


	@AfterEach
	void closeContext() {
		if (this.ctx != null) {
			this.ctx.close();
		}
	}


	@Test
	void testLazyNaming() throws Exception {
		load(LazyNamingConfiguration.class);
		validateAnnotationTestBean();
	}

	private void load(Class<?>... config) {
		this.ctx = new AnnotationConfigApplicationContext(config);
	}

	@Test
	void testOnlyTargetClassIsExposed() throws Exception {
		load(ProxyConfiguration.class);
		validateAnnotationTestBean();
	}

	@Test
	@SuppressWarnings("resource")
	public void testPackagePrivateExtensionCantBeExposed() {
		assertThatExceptionOfType(InvalidMetadataException.class).isThrownBy(() ->
				new AnnotationConfigApplicationContext(PackagePrivateConfiguration.class))
			.withMessageContaining(PackagePrivateTestBean.class.getName())
			.withMessageContaining("must be public");
	}

	@Test
	@SuppressWarnings("resource")
	public void testPackagePrivateImplementationCantBeExposed() {
		assertThatExceptionOfType(InvalidMetadataException.class).isThrownBy(() ->
				new AnnotationConfigApplicationContext(PackagePrivateInterfaceImplementationConfiguration.class))
			.withMessageContaining(PackagePrivateAnnotationTestBean.class.getName())
			.withMessageContaining("must be public");
	}

	@Test
	void testPackagePrivateClassExtensionCanBeExposed() throws Exception {
		load(PackagePrivateExtensionConfiguration.class);
		validateAnnotationTestBean();
	}

	@Test
	void testPlaceholderBased() throws Exception {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("serverName", "server");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.setEnvironment(env);
		context.register(PlaceholderBasedConfiguration.class);
		context.refresh();
		this.ctx = context;
		validateAnnotationTestBean();
	}

	@Test
	void testLazyAssembling() throws Exception {
		System.setProperty("domain", "bean");
		load(LazyAssemblingConfiguration.class);
		try {
			MBeanServer server = (MBeanServer) this.ctx.getBean("server");

			validateMBeanAttribute(server, "bean:name=testBean4", "TEST");
			validateMBeanAttribute(server, "bean:name=testBean5", "FACTORY");
			validateMBeanAttribute(server, "spring:mbean=true", "Rob Harrop");
			validateMBeanAttribute(server, "spring:mbean=another", "Juergen Hoeller");
		}
		finally {
			System.clearProperty("domain");
		}
	}

	@Test
	void testComponentScan() throws Exception {
		load(ComponentScanConfiguration.class);
		MBeanServer server = (MBeanServer) this.ctx.getBean("server");
		validateMBeanAttribute(server, "bean:name=testBean4", null);
	}

	private void validateAnnotationTestBean() throws Exception {
		MBeanServer server = (MBeanServer) this.ctx.getBean("server");
		validateMBeanAttribute(server,"bean:name=testBean4", "TEST");
	}

	private void validateMBeanAttribute(MBeanServer server, String objectName, String expected) throws Exception {
		ObjectName oname = ObjectNameManager.getInstance(objectName);
		assertThat(server.getObjectInstance(oname)).isNotNull();
		String name = (String) server.getAttribute(oname, "Name");
		assertThat(name).as("Invalid name returned").isEqualTo(expected);
	}


	@Configuration
	@EnableMBeanExport(server = "server")
	static class LazyNamingConfiguration {

		@Bean
		public MBeanServerFactoryBean server() {
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
	@EnableMBeanExport(server = "server")
	static class ProxyConfiguration {

		@Bean
		public MBeanServerFactoryBean server() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		@Lazy
		@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
		public AnnotationTestBean testBean() {
			AnnotationTestBean bean = new AnnotationTestBean();
			bean.setName("TEST");
			bean.setAge(100);
			return bean;
		}
	}


	@Configuration
	@EnableMBeanExport(server = "${serverName}")
	static class PlaceholderBasedConfiguration {

		@Bean
		public MBeanServerFactoryBean server() {
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
		public MBeanServerFactoryBean server() {
			return new MBeanServerFactoryBean();
		}

		@Bean("bean:name=testBean4")
		@Lazy
		public AnnotationTestBean testBean4() {
			AnnotationTestBean bean = new AnnotationTestBean();
			bean.setName("TEST");
			bean.setAge(100);
			return bean;
		}

		@Bean("bean:name=testBean5")
		public AnnotationTestBeanFactory testBean5() {
			return new AnnotationTestBeanFactory();
		}

		@Bean(name="spring:mbean=true")
		@Lazy
		public TestDynamicMBean dynamic() {
			return new TestDynamicMBean();
		}

		@Bean(name="spring:mbean=another")
		@Lazy
		public Person person() {
			Person person = new Person();
			person.setName("Juergen Hoeller");
			return person;
		}

		@Bean
		@Lazy
		public Object notLoadable() throws Exception {
			return Class.forName("does.not.exist").getDeclaredConstructor().newInstance();
		}
	}


	@Configuration
	@ComponentScan(excludeFilters = @ComponentScan.Filter(Configuration.class))
	@EnableMBeanExport(server = "server")
	static class ComponentScanConfiguration {

		@Bean
		public MBeanServerFactoryBean server() {
			return new MBeanServerFactoryBean();
		}
	}

	@Configuration
	@EnableMBeanExport(server = "server")
	static class PackagePrivateConfiguration {

		@Bean
		public MBeanServerFactoryBean server() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		public PackagePrivateTestBean testBean() {
			return new PackagePrivateTestBean();
		}
	}

	@ManagedResource(objectName = "bean:name=packagePrivate")
	private static class PackagePrivateTestBean {

		private String name;

		@ManagedAttribute
		public String getName() {
			return this.name;
		}

		@ManagedAttribute
		public void setName(String name) {
			this.name = name;
		}
	}


	@Configuration
	@EnableMBeanExport(server = "server")
	static class PackagePrivateExtensionConfiguration {

		@Bean
		public MBeanServerFactoryBean server() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		public PackagePrivateTestBeanExtension testBean() {
			PackagePrivateTestBeanExtension bean = new PackagePrivateTestBeanExtension();
			bean.setName("TEST");
			return bean;
		}
	}

	private static class PackagePrivateTestBeanExtension extends AnnotationTestBean {

	}

	@Configuration
	@EnableMBeanExport(server = "server")
	static class PackagePrivateInterfaceImplementationConfiguration {

		@Bean
		public MBeanServerFactoryBean server() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		public PackagePrivateAnnotationTestBean testBean() {
			return new PackagePrivateAnnotationTestBean();
		}
	}

	private static class PackagePrivateAnnotationTestBean implements AnotherAnnotationTestBean {

		private String bar;

		@Override
		public void foo() {
		}

		@Override
		public String getBar() {
			return this.bar;
		}

		@Override
		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public int getCacheEntries() {
			return 0;
		}
	}

}
