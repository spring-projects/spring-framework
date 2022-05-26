/*
 * Copyright 2002-2020 the original author or authors.
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

import example.scannable.FooService;
import example.scannable.ScopedProxyTestBean;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.testfixture.SimpleMapScope;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ComponentScanParserScopedProxyTests {

	@Test
	public void testDefaultScopedProxy() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/scopedProxyDefaultTests.xml");
		context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

		ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
		// should not be a proxy
		assertThat(AopUtils.isAopProxy(bean)).isFalse();
		context.close();
	}

	@Test
	public void testNoScopedProxy() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/scopedProxyNoTests.xml");
		context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

		ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
		// should not be a proxy
		assertThat(AopUtils.isAopProxy(bean)).isFalse();
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
		assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
		// test serializability
		assertThat(bean.foo(1)).isEqualTo("bar");
		FooService deserialized = SerializationTestUtils.serializeAndDeserialize(bean);
		assertThat(deserialized).isNotNull();
		assertThat(deserialized.foo(1)).isEqualTo("bar");
		context.close();
	}

	@Test
	public void testTargetClassScopedProxy() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/context/annotation/scopedProxyTargetClassTests.xml");
		context.getBeanFactory().registerScope("myScope", new SimpleMapScope());

		ScopedProxyTestBean bean = (ScopedProxyTestBean) context.getBean("scopedProxyTestBean");
		// should be a class-based proxy
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();
		// test serializability
		assertThat(bean.foo(1)).isEqualTo("bar");
		ScopedProxyTestBean deserialized = SerializationTestUtils.serializeAndDeserialize(bean);
		assertThat(deserialized).isNotNull();
		assertThat(deserialized.foo(1)).isEqualTo("bar");
		context.close();
	}

	@Test
	@SuppressWarnings("resource")
	public void testInvalidConfigScopedProxy() throws Exception {
		assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext("org/springframework/context/annotation/scopedProxyInvalidConfigTests.xml"))
			.withMessageContaining("Cannot define both 'scope-resolver' and 'scoped-proxy' on <component-scan> tag")
			.withMessageContaining("Offending resource: class path resource [org/springframework/context/annotation/scopedProxyInvalidConfigTests.xml]");
	}

}
