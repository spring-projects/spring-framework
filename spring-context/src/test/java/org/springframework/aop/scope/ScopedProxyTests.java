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

package org.springframework.aop.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.SimpleMapScope;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.SerializationTestUtils;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ScopedProxyTests {

	private static final Class<?> CLASS = ScopedProxyTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final ClassPathResource LIST_CONTEXT = new ClassPathResource(CLASSNAME + "-list.xml", CLASS);
	private static final ClassPathResource MAP_CONTEXT = new ClassPathResource(CLASSNAME + "-map.xml", CLASS);
	private static final ClassPathResource OVERRIDE_CONTEXT = new ClassPathResource(CLASSNAME + "-override.xml", CLASS);
	private static final ClassPathResource TESTBEAN_CONTEXT = new ClassPathResource(CLASSNAME + "-testbean.xml", CLASS);

	/* SPR-2108 */
	@Test
	public void testProxyAssignable() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(MAP_CONTEXT);
		Object baseMap = bf.getBean("singletonMap");
		assertTrue(baseMap instanceof Map);
	}

	@Test
	public void testSimpleProxy() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(MAP_CONTEXT);
		Object simpleMap = bf.getBean("simpleMap");
		assertTrue(simpleMap instanceof Map);
		assertTrue(simpleMap instanceof HashMap);
	}

	@Test
	public void testScopedOverride() throws Exception {
		GenericApplicationContext ctx = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(OVERRIDE_CONTEXT);
		SimpleMapScope scope = new SimpleMapScope();
		ctx.getBeanFactory().registerScope("request", scope);
		ctx.refresh();

		ITestBean bean = (ITestBean) ctx.getBean("testBean");
		assertEquals("male", bean.getName());
		assertEquals(99, bean.getAge());

		assertTrue(scope.getMap().containsKey("scopedTarget.testBean"));
		assertEquals(TestBean.class, scope.getMap().get("scopedTarget.testBean").getClass());
	}

	@Test
	public void testJdkScopedProxy() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(TESTBEAN_CONTEXT);
		bf.setSerializationId("X");
		SimpleMapScope scope = new SimpleMapScope();
		bf.registerScope("request", scope);

		ITestBean bean = (ITestBean) bf.getBean("testBean");
		assertNotNull(bean);
		assertTrue(AopUtils.isJdkDynamicProxy(bean));
		assertTrue(bean instanceof ScopedObject);
		ScopedObject scoped = (ScopedObject) bean;
		assertEquals(TestBean.class, scoped.getTargetObject().getClass());
		bean.setAge(101);

		assertTrue(scope.getMap().containsKey("testBeanTarget"));
		assertEquals(TestBean.class, scope.getMap().get("testBeanTarget").getClass());

		ITestBean deserialized = (ITestBean) SerializationTestUtils.serializeAndDeserialize(bean);
		assertNotNull(deserialized);
		assertTrue(AopUtils.isJdkDynamicProxy(deserialized));
		assertEquals(101, bean.getAge());
		assertTrue(deserialized instanceof ScopedObject);
		ScopedObject scopedDeserialized = (ScopedObject) deserialized;
		assertEquals(TestBean.class, scopedDeserialized.getTargetObject().getClass());

		bf.setSerializationId(null);
	}

	@Test
	public void testCglibScopedProxy() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(LIST_CONTEXT);
		bf.setSerializationId("Y");
		SimpleMapScope scope = new SimpleMapScope();
		bf.registerScope("request", scope);

		TestBean tb = (TestBean) bf.getBean("testBean");
		assertTrue(AopUtils.isCglibProxy(tb.getFriends()));
		assertTrue(tb.getFriends() instanceof ScopedObject);
		ScopedObject scoped = (ScopedObject) tb.getFriends();
		assertEquals(ArrayList.class, scoped.getTargetObject().getClass());
		tb.getFriends().add("myFriend");

		assertTrue(scope.getMap().containsKey("scopedTarget.scopedList"));
		assertEquals(ArrayList.class, scope.getMap().get("scopedTarget.scopedList").getClass());

		ArrayList deserialized = (ArrayList) SerializationTestUtils.serializeAndDeserialize(tb.getFriends());
		assertNotNull(deserialized);
		assertTrue(AopUtils.isCglibProxy(deserialized));
		assertTrue(deserialized.contains("myFriend"));
		assertTrue(deserialized instanceof ScopedObject);
		ScopedObject scopedDeserialized = (ScopedObject) deserialized;
		assertEquals(ArrayList.class, scopedDeserialized.getTargetObject().getClass());

		bf.setSerializationId(null);
	}

}
