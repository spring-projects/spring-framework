/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.context.request;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see RequestScopeTests
 */
public class SessionScopeTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	public void setup() throws Exception {
		this.beanFactory.registerScope("session", new SessionScope());
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("sessionScopeTests.xml", getClass()));
	}

	@AfterEach
	public void resetRequestAttributes() {
		RequestContextHolder.setRequestAttributes(null);
	}


	@Test
	public void getFromScope() throws Exception {
		AtomicInteger count = new AtomicInteger();
		MockHttpSession session = new MockHttpSession() {
			@Override
			public void setAttribute(String name, Object value) {
				super.setAttribute(name, value);
				count.incrementAndGet();
			}
		};
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		String name = "sessionScopedObject";
		assertThat(session.getAttribute(name)).isNull();
		TestBean bean = (TestBean) this.beanFactory.getBean(name);
		assertThat(count.get()).isEqualTo(1);
		assertThat(bean).isEqualTo(session.getAttribute(name));
		assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
		assertThat(count.get()).isEqualTo(1);

		// should re-propagate updated attribute
		requestAttributes.requestCompleted();
		assertThat(bean).isEqualTo(session.getAttribute(name));
		assertThat(count.get()).isEqualTo(2);
	}

	@Test
	public void getFromScopeWithSingleAccess() throws Exception {
		AtomicInteger count = new AtomicInteger();
		MockHttpSession session = new MockHttpSession() {
			@Override
			public void setAttribute(String name, Object value) {
				super.setAttribute(name, value);
				count.incrementAndGet();
			}
		};
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		String name = "sessionScopedObject";
		assertThat(session.getAttribute(name)).isNull();
		TestBean bean = (TestBean) this.beanFactory.getBean(name);
		assertThat(count.get()).isEqualTo(1);

		// should re-propagate updated attribute
		requestAttributes.requestCompleted();
		assertThat(bean).isEqualTo(session.getAttribute(name));
		assertThat(count.get()).isEqualTo(2);
	}

	@Test
	public void destructionAtSessionTermination() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		String name = "sessionScopedDisposableObject";
		assertThat(session.getAttribute(name)).isNull();
		DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertThat(bean).isEqualTo(session.getAttribute(name));
		assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

		requestAttributes.requestCompleted();
		session.invalidate();
		assertThat(bean.wasDestroyed()).isTrue();
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
		assertThat(session.getAttribute(name)).isNull();
		DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertThat(bean).isEqualTo(session.getAttribute(name));
		assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

		requestAttributes.requestCompleted();
		serializedState = session.serializeState();
		assertThat(bean.wasDestroyed()).isFalse();

		serializedState = SerializationTestUtils.serializeAndDeserialize(serializedState);

		session = new MockHttpSession();
		session.deserializeState(serializedState);
		request = new MockHttpServletRequest();
		request.setSession(session);
		requestAttributes = new ServletRequestAttributes(request);

		RequestContextHolder.setRequestAttributes(requestAttributes);
		name = "sessionScopedDisposableObject";
		assertThat(session.getAttribute(name)).isNotNull();
		bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertThat(bean).isEqualTo(session.getAttribute(name));
		assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

		requestAttributes.requestCompleted();
		session.invalidate();
		assertThat(bean.wasDestroyed()).isTrue();

		if (beanNameReset) {
			assertThat(bean.getBeanName()).isNull();
		}
		else {
			assertThat(bean.getBeanName()).isNotNull();
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

		@Override
		public boolean requiresDestruction(Object bean) {
			return true;
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
			if (bean instanceof BeanNameAware beanNameAware) {
				beanNameAware.setBeanName(null);
			}
		}

		@Override
		public boolean requiresDestruction(Object bean) {
			return true;
		}
	}

}
