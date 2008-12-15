/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.factory.xml.support;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.aop.interceptor.NopInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.xml.sax.InputSource;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 */
public final class CustomNamespaceHandlerTests {

	private DefaultListableBeanFactory beanFactory;


	@Before
	public void setUp() throws Exception {
		String location = "org/springframework/beans/factory/xml/support/customNamespace.properties";
		NamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver(getClass().getClassLoader(), location);
		this.beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setNamespaceHandlerResolver(resolver);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.setEntityResolver(new DummySchemaResolver());
		reader.loadBeanDefinitions(getResource());
	}


	@Test
	public void testSimpleParser() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assetTestBean(bean);
	}

	@Test
	public void testSimpleDecorator() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("customisedTestBean");
		assetTestBean(bean);
	}

	@Test
	public void testProxyingDecorator() throws Exception {
		ITestBean bean = (ITestBean) this.beanFactory.getBean("debuggingTestBean");
		assetTestBean(bean);
		assertTrue(AopUtils.isAopProxy(bean));
		Advisor[] advisors = ((Advised) bean).getAdvisors();
		assertEquals("Incorrect number of advisors", 1, advisors.length);
		assertEquals("Incorrect advice class.", DebugInterceptor.class, advisors[0].getAdvice().getClass());
	}

	@Test
	public void testChainedDecorators() throws Exception {
		ITestBean bean = (ITestBean) this.beanFactory.getBean("chainedTestBean");
		assetTestBean(bean);
		assertTrue(AopUtils.isAopProxy(bean));
		Advisor[] advisors = ((Advised) bean).getAdvisors();
		assertEquals("Incorrect number of advisors", 2, advisors.length);
		assertEquals("Incorrect advice class.", DebugInterceptor.class, advisors[0].getAdvice().getClass());
		assertEquals("Incorrect advice class.", NopInterceptor.class, advisors[1].getAdvice().getClass());
	}

	@Test
	public void testDecorationViaAttribute() throws Exception {
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("decorateWithAttribute");
		assertEquals("foo", beanDefinition.getAttribute("objectName"));
	}

	/**
	 * http://opensource.atlassian.com/projects/spring/browse/SPR-2728
	 */
	@Test
	public void testCustomElementNestedWithinUtilList() throws Exception {
		List<?> things = (List<?>) this.beanFactory.getBean("list.of.things");
		assertNotNull(things);
		assertEquals(2, things.size());
	}

	/**
	 * http://opensource.atlassian.com/projects/spring/browse/SPR-2728
	 */
	@Test
	public void testCustomElementNestedWithinUtilSet() throws Exception {
		Set<?> things = (Set<?>) this.beanFactory.getBean("set.of.things");
		assertNotNull(things);
		assertEquals(2, things.size());
	}

	/**
	 * http://opensource.atlassian.com/projects/spring/browse/SPR-2728
	 */
	@Test
	public void testCustomElementNestedWithinUtilMap() throws Exception {
		Map<?, ?> things = (Map<?, ?>) this.beanFactory.getBean("map.of.things");
		assertNotNull(things);
		assertEquals(2, things.size());
	}


	private void assetTestBean(ITestBean bean) {
		assertEquals("Invalid name", "Rob Harrop", bean.getName());
		assertEquals("Invalid age", 23, bean.getAge());
	}

	private Resource getResource() {
		return new ClassPathResource("customNamespace.xml", getClass());
	}


	private final class DummySchemaResolver extends PluggableSchemaResolver {

		public DummySchemaResolver() {
			super(CustomNamespaceHandlerTests.this.getClass().getClassLoader());
		}


		public InputSource resolveEntity(String publicId, String systemId) throws IOException {
			InputSource source = super.resolveEntity(publicId, systemId);
			if (source == null) {
				Resource resource = new ClassPathResource("org/springframework/beans/factory/xml/support/spring-test.xsd");
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
			}
			return source;
		}
	}

}
