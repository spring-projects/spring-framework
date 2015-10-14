/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.context.request;

import java.io.Serializable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpSession;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SerializationTestUtils;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see RequestScopeTests
 */
public class SessionScopeTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@Before
	public void setUp() throws Exception {
		this.beanFactory.registerScope("session", new SessionScope());
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("sessionScopeTests.xml", getClass()));
	}

	@After
	public void resetRequestAttributes() {
		RequestContextHolder.setRequestAttributes(null);
	}

	@Test
	public void getFromScope() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		String name = "sessionScopedObject";
		assertNull(session.getAttribute(name));
		TestBean bean = (TestBean) this.beanFactory.getBean(name);
		assertEquals(session.getAttribute(name), bean);
		assertSame(bean, this.beanFactory.getBean(name));
	}

	@Test
	public void destructionAtSessionTermination() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		String name = "sessionScopedDisposableObject";
		assertNull(session.getAttribute(name));
		DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertEquals(session.getAttribute(name), bean);
		assertSame(bean, this.beanFactory.getBean(name));

		requestAttributes.requestCompleted();
		session.invalidate();
		assertTrue(bean.wasDestroyed());
	}

	@Test
	public void destructionWithSessionSerialization() throws Exception {
		doTestDestructionWithSessionSerialization(false);
	}

	@Test
	public void destructionWithSessionSerializationAndBeanPostProcessor() throws Exception {
		this.beanFactory.addBeanPostProcessor(new CustomDestructionAwareBeanPostProcessor());
		doTestDestructionWithSessionSerialization(false);
	}

	@Test
	public void destructionWithSessionSerializationAndSerializableBeanPostProcessor() throws Exception {
		this.beanFactory.addBeanPostProcessor(new CustomSerializableDestructionAwareBeanPostProcessor());
		doTestDestructionWithSessionSerialization(true);
	}

	private void doTestDestructionWithSessionSerialization(boolean beanNameReset) throws Exception {
		Serializable serializedState = null;

		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		String name = "sessionScopedDisposableObject";
		assertNull(session.getAttribute(name));
		DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertEquals(session.getAttribute(name), bean);
		assertSame(bean, this.beanFactory.getBean(name));

		requestAttributes.requestCompleted();
		serializedState = session.serializeState();
		assertFalse(bean.wasDestroyed());

		serializedState = (Serializable) SerializationTestUtils.serializeAndDeserialize(serializedState);

		session = new MockHttpSession();
		session.deserializeState(serializedState);
		request = new MockHttpServletRequest();
		request.setSession(session);
		requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		name = "sessionScopedDisposableObject";
		assertNotNull(session.getAttribute(name));
		bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertEquals(session.getAttribute(name), bean);
		assertSame(bean, this.beanFactory.getBean(name));

		requestAttributes.requestCompleted();
		session.invalidate();
		assertTrue(bean.wasDestroyed());

		if (beanNameReset) {
			assertNull(bean.getBeanName());
		}
		else {
			assertNotNull(bean.getBeanName());
		}
	}


	private static class CustomDestructionAwareBeanPostProcessor implements DestructionAwareBeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		}
	}


	@SuppressWarnings("serial")
	private static class CustomSerializableDestructionAwareBeanPostProcessor
			implements DestructionAwareBeanPostProcessor, Serializable {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
			if (bean instanceof BeanNameAware) {
				((BeanNameAware) bean).setBeanName(null);
			}
		}
	}

}
