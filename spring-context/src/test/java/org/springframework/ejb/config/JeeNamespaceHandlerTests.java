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

package org.springframework.ejb.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;
import org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.tests.sample.beans.ITestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Oliver Gierke
 */
public class JeeNamespaceHandlerTests {

	private ConfigurableListableBeanFactory beanFactory;

	@BeforeEach
	public void setUp() throws Exception {
		GenericApplicationContext ctx = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(
				new ClassPathResource("jeeNamespaceHandlerTests.xml", getClass()));
		ctx.refresh();
		this.beanFactory = ctx.getBeanFactory();
		this.beanFactory.getBeanNamesForType(ITestBean.class);
	}

	@Test
	public void testSimpleDefinition() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("simple");
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
		assertPropertyValue(beanDefinition, "jndiName", "jdbc/MyDataSource");
		assertPropertyValue(beanDefinition, "resourceRef", "true");
	}

	@Test
	public void testComplexDefinition() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("complex");
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(JndiObjectFactoryBean.class.getName());
		assertPropertyValue(beanDefinition, "jndiName", "jdbc/MyDataSource");
		assertPropertyValue(beanDefinition, "resourceRef", "true");
		assertPropertyValue(beanDefinition, "cache", "true");
		assertPropertyValue(beanDefinition, "lookupOnStartup", "true");
		assertPropertyValue(beanDefinition, "exposeAccessContext", "true");
		assertPropertyValue(beanDefinition, "expectedType", "com.myapp.DefaultFoo");
		assertPropertyValue(beanDefinition, "proxyInterface", "com.myapp.Foo");
		assertPropertyValue(beanDefinition, "defaultObject", "myValue");
	}

	@Test
	public void testWithEnvironment() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("withEnvironment");
		assertPropertyValue(beanDefinition, "jndiEnvironment", "foo=bar");
		assertPropertyValue(beanDefinition, "defaultObject", new RuntimeBeanReference("myBean"));
	}

	@Test
	public void testWithReferencedEnvironment() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("withReferencedEnvironment");
		assertPropertyValue(beanDefinition, "jndiEnvironment", new RuntimeBeanReference("myEnvironment"));
		assertThat(beanDefinition.getPropertyValues().contains("environmentRef")).isFalse();
	}

	@Test
	public void testSimpleLocalSlsb() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("simpleLocalEjb");
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(LocalStatelessSessionProxyFactoryBean.class.getName());
		assertPropertyValue(beanDefinition, "businessInterface", ITestBean.class.getName());
		assertPropertyValue(beanDefinition, "jndiName", "ejb/MyLocalBean");
	}

	@Test
	public void testSimpleRemoteSlsb() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("simpleRemoteEjb");
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(SimpleRemoteStatelessSessionProxyFactoryBean.class.getName());
		assertPropertyValue(beanDefinition, "businessInterface", ITestBean.class.getName());
		assertPropertyValue(beanDefinition, "jndiName", "ejb/MyRemoteBean");
	}

	@Test
	public void testComplexLocalSlsb() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("complexLocalEjb");
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(LocalStatelessSessionProxyFactoryBean.class.getName());
		assertPropertyValue(beanDefinition, "businessInterface", ITestBean.class.getName());
		assertPropertyValue(beanDefinition, "jndiName", "ejb/MyLocalBean");
		assertPropertyValue(beanDefinition, "cacheHome", "true");
		assertPropertyValue(beanDefinition, "lookupHomeOnStartup", "true");
		assertPropertyValue(beanDefinition, "resourceRef", "true");
		assertPropertyValue(beanDefinition, "jndiEnvironment", "foo=bar");
	}

	@Test
	public void testComplexRemoteSlsb() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("complexRemoteEjb");
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(SimpleRemoteStatelessSessionProxyFactoryBean.class.getName());
		assertPropertyValue(beanDefinition, "businessInterface", ITestBean.class.getName());
		assertPropertyValue(beanDefinition, "jndiName", "ejb/MyRemoteBean");
		assertPropertyValue(beanDefinition, "cacheHome", "true");
		assertPropertyValue(beanDefinition, "lookupHomeOnStartup", "true");
		assertPropertyValue(beanDefinition, "resourceRef", "true");
		assertPropertyValue(beanDefinition, "jndiEnvironment", "foo=bar");
		assertPropertyValue(beanDefinition, "homeInterface", "org.springframework.tests.sample.beans.ITestBean");
		assertPropertyValue(beanDefinition, "refreshHomeOnConnectFailure", "true");
		assertPropertyValue(beanDefinition, "cacheSessionBean", "true");
	}

	@Test
	public void testLazyInitJndiLookup() throws Exception {
		BeanDefinition definition = this.beanFactory.getMergedBeanDefinition("lazyDataSource");
		assertThat(definition.isLazyInit()).isTrue();
		definition = this.beanFactory.getMergedBeanDefinition("lazyLocalBean");
		assertThat(definition.isLazyInit()).isTrue();
		definition = this.beanFactory.getMergedBeanDefinition("lazyRemoteBean");
		assertThat(definition.isLazyInit()).isTrue();
	}

	private void assertPropertyValue(BeanDefinition beanDefinition, String propertyName, Object expectedValue) {
		assertThat(beanDefinition.getPropertyValues().getPropertyValue(propertyName).getValue()).as("Property '" + propertyName + "' incorrect").isEqualTo(expectedValue);
	}

}
