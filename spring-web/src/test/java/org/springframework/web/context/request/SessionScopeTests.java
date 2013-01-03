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

package org.springframework.web.context.request;

import java.io.Serializable;

import junit.framework.TestCase;

import org.springframework.beans.BeansException;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpSession;
import org.springframework.util.SerializationTestUtils;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class SessionScopeTests extends TestCase {

	private DefaultListableBeanFactory beanFactory;

	@Override
	protected void setUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory();
		this.beanFactory.registerScope("session", new SessionScope());
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("sessionScopeTests.xml", getClass()));
	}

	public void testGetFromScope() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			String name = "sessionScopedObject";
			assertNull(session.getAttribute(name));
			TestBean bean = (TestBean) this.beanFactory.getBean(name);
			assertEquals(session.getAttribute(name), bean);
			assertSame(bean, this.beanFactory.getBean(name));
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	public void testDestructionAtSessionTermination() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			String name = "sessionScopedDisposableObject";
			assertNull(session.getAttribute(name));
			DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
			assertEquals(session.getAttribute(name), bean);
			assertSame(bean, this.beanFactory.getBean(name));

			requestAttributes.requestCompleted();
			session.invalidate();
			assertTrue(bean.wasDestroyed());
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	public void testDestructionWithSessionSerialization() throws Exception {
		doTestDestructionWithSessionSerialization(false);
	}

	public void testDestructionWithSessionSerializationAndBeanPostProcessor() throws Exception {
		this.beanFactory.addBeanPostProcessor(new CustomDestructionAwareBeanPostProcessor());
		doTestDestructionWithSessionSerialization(false);
	}

	public void testDestructionWithSessionSerializationAndSerializableBeanPostProcessor() throws Exception {
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
		try {
			String name = "sessionScopedDisposableObject";
			assertNull(session.getAttribute(name));
			DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
			assertEquals(session.getAttribute(name), bean);
			assertSame(bean, this.beanFactory.getBean(name));

			requestAttributes.requestCompleted();
			serializedState = session.serializeState();
			assertFalse(bean.wasDestroyed());
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}

		serializedState = (Serializable) SerializationTestUtils.serializeAndDeserialize(serializedState);

		session = new MockHttpSession();
		session.deserializeState(serializedState);
		request = new MockHttpServletRequest();
		request.setSession(session);
		requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		try {
			String name = "sessionScopedDisposableObject";
			assertNotNull(session.getAttribute(name));
			DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
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
		finally {
			RequestContextHolder.setRequestAttributes(null);
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
