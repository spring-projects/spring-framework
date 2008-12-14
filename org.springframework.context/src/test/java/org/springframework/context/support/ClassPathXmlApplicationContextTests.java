/*
 * Copyright 2002-2008 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ResourceTestBean;
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
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Juergen Hoeller
 */
public class ClassPathXmlApplicationContextTests extends TestCase {

	public void testSingleConfigLocation() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/simpleContext.xml");
		assertTrue(ctx.containsBean("someMessageSource"));
		ctx.close();
	}

	public void testMultipleConfigLocations() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] {
					"/org/springframework/context/support/test/contextB.xml",
					"/org/springframework/context/support/test/contextC.xml",
					"/org/springframework/context/support/test/contextA.xml"});
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		Service service = (Service) ctx.getBean("service");
		ctx.refresh();
		assertTrue(service.isProperlyDestroyed());
		service = (Service) ctx.getBean("service");
		ctx.close();
		assertTrue(service.isProperlyDestroyed());
	}

	public void testConfigLocationPattern() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/test/context*.xml");
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		Service service = (Service) ctx.getBean("service");
		ctx.close();
		assertTrue(service.isProperlyDestroyed());
	}

	public void testSingleConfigLocationWithClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"simpleContext.xml", getClass());
		assertTrue(ctx.containsBean("someMessageSource"));
		ctx.close();
	}

	public void testAliasWithPlaceholder() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] {
					"/org/springframework/context/support/test/contextB.xml",
					"/org/springframework/context/support/test/aliased-contextC.xml",
					"/org/springframework/context/support/test/contextA.xml"});
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.refresh();
	}

	public void testContextWithInvalidValueType() throws IOException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"/org/springframework/context/support/invalidValueType.xml"}, false);
		try {
			context.refresh();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(TypeMismatchException.class));
			assertTrue(ex.toString().indexOf("someMessageSource") != -1);
			assertTrue(ex.toString().indexOf("useCodeAsDefaultMessage") != -1);
			checkExceptionFromInvalidValueType(ex);
			checkExceptionFromInvalidValueType(new ExceptionInInitializerError(ex));
			assertFalse(context.isActive());
		}
	}

	private void checkExceptionFromInvalidValueType(Throwable ex) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ex.printStackTrace(new PrintStream(baos));
		String dump = FileCopyUtils.copyToString(
				new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
		assertTrue(dump.indexOf("someMessageSource") != -1);
		assertTrue(dump.indexOf("useCodeAsDefaultMessage") != -1);
	}

	public void testContextWithInvalidLazyClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"invalidClass.xml", getClass());
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

	public void testContextWithClassNameThatContainsPlaceholder() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"classWithPlaceholder.xml", getClass());
		assertTrue(ctx.containsBean("someMessageSource"));
		assertTrue(ctx.getBean("someMessageSource") instanceof StaticMessageSource);
		ctx.close();
	}

	public void testMultipleConfigLocationsWithClass() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] {"test/contextB.xml", "test/contextC.xml", "test/contextA.xml"}, getClass());
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

	public void testFactoryBeanAndApplicationListener() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/test/context*.xml");
		ctx.getBeanFactory().registerSingleton("manualFBAAL", new FactoryBeanAndApplicationListener());
		assertEquals(2, ctx.getBeansOfType(ApplicationListener.class).size());
		ctx.close();
	}

	public void testMessageSourceAware() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/test/context*.xml");
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

	public void testResourceArrayPropertyEditor() throws IOException {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/test/context*.xml");
		Service service = (Service) ctx.getBean("service");
		assertEquals(3, service.getResources().length);
		List resources = Arrays.asList(service.getResources());
		assertTrue(resources.contains(
		    new FileSystemResource(new ClassPathResource("/org/springframework/context/support/test/contextA.xml").getFile())));
		assertTrue(resources.contains(
		    new FileSystemResource(new ClassPathResource("/org/springframework/context/support/test/contextB.xml").getFile())));
		assertTrue(resources.contains(
		    new FileSystemResource(new ClassPathResource("/org/springframework/context/support/test/contextC.xml").getFile())));
		ctx.close();
	}

	public void testChildWithProxy() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/test/context*.xml");
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {"/org/springframework/context/support/childWithProxy.xml"}, ctx);
		assertTrue(AopUtils.isAopProxy(child.getBean("assemblerOne")));
		assertTrue(AopUtils.isAopProxy(child.getBean("assemblerTwo")));
		ctx.close();
	}

	public void testAliasForParentContext() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/simpleContext.xml");
		assertTrue(ctx.containsBean("someMessageSource"));

		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {"/org/springframework/context/support/aliasForParent.xml"}, ctx);
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

	public void testAliasThatOverridesParent() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/context/support/simpleContext.xml");
		Object someMs = ctx.getBean("someMessageSource");

		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(
				new String[] {"/org/springframework/context/support/aliasThatOverridesParent.xml"}, ctx);
		Object myMs = child.getBean("myMessageSource");
		Object someMs2 = child.getBean("someMessageSource");
		assertSame(myMs, someMs2);
		assertNotSame(someMs, someMs2);
		assertOneMessageSourceOnly(child, myMs);
	}

	public void testAliasThatOverridesEarlierBean() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				new String[] {"/org/springframework/context/support/simpleContext.xml",
											"/org/springframework/context/support/aliasThatOverridesParent.xml"});
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

		Map beansOfType = ctx.getBeansOfType(StaticMessageSource.class);
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

	public void testResourceAndInputStream() throws IOException {
		ClassPathXmlApplicationContext ctx =
		    new ClassPathXmlApplicationContext("/org/springframework/beans/factory/xml/resource.xml") {
			public Resource getResource(String location) {
				if ("classpath:org/springframework/beans/factory/xml/test.properties".equals(location)) {
					return new ClassPathResource("test.properties", ClassPathXmlApplicationContextTests.class);
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
		assertEquals("contexttest", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getResource().getInputStream()), writer);
		assertEquals("contexttest", writer.toString());
		writer = new StringWriter();
		FileCopyUtils.copy(new InputStreamReader(resource2.getInputStream()), writer);
		assertEquals("contexttest", writer.toString());
		ctx.close();
	}

	public void testGenericApplicationContextWithXmlBeanDefinitions() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource("test/contextB.xml", getClass()));
		reader.loadBeanDefinitions(new ClassPathResource("test/contextC.xml", getClass()));
		reader.loadBeanDefinitions(new ClassPathResource("test/contextA.xml", getClass()));
		ctx.refresh();
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

	public void testGenericApplicationContextWithXmlBeanDefinitionsAndClassLoaderNull() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setClassLoader(null);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource("test/contextB.xml", getClass()));
		reader.loadBeanDefinitions(new ClassPathResource("test/contextC.xml", getClass()));
		reader.loadBeanDefinitions(new ClassPathResource("test/contextA.xml", getClass()));
		ctx.refresh();
		assertEquals(ObjectUtils.identityToString(ctx), ctx.getId());
		assertEquals(ObjectUtils.identityToString(ctx), ctx.getDisplayName());
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

	public void testGenericApplicationContextWithXmlBeanDefinitionsAndSpecifiedId() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setId("testContext");
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
		reader.loadBeanDefinitions(new ClassPathResource("test/contextB.xml", getClass()));
		reader.loadBeanDefinitions(new ClassPathResource("test/contextC.xml", getClass()));
		reader.loadBeanDefinitions(new ClassPathResource("test/contextA.xml", getClass()));
		ctx.refresh();
		assertEquals("testContext", ctx.getId());
		assertEquals("testContext", ctx.getDisplayName());
		assertTrue(ctx.containsBean("service"));
		assertTrue(ctx.containsBean("logicOne"));
		assertTrue(ctx.containsBean("logicTwo"));
		ctx.close();
	}

}
