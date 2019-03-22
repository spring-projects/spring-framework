/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @see SessionScopeTests
 */
public class RequestScopeTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@Before
	public void setup() throws Exception {
		this.beanFactory.registerScope("request", new RequestScope());
		this.beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver());
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("requestScopeTests.xml", getClass()));
		this.beanFactory.preInstantiateSingletons();
	}

	@After
	public void resetRequestAttributes() {
		RequestContextHolder.setRequestAttributes(null);
	}


	@Test
	public void getFromScope() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContextPath("/path");
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		String name = "requestScopedObject";
		assertNull(request.getAttribute(name));
		TestBean bean = (TestBean) this.beanFactory.getBean(name);
		assertEquals("/path", bean.getName());
		assertSame(bean, request.getAttribute(name));
		assertSame(bean, this.beanFactory.getBean(name));
	}

	@Test
	public void destructionAtRequestCompletion() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		String name = "requestScopedDisposableObject";
		assertNull(request.getAttribute(name));
		DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertSame(bean, request.getAttribute(name));
		assertSame(bean, this.beanFactory.getBean(name));

		requestAttributes.requestCompleted();
		assertTrue(bean.wasDestroyed());
	}

	@Test
	public void getFromFactoryBeanInScope() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		String name = "requestScopedFactoryBean";
		assertNull(request.getAttribute(name));
		TestBean bean = (TestBean) this.beanFactory.getBean(name);
		assertTrue(request.getAttribute(name) instanceof FactoryBean);
		assertSame(bean, this.beanFactory.getBean(name));
	}

	@Test
	public void circleLeadsToException() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		try {
			String name = "requestScopedObjectCircle1";
			assertNull(request.getAttribute(name));

			this.beanFactory.getBean(name);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(BeanCurrentlyInCreationException.class));
		}
	}

	@Test
	public void innerBeanInheritsContainingBeanScopeByDefault() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		String outerBeanName = "requestScopedOuterBean";
		assertNull(request.getAttribute(outerBeanName));
		TestBean outer1 = (TestBean) this.beanFactory.getBean(outerBeanName);
		assertNotNull(request.getAttribute(outerBeanName));
		TestBean inner1 = (TestBean) outer1.getSpouse();
		assertSame(outer1, this.beanFactory.getBean(outerBeanName));
		requestAttributes.requestCompleted();
		assertTrue(outer1.wasDestroyed());
		assertTrue(inner1.wasDestroyed());
		request = new MockHttpServletRequest();
		requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		TestBean outer2 = (TestBean) this.beanFactory.getBean(outerBeanName);
		assertNotSame(outer1, outer2);
		assertNotSame(inner1, outer2.getSpouse());
	}

	@Test
	public void requestScopedInnerBeanDestroyedWhileContainedBySingleton() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		String outerBeanName = "singletonOuterBean";
		TestBean outer1 = (TestBean) this.beanFactory.getBean(outerBeanName);
		assertNull(request.getAttribute(outerBeanName));
		TestBean inner1 = (TestBean) outer1.getSpouse();
		TestBean outer2 = (TestBean) this.beanFactory.getBean(outerBeanName);
		assertSame(outer1, outer2);
		assertSame(inner1, outer2.getSpouse());
		requestAttributes.requestCompleted();
		assertTrue(inner1.wasDestroyed());
		assertFalse(outer1.wasDestroyed());
	}

}
