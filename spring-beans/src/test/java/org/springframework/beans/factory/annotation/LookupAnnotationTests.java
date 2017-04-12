/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Karl Pietrzak
 * @author Juergen Hoeller
 */
public class LookupAnnotationTests {

	private DefaultListableBeanFactory beanFactory;


	@Before
	public void setUp() {
		beanFactory = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor aabpp = new AutowiredAnnotationBeanPostProcessor();
		aabpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(aabpp);
		beanFactory.registerBeanDefinition("abstractBean", new RootBeanDefinition(AbstractBean.class));
		beanFactory.registerBeanDefinition("beanConsumer", new RootBeanDefinition(BeanConsumer.class));
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		beanFactory.registerBeanDefinition("testBean", tbd);
	}


	@Test
	public void testWithoutConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		Object expected = bean.get();
		assertEquals(TestBean.class, expected.getClass());
		assertSame(bean, beanFactory.getBean(BeanConsumer.class).abstractBean);
	}

	@Test
	public void testWithOverloadedArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		TestBean expected = bean.get("haha");
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
		assertSame(bean, beanFactory.getBean(BeanConsumer.class).abstractBean);
	}

	@Test
	public void testWithOneConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		TestBean expected = bean.getOneArgument("haha");
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
		assertSame(bean, beanFactory.getBean(BeanConsumer.class).abstractBean);
	}

	@Test
	public void testWithTwoConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		TestBean expected = bean.getTwoArguments("haha", 72);
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
		assertEquals(72, expected.getAge());
		assertSame(bean, beanFactory.getBean(BeanConsumer.class).abstractBean);
	}

	@Test
	public void testWithThreeArgsShouldFail() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		try {
			bean.getThreeArguments("name", 1, 2);
			fail("TestBean does not have a three arg constructor so this should not have worked");
		}
		catch (AbstractMethodError ex) {
		}
		assertSame(bean, beanFactory.getBean(BeanConsumer.class).abstractBean);
	}

	@Test
	public void testWithEarlyInjection() {
		AbstractBean bean = beanFactory.getBean("beanConsumer", BeanConsumer.class).abstractBean;
		assertNotNull(bean);
		Object expected = bean.get();
		assertEquals(TestBean.class, expected.getClass());
		assertSame(bean, beanFactory.getBean(BeanConsumer.class).abstractBean);
	}


	public static abstract class AbstractBean {

		@Lookup
		public abstract TestBean get();

		@Lookup
		public abstract TestBean get(String name);  // overloaded

		@Lookup
		public abstract TestBean getOneArgument(String name);

		@Lookup
		public abstract TestBean getTwoArguments(String name, int age);

		public abstract TestBean getThreeArguments(String name, int age, int anotherArg);
	}


	public static class BeanConsumer {

		@Autowired
		AbstractBean abstractBean;
	}

}
