/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.ResourceTestBean;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ClassPathXmlApplicationContextTests {

	private static final String PATH = "/org/springframework/context/support/";
	private static final String RESOURCE_CONTEXT = PATH + "ClassPathXmlApplicationContextTests-resource.xml";
	private static final String CONTEXT_WILDCARD = PATH + "test/context*.xml";
	private static final String CONTEXT_A = "test/contextA.xml";
	private static final String CONTEXT_B = "test/contextB.xml";
	private static final String CONTEXT_C = "test/contextC.xml";
	private static final String FQ_CONTEXT_A = PATH + CONTEXT_A;
	private static final String FQ_CONTEXT_B = PATH + CONTEXT_B;
	private static final String FQ_CONTEXT_C = PATH + CONTEXT_C;
	private static final String SIMPLE_CONTEXT = "simpleContext.xml";
	private static final String FQ_SIMPLE_CONTEXT = PATH + "simpleContext.xml";
	private static final String FQ_ALIASED_CONTEXT_C = PATH + "test/aliased-contextC.xml";
	private static final String INVALID_VALUE_TYPE_CONTEXT = PATH + "invalidValueType.xml";
	private static final String CHILD_WITH_PROXY_CONTEXT = PATH + "childWithProxy.xml";
	private static final String INVALID_CLASS_CONTEXT = "invalidClass.xml";
	private static final String CLASS_WITH_PLACEHOLDER_CONTEXT = "classWithPlaceholder.xml";
	private static final String ALIAS_THAT_OVERRIDES_PARENT_CONTEXT = PATH + "aliasThatOverridesParent.xml";
	private static final String ALIAS_FOR_PARENT_CONTEXT = PATH + "aliasForParent.xml";
	private static final String TEST_PROPERTIES = "test.properties";


	@Test
	public void testSingleConfigLocation() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(FQ_SIMPLE_CONTEXT);
		assertTrue(ctx.containsBean("someMessageSource"));
		ctx.close();
	}

	@Test
	public void testMultipleConfigLocations() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				FQ_CONTEXT_B, FQ_CONTEXT_C, FQ_CONTEXT_A);
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));

		// re-refresh (after construction refresh)
		Service service = (Service) ctx.getBean("service");
		ctx.refresh();
		assertTrue(service.isProperlyDestroyed());

		// regular close call
		service = (Service) ctx.getBean("service");
		ctx.close();
		assertTrue(service.isProperlyDestroyed());

		// re-activating and re-closing the context (SPR-13425)
		ctx.refresh();
		service = (Service) ctx.getBean("service");
		ctx.close();
		assertTrue(service.isProperlyDestroyed());
	}

	@Test
	public void testConfigLocationPattern() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		Service service = (Service) ctx.getBean("service");
		ctx.close();
		assertTrue(service.isProperlyDestroyed());
	}

	@Test
	public void testSingleConfigLocationWithClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(SIMPLE_CONTEXT, getClass());
		assertTrue(ctx.containsBean("someMessageSource"));
		ctx.close();
	}

	@Test
	public void testAliasWithPlaceholder() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				FQ_CONTEXT_B, FQ_ALIASED_CONTEXT_C, FQ_CONTEXT_A);
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.refresh();
	}

	@Test
	public void testContextWithInvalidValueType() throws IOException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {INVALID_VALUE_TYPE_CONTEXT}, false);
		try {
			context.refresh();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(TypeMismatchException.class));
			assertTrue(ex.toString().contains("someMessageSource"));
			assertTrue(ex.toString().contains("useCodeAsDefaultMessage"));
			checkExceptionFromInvalidValueType(ex);
			checkExceptionFromInvalidValueType(new ExceptionInInitializerError(ex));
			assertFalse(context.isActive());
		}
	}

	private void checkExceptionFromInvalidValueType(Throwable ex) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ex.printStackTrace(new PrintStream(baos));
		String dump = FileCopyUtils.copyToString(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
		assertTrue(dump.contains("someMessageSource"));
		assertTrue(dump.contains("useCodeAsDefaultMessage"));
	}

	@Test
	public void testContextWithInvalidLazyClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(INVALID_CLASS_CONTEXT, getClass());
		assertTrue(ctx.containsBean("someMessageSource"));
		try {
			ctx.getBean("someMessageSource");
			fail("Should have thrown CannotLoadBeanClassException");
		}
		catch (CannotLoadBeanClassException ex) {
			assertTrue(ex.contains(ClassNotFoundException.class));
		}
		ctx.close();
	}

	@Test
	public void testContextWithClassNameThatContainsPlaceholder() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CLASS_WITH_PLACEHOLDER_CONTEXT, getClass());
		assertTrue(ctx.containsBean("someMessageSource"));
		assertTrue(ctx.getBean("someMessageSource") instanceof StaticMessageSource);
		ctx.close();
	}

	@Test
	public void testMultipleConfigLocationsWithClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] {CONTEXT_B, CONTEXT_C, CONTEXT_A}, getClass());
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

	@Test
	public void testFactoryBeanAndApplicationListener() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		ctx.getBeanFactory().registerSingleton("manualFBAAL", new FactoryBeanAndApplicationListener());
		assertEquals(2, ctx.getBeansOfType(ApplicationListener.class).size());
		ctx.close();
	}

	@Test
	public void testMessageSourceAware() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		MessageSource messageSource = (MessageSource) ctx.getBean("messageSource");
		Service service1 = (Service) ctx.getBean("service");
		assertEquals(ctx, service1.getMessageSource());
		Service service2 = (Service) ctx.getBean("service2");
		assertEquals(ctx, service2.getMessageSource());
		AutowiredService autowiredService1 = (AutowiredService) ctx.getBean("autowiredService");
		assertEquals(messageSource, autowiredService1.getMessageSource());
		AutowiredService autowiredService2 = (AutowiredService) ctx.getBean("autowiredService2");
		assertEquals(messageSource, autowiredService2.getMessageSource());
		ctx.close();
	}

	@Test
	public void testResourceArrayPropertyEditor() throws IOException {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		Service service = (Service) ctx.getBean("service");
		assertEquals(3, service.getResources().length);
		List<Resource> resources = Arrays.asList(service.getResources());
		assertTrue(resources.contains(new FileSystemResource(new ClassPathResource(FQ_CONTEXT_A).getFile())));
		assertTrue(resources.contains(new FileSystemResource(new ClassPathResource(FQ_CONTEXT_B).getFile())));
		assertTrue(resources.contains(new FileSystemResource(new ClassPathResource(FQ_CONTEXT_C).getFile())));
		ctx.close();
	}

	@Test
	public void testChildWithProxy() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(CONTEXT_WILDCARD);
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {CHILD_WITH_PROXY_CONTEXT}, ctx);
		assertTrue(AopUtils.isAopProxy(child.getBean("assemblerOne")));
		assertTrue(AopUtils.isAopProxy(child.getBean("assemblerTwo")));
		ctx.close();
	}

	@Test
	public void testAliasForParentContext() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(FQ_SIMPLE_CONTEXT);
		assertTrue(ctx.containsBean("someMessageSource"));

		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {ALIAS_FOR_PARENT_CONTEXT}, ctx);
		assertTrue(child.containsBean("someMessageSource"));
		assertTrue(child.containsBean("yourMessageSource"));
		assertTrue(child.containsBean("myMessageSource"));
		assertTrue(child.isSingleton("someMessageSource"));
		assertTrue(child.isSingleton("yourMessageSource"));
		assertTrue(child.isSingleton("myMessageSource"));
		assertEquals(StaticMessageSource.class, child.getType("someMessageSource"));
		assertEquals(StaticMessageSource.class, child.getType("yourMessageSource"));
		assertEquals(StaticMessageSource.class, child.getType("myMessageSource"));

		Object someMs = child.getBean("someMessageSource");
		Object yourMs = child.getBean("yourMessageSource");
		Object myMs = child.getBean("myMessageSource");
		assertSame(someMs, yourMs);
		assertSame(someMs, myMs);

		String[] aliases = child.getAliases("someMessageSource");
		assertEquals(2, aliases.length);
		assertEquals("myMessageSource", aliases[0]);
		assertEquals("yourMessageSource", aliases[1]);
		aliases = child.getAliases("myMessageSource");
		assertEquals(2, aliases.length);
		assertEquals("someMessageSource", aliases[0]);
		assertEquals("yourMessageSource", aliases[1]);

		child.close();
		ctx.close();
	}

	@Test
	public void testAliasThatOverridesParent() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(FQ_SIMPLE_CONTEXT);
		Object someMs = ctx.getBean("someMessageSource");

		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {ALIAS_THAT_OVERRIDES_PARENT_CONTEXT}, ctx);
		Object myMs = child.getBean("myMessageSource");
		Object someMs2 = child.getBean("someMessageSource");
		assertSame(myMs, someMs2);
		assertNotSame(someMs, someMs2);
		assertOneMessageSourceOnly(child, myMs);
	}

	@Test
	public void testAliasThatOverridesEarlierBean() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				FQ_SIMPLE_CONTEXT, ALIAS_THAT_OVERRIDES_PARENT_CONTEXT);
		Object myMs = ctx.getBean("myMessageSource");
		Object someMs2 = ctx.getBean("someMessageSource");
		assertSame(myMs, someMs2);
		assertOneMessageSourceOnly(ctx, myMs);
	}

	private void assertOneMessageSourceOnly(ClassPathXmlApplicationContext ctx, Object myMessageSource) {
		String[] beanNamesForType = ctx.getBeanNamesForType(StaticMessageSource.class);
		assertEquals(1, beanNamesForType.length);
		assertEquals("myMessageSource", beanNamesForType[0]);
		beanNamesForType = ctx.getBeanNamesForType(StaticMessageSource.class, true, true);
		assertEquals(1, beanNamesForType.length);
		assertEquals("myMessageSource", beanNamesForType[0]);
		beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(ctx, StaticMessageSource.class);
		assertEquals(1, beanNamesForType.length);
		assertEquals("myMessageSource", beanNamesForType[0]);
		beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(ctx, StaticMessageSource.class, true, true);
		assertEquals(1, beanNamesForType.length);
		assertEquals("myMessageSource", beanNamesForType[0]);

		Map<?, StaticMessageSource> beansOfType = ctx.getBeansOfType(StaticMessageSource.class);
		assertEquals(1, beansOfType.size());
		assertSame(myMessageSource, beansOfType.values().iterator().next());
		beansOfType = ctx.getBeansOfType(StaticMessageSource.class, true, true);
		assertEquals(1, beansOfType.size());
		assertSame(myMessageSource, beansOfType.values().iterator().next());
		beansOfType = BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx, StaticMessageSource.class);
		assertEquals(1, beansOfType.size());
		assertSame(myMessageSource, beansOfType.values().iterator().next());
		beansOfType = BeanFactoryUtils.beansOfTypeIncludingAncestors(ctx, StaticMessageSource.class, true, true);
		assertEquals(1, beansOfType.size());
		assertSame(myMessageSource, beansOfType.values().iterator().next());
	}

	@Test
	public void testResourceAndInputStream() throws IOException {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(RESOURCE_CONTEXT) {
			@Override
			public Resource getResource(String location) {
				if (TEST_PROPERTIES.equals(location)) {
					return new ClassPathResource(TEST_PROPERTIES, ClassPathXmlApplicationContextTests.class);
				}
				return super.getResource(location);
			}
		};
		ResourceTestBean resource1 = (ResourceTestBean) ctx.getBean("resource1");
		ResourceTestBean resource2 = (ResourceTestBean) ctx.getBean("resource2");
		assertTrue(resource1.getResource() instanceof ClassPathResource);
		StringWriter writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getResource().getInputStream()), writer);
		assertEquals("contexttest", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource1.getInputStream()), writer);
		assertEquals("test", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getResource().getInputStream()), writer);
		assertEquals("contexttest", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getInputStream()), writer);
		assertEquals("test", writer.toString());
		ctx.close();
	}

	@Test
	public void testGenericApplicationContextWithXmlBeanDefinitions() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_B, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_C, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_A, getClass()));
		ctx.refresh();
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

	@Test
	public void testGenericApplicationContextWithXmlBeanDefinitionsAndClassLoaderNull() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setClassLoader(null);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_B, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_C, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_A, getClass()));
		ctx.refresh();
		assertEquals(ObjectUtils.identityToString(ctx), ctx.getId());
		assertEquals(ObjectUtils.identityToString(ctx), ctx.getDisplayName());
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

	@Test
	public void testGenericApplicationContextWithXmlBeanDefinitionsAndSpecifiedId() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setId("testContext");
		ctx.setDisplayName("Test Context");
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_B, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_C, getClass()));
		reader.loadBeanDefinitions(new ClassPathResource(CONTEXT_A, getClass()));
		ctx.refresh();
		assertEquals("testContext", ctx.getId());
		assertEquals("Test Context", ctx.getDisplayName());
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

}
