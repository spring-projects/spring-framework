/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.aop.Advisor;
import org.springframework.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.aop.interceptor.NopInterceptor;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static java.lang.String.format;
import static org.junit.Assert.*;

/**
 * Unit tests for custom XML namespace handler implementations.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class CustomNamespaceHandlerTests {

	private static final Class<?> CLASS = CustomNamespaceHandlerTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();
	private static final String FQ_PATH = "org/springframework/beans/factory/xml/support";

	private static final String NS_PROPS = format("%s/%s.properties", FQ_PATH, CLASSNAME);
	private static final String NS_XML = format("%s/%s-context.xml", FQ_PATH, CLASSNAME);
	private static final String TEST_XSD = format("%s/%s.xsd", FQ_PATH, CLASSNAME);

	private GenericApplicationContext beanFactory;


	@Before
	public void setUp() throws Exception {
		NamespaceHandlerResolver resolver = new DefaultNamespaceHandlerResolver(CLASS.getClassLoader(), NS_PROPS);
		this.beanFactory = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setNamespaceHandlerResolver(resolver);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.setEntityResolver(new DummySchemaResolver());
		reader.loadBeanDefinitions(getResource());
		this.beanFactory.refresh();
	}


	@Test
	public void testSimpleParser() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertTestBean(bean);
	}

	@Test
	public void testSimpleDecorator() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("customisedTestBean");
		assertTestBean(bean);
	}

	@Test
	public void testProxyingDecorator() throws Exception {
		ITestBean bean = (ITestBean) this.beanFactory.getBean("debuggingTestBean");
		assertTestBean(bean);
		assertTrue(AopUtils.isAopProxy(bean));
		Advisor[] advisors = ((Advised) bean).getAdvisors();
		assertEquals("Incorrect number of advisors", 1, advisors.length);
		assertEquals("Incorrect advice class.", DebugInterceptor.class, advisors[0].getAdvice().getClass());
	}

	@Test
	public void testProxyingDecoratorNoInstance() throws Exception {
		String[] beanNames = this.beanFactory.getBeanNamesForType(ApplicationListener.class);
		assertTrue(Arrays.asList(beanNames).contains("debuggingTestBeanNoInstance"));
		assertEquals(ApplicationListener.class, this.beanFactory.getType("debuggingTestBeanNoInstance"));
		try {
			this.beanFactory.getBean("debuggingTestBeanNoInstance");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getRootCause() instanceof BeanInstantiationException);
		}
	}

	@Test
	public void testChainedDecorators() throws Exception {
		ITestBean bean = (ITestBean) this.beanFactory.getBean("chainedTestBean");
		assertTestBean(bean);
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

	@Test  // SPR-2728
	public void testCustomElementNestedWithinUtilList() throws Exception {
		List<?> things = (List<?>) this.beanFactory.getBean("list.of.things");
		assertNotNull(things);
		assertEquals(2, things.size());
	}

	@Test  // SPR-2728
	public void testCustomElementNestedWithinUtilSet() throws Exception {
		Set<?> things = (Set<?>) this.beanFactory.getBean("set.of.things");
		assertNotNull(things);
		assertEquals(2, things.size());
	}

	@Test  // SPR-2728
	public void testCustomElementNestedWithinUtilMap() throws Exception {
		Map<?, ?> things = (Map<?, ?>) this.beanFactory.getBean("map.of.things");
		assertNotNull(things);
		assertEquals(2, things.size());
	}


	private void assertTestBean(ITestBean bean) {
		assertEquals("Invalid name", "Rob Harrop", bean.getName());
		assertEquals("Invalid age", 23, bean.getAge());
	}

	private Resource getResource() {
		return new ClassPathResource(NS_XML);
	}


	private final class DummySchemaResolver extends PluggableSchemaResolver {

		public DummySchemaResolver() {
			super(CLASS.getClassLoader());
		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws IOException {
			InputSource source = super.resolveEntity(publicId, systemId);
			if (source == null) {
				Resource resource = new ClassPathResource(TEST_XSD);
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
			}
			return source;
		}
	}

}


/**
 * Custom namespace handler implementation.
 *
 * @author Rob Harrop
 */
final class TestNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("testBean", new TestBeanDefinitionParser());
		registerBeanDefinitionParser("person", new PersonDefinitionParser());

		registerBeanDefinitionDecorator("set", new PropertyModifyingBeanDefinitionDecorator());
		registerBeanDefinitionDecorator("debug", new DebugBeanDefinitionDecorator());
		registerBeanDefinitionDecorator("nop", new NopInterceptorBeanDefinitionDecorator());
		registerBeanDefinitionDecoratorForAttribute("object-name", new ObjectNameBeanDefinitionDecorator());
	}


	private static class TestBeanDefinitionParser implements BeanDefinitionParser {

		@Override
		public BeanDefinition parse(Element element, ParserContext parserContext) {
			RootBeanDefinition definition = new RootBeanDefinition();
			definition.setBeanClass(TestBean.class);

			MutablePropertyValues mpvs = new MutablePropertyValues();
			mpvs.add("name", element.getAttribute("name"));
			mpvs.add("age", element.getAttribute("age"));
			definition.setPropertyValues(mpvs);

			parserContext.getRegistry().registerBeanDefinition(element.getAttribute("id"), definition);

			return null;
		}
	}


	private static final class PersonDefinitionParser extends AbstractSingleBeanDefinitionParser {

		@Override
		protected Class<?> getBeanClass(Element element) {
			return TestBean.class;
		}

		@Override
		protected void doParse(Element element, BeanDefinitionBuilder builder) {
			builder.addPropertyValue("name", element.getAttribute("name"));
			builder.addPropertyValue("age", element.getAttribute("age"));
		}
	}


	private static class PropertyModifyingBeanDefinitionDecorator implements BeanDefinitionDecorator {

		@Override
		public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
			Element element = (Element) node;
			BeanDefinition def = definition.getBeanDefinition();

			MutablePropertyValues mpvs = (def.getPropertyValues() == null) ? new MutablePropertyValues() : def.getPropertyValues();
			mpvs.add("name", element.getAttribute("name"));
			mpvs.add("age", element.getAttribute("age"));

			((AbstractBeanDefinition) def).setPropertyValues(mpvs);
			return definition;
		}
	}


	private static class DebugBeanDefinitionDecorator extends AbstractInterceptorDrivenBeanDefinitionDecorator {

		@Override
		protected BeanDefinition createInterceptorDefinition(Node node) {
			return new RootBeanDefinition(DebugInterceptor.class);
		}
	}


	private static class NopInterceptorBeanDefinitionDecorator extends AbstractInterceptorDrivenBeanDefinitionDecorator {

		@Override
		protected BeanDefinition createInterceptorDefinition(Node node) {
			return new RootBeanDefinition(NopInterceptor.class);
		}
	}


	private static class ObjectNameBeanDefinitionDecorator implements BeanDefinitionDecorator {

		@Override
		public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
			Attr objectNameAttribute = (Attr) node;
			definition.getBeanDefinition().setAttribute("objectName", objectNameAttribute.getValue());
			return definition;
		}
	}

}
