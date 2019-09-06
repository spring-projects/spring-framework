/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.factory.DummyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class RequestScopedProxyTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	public void setup() {
		this.beanFactory.registerScope("request", new RequestScope());
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("requestScopedProxyTests.xml", getClass()));
		this.beanFactory.preInstantiateSingletons();
	}


	@Test
	public void testGetFromScope() throws Exception {
		String name = "requestScopedObject";
		TestBean bean = (TestBean) this.beanFactory.getBean(name);
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		try {
			assertThat(request.getAttribute("scopedTarget." + name)).isNull();
			assertThat(bean.getName()).isEqualTo("scoped");
			assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
			TestBean target = (TestBean) request.getAttribute("scopedTarget." + name);
			assertThat(target.getClass()).isEqualTo(TestBean.class);
			assertThat(target.getName()).isEqualTo("scoped");
			assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
			assertThat(target.toString()).isEqualTo(bean.toString());
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testGetFromScopeThroughDynamicProxy() throws Exception {
		String name = "requestScopedProxy";
		ITestBean bean = (ITestBean) this.beanFactory.getBean(name);
		// assertTrue(AopUtils.isJdkDynamicProxy(bean));

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		try {
			assertThat(request.getAttribute("scopedTarget." + name)).isNull();
			assertThat(bean.getName()).isEqualTo("scoped");
			assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
			TestBean target = (TestBean) request.getAttribute("scopedTarget." + name);
			assertThat(target.getClass()).isEqualTo(TestBean.class);
			assertThat(target.getName()).isEqualTo("scoped");
			assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
			assertThat(target.toString()).isEqualTo(bean.toString());
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testDestructionAtRequestCompletion() throws Exception {
		String name = "requestScopedDisposableObject";
		DerivedTestBean bean = (DerivedTestBean) this.beanFactory.getBean(name);
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();

		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		try {
			assertThat(request.getAttribute("scopedTarget." + name)).isNull();
			assertThat(bean.getName()).isEqualTo("scoped");
			assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
			assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(DerivedTestBean.class);
			assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).getName()).isEqualTo("scoped");
			assertThat(this.beanFactory.getBean(name)).isSameAs(bean);

			requestAttributes.requestCompleted();
			assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).wasDestroyed()).isTrue();
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testGetFromFactoryBeanInScope() throws Exception {
		String name = "requestScopedFactoryBean";
		TestBean bean = (TestBean) this.beanFactory.getBean(name);
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		try {
			assertThat(request.getAttribute("scopedTarget." + name)).isNull();
			assertThat(bean.getName()).isEqualTo(DummyFactory.SINGLETON_NAME);
			assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
			assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(DummyFactory.class);
			assertThat(this.beanFactory.getBean(name)).isSameAs(bean);
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testGetInnerBeanFromScope() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("outerBean");
		assertThat(AopUtils.isAopProxy(bean)).isFalse();
		assertThat(AopUtils.isCglibProxy(bean.getSpouse())).isTrue();

		String name = "scopedInnerBean";

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		try {
			assertThat(request.getAttribute("scopedTarget." + name)).isNull();
			assertThat(bean.getSpouse().getName()).isEqualTo("scoped");
			assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
			assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(TestBean.class);
			assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).getName()).isEqualTo("scoped");
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

	@Test
	public void testGetAnonymousInnerBeanFromScope() throws Exception {
		TestBean bean = (TestBean) this.beanFactory.getBean("outerBean");
		assertThat(AopUtils.isAopProxy(bean)).isFalse();
		assertThat(AopUtils.isCglibProxy(bean.getSpouse())).isTrue();

		BeanDefinition beanDef = this.beanFactory.getBeanDefinition("outerBean");
		BeanDefinitionHolder innerBeanDef =
				(BeanDefinitionHolder) beanDef.getPropertyValues().getPropertyValue("spouse").getValue();
		String name = innerBeanDef.getBeanName();

		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		try {
			assertThat(request.getAttribute("scopedTarget." + name)).isNull();
			assertThat(bean.getSpouse().getName()).isEqualTo("scoped");
			assertThat(request.getAttribute("scopedTarget." + name)).isNotNull();
			assertThat(request.getAttribute("scopedTarget." + name).getClass()).isEqualTo(TestBean.class);
			assertThat(((TestBean) request.getAttribute("scopedTarget." + name)).getName()).isEqualTo("scoped");
		}
		finally {
			RequestContextHolder.setRequestAttributes(null);
		}
	}

}
