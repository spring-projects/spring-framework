/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.testfixture.SimpleMapScope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class ScopedProxyTests {

	private static final Class<?> CLASS = ScopedProxyTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final ClassPathResource LIST_CONTEXT = new ClassPathResource(CLASSNAME + "-list.xml", CLASS);
	private static final ClassPathResource MAP_CONTEXT = new ClassPathResource(CLASSNAME + "-map.xml", CLASS);
	private static final ClassPathResource OVERRIDE_CONTEXT = new ClassPathResource(CLASSNAME + "-override.xml", CLASS);
	private static final ClassPathResource TESTBEAN_CONTEXT = new ClassPathResource(CLASSNAME + "-testbean.xml", CLASS);


	@Test  // SPR-2108
	void testProxyAssignable() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(MAP_CONTEXT);
		Object baseMap = bf.getBean("singletonMap");
		assertThat(baseMap instanceof Map).isTrue();
	}

	@Test
	void testSimpleProxy() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(MAP_CONTEXT);
		Object simpleMap = bf.getBean("simpleMap");
		assertThat(simpleMap instanceof Map).isTrue();
		assertThat(simpleMap instanceof HashMap).isTrue();
	}

	@Test
	void testScopedOverride() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(OVERRIDE_CONTEXT);
		SimpleMapScope scope = new SimpleMapScope();
		ctx.getBeanFactory().registerScope("request", scope);
		ctx.refresh();

		ITestBean bean = (ITestBean) ctx.getBean("testBean");
		assertThat(bean.getName()).isEqualTo("male");
		assertThat(bean.getAge()).isEqualTo(99);

		assertThat(scope.getMap().containsKey("scopedTarget.testBean")).isTrue();
		assertThat(scope.getMap().get("scopedTarget.testBean").getClass()).isEqualTo(TestBean.class);
	}

	@Test
	void testJdkScopedProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(TESTBEAN_CONTEXT);
		bf.setSerializationId("X");
		SimpleMapScope scope = new SimpleMapScope();
		bf.registerScope("request", scope);

		ITestBean bean = (ITestBean) bf.getBean("testBean");
		assertThat(bean).isNotNull();
		assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
		assertThat(bean instanceof ScopedObject).isTrue();
		ScopedObject scoped = (ScopedObject) bean;
		assertThat(scoped.getTargetObject().getClass()).isEqualTo(TestBean.class);
		bean.setAge(101);

		assertThat(scope.getMap().containsKey("testBeanTarget")).isTrue();
		assertThat(scope.getMap().get("testBeanTarget").getClass()).isEqualTo(TestBean.class);

		ITestBean deserialized = SerializationTestUtils.serializeAndDeserialize(bean);
		assertThat(deserialized).isNotNull();
		assertThat(AopUtils.isJdkDynamicProxy(deserialized)).isTrue();
		assertThat(bean.getAge()).isEqualTo(101);
		assertThat(deserialized instanceof ScopedObject).isTrue();
		ScopedObject scopedDeserialized = (ScopedObject) deserialized;
		assertThat(scopedDeserialized.getTargetObject().getClass()).isEqualTo(TestBean.class);

		bf.setSerializationId(null);
	}

	@Test
	void testCglibScopedProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(LIST_CONTEXT);
		bf.setSerializationId("Y");
		SimpleMapScope scope = new SimpleMapScope();
		bf.registerScope("request", scope);

		TestBean tb = (TestBean) bf.getBean("testBean");
		assertThat(AopUtils.isCglibProxy(tb.getFriends())).isTrue();
		assertThat(tb.getFriends() instanceof ScopedObject).isTrue();
		ScopedObject scoped = (ScopedObject) tb.getFriends();
		assertThat(scoped.getTargetObject().getClass()).isEqualTo(ArrayList.class);
		tb.getFriends().add("myFriend");

		assertThat(scope.getMap().containsKey("scopedTarget.scopedList")).isTrue();
		assertThat(scope.getMap().get("scopedTarget.scopedList").getClass()).isEqualTo(ArrayList.class);

		ArrayList<Object> deserialized = (ArrayList<Object>) SerializationTestUtils.serializeAndDeserialize(tb.getFriends());
		assertThat(deserialized).isNotNull();
		assertThat(AopUtils.isCglibProxy(deserialized)).isTrue();
		assertThat(deserialized).contains("myFriend");
		assertThat(deserialized instanceof ScopedObject).isTrue();
		ScopedObject scopedDeserialized = (ScopedObject) deserialized;
		assertThat(scopedDeserialized.getTargetObject().getClass()).isEqualTo(ArrayList.class);

		bf.setSerializationId(null);
	}

}
