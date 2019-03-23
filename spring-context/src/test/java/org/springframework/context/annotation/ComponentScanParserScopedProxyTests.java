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

package org.springframework.context.annotation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.context.SimpleMapScope;
import org.springframework.util.SerializationTestUtils;

import example.scannable.FooService;
import example.scannable.ScopedProxyTestBean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ComponentScanParserScopedProxyTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void testDefaultScopedProxy() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/scopedProxyDefaultTests.xml");
		context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

		ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
		// should not be a proxy
		assertFalse(AopUtils.isAopProxy(bean));
		context.close();
	}

	@Test
	public void testNoScopedProxy() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/scopedProxyNoTests.xml");
		context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

		ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
		// should not be a proxy
		assertFalse(AopUtils.isAopProxy(bean));
		context.close();
	}

	@Test
	public void testInterfacesScopedProxy() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/scopedProxyInterfacesTests.xml");
		context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

		// should cast to the interface
		FooService bean = (FooService) context.getBean("scopedProxyTestBean");
		// should be dynamic proxy
		assertTrue(AopUtils.isJdkDynamicProxy(bean));
		// test serializability
		assertEquals("bar", bean.foo(1));
		FooService deserialized = (FooService) SerializationTestUtils.serializeAndDeserialize(bean);
		assertNotNull(deserialized);
		assertEquals("bar", deserialized.foo(1));
		context.close();
	}

	@Test
	public void testTargetClassScopedProxy() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/scopedProxyTargetClassTests.xml");
		context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

		ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
		// should be a class-based proxy
		assertTrue(AopUtils.isCglibProxy(bean));
		// test serializability
		assertEquals("bar", bean.foo(1));
		ScopedProxyTestBean deserialized = (ScopedProxyTestBean) SerializationTestUtils.serializeAndDeserialize(bean);
		assertNotNull(deserialized);
		assertEquals("bar", deserialized.foo(1));
		context.close();
	}

	@Test
	@SuppressWarnings("resource")
	public void testInvalidConfigScopedProxy() throws Exception {
		exception.expect(BeanDefinitionParsingException.class);
		exception.expectMessage(containsString("Cannot define both 'scope-resolver' and 'scoped-proxy' on <component-scan> tag"));
		exception.expectMessage(containsString("Offending resource: class path resource [org/springframework/context/annotation/scopedProxyInvalidConfigTests.xml]"));

		new ClassPathXmlApplicationContext("org/springframework/context/annotation/scopedProxyInvalidConfigTests.xml");
	}

}
