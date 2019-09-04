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

package org.springframework.context.annotation.scope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.context.annotation.ScopedProxyMode.DEFAULT;
import static org.springframework.context.annotation.ScopedProxyMode.INTERFACES;
import static org.springframework.context.annotation.ScopedProxyMode.NO;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class ClassPathBeanDefinitionScannerScopeIntegrationTests {

	private static final String DEFAULT_NAME = "default";
	private static final String MODIFIED_NAME = "modified";

	private ServletRequestAttributes oldRequestAttributes = new ServletRequestAttributes(new MockHttpServletRequest());
	private ServletRequestAttributes newRequestAttributes = new ServletRequestAttributes(new MockHttpServletRequest());

	private ServletRequestAttributes oldRequestAttributesWithSession;
	private ServletRequestAttributes newRequestAttributesWithSession;


	@BeforeEach
	void setup() {
		MockHttpServletRequest oldRequestWithSession = new MockHttpServletRequest();
		oldRequestWithSession.setSession(new MockHttpSession());
		this.oldRequestAttributesWithSession = new ServletRequestAttributes(oldRequestWithSession);

		MockHttpServletRequest newRequestWithSession = new MockHttpServletRequest();
		newRequestWithSession.setSession(new MockHttpSession());
		this.newRequestAttributesWithSession = new ServletRequestAttributes(newRequestWithSession);
	}

	@AfterEach
	void reset() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	void singletonScopeWithNoProxy() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(NO);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

		// should not be a proxy
		assertThat(AopUtils.isAopProxy(bean)).isFalse();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

		// singleton bean, so name should be modified even after lookup
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
		assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
	}

	@Test
	void singletonScopeIgnoresProxyInterfaces() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(INTERFACES);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

		// should not be a proxy
		assertThat(AopUtils.isAopProxy(bean)).isFalse();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

		// singleton bean, so name should be modified even after lookup
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
		assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
	}

	@Test
	void singletonScopeIgnoresProxyTargetClass() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(TARGET_CLASS);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

		// should not be a proxy
		assertThat(AopUtils.isAopProxy(bean)).isFalse();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

		// singleton bean, so name should be modified even after lookup
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
		assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
	}

	@Test
	void requestScopeWithNoProxy() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(NO);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("request");

		// should not be a proxy
		assertThat(AopUtils.isAopProxy(bean)).isFalse();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

		// but a newly retrieved bean should have the default name
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("request");
		assertThat(bean2.getName()).isEqualTo(DEFAULT_NAME);
	}

	@Test
	void requestScopeWithProxiedInterfaces() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(INTERFACES);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

		// should be dynamic proxy, implementing both interfaces
		assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
		boolean condition = bean instanceof AnotherScopeTestInterface;
		assertThat(condition).isTrue();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// this is a proxy so it should be reset to default
		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
	}

	@Test
	void requestScopeWithProxiedTargetClass() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(TARGET_CLASS);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

		// should be a class-based proxy
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();
		boolean condition = bean instanceof RequestScopedTestBean;
		assertThat(condition).isTrue();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// this is a proxy so it should be reset to default
		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
	}

	@Test
	void sessionScopeWithNoProxy() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		ApplicationContext context = createContext(NO);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("session");

		// should not be a proxy
		assertThat(AopUtils.isAopProxy(bean)).isFalse();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributesWithSession);
		// not a proxy so this should not have changed
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);

		// but a newly retrieved bean should have the default name
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("session");
		assertThat(bean2.getName()).isEqualTo(DEFAULT_NAME);
	}

	@Test
	void sessionScopeWithProxiedInterfaces() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		ApplicationContext context = createContext(INTERFACES);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("session");

		// should be dynamic proxy, implementing both interfaces
		assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
		boolean condition = bean instanceof AnotherScopeTestInterface;
		assertThat(condition).isTrue();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributesWithSession);
		// this is a proxy so it should be reset to default
		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
		assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
		bean2.setName(DEFAULT_NAME);
		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
	}

	@Test
	void sessionScopeWithProxiedTargetClass() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		ApplicationContext context = createContext(TARGET_CLASS);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("session");

		// should be a class-based proxy
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();
		boolean condition1 = bean instanceof ScopedTestBean;
		assertThat(condition1).isTrue();
		boolean condition = bean instanceof SessionScopedTestBean;
		assertThat(condition).isTrue();

		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributesWithSession);
		// this is a proxy so it should be reset to default
		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);
		bean.setName(MODIFIED_NAME);

		IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
		assertThat(bean2.getName()).isEqualTo(MODIFIED_NAME);
		bean2.setName(DEFAULT_NAME);
		assertThat(bean.getName()).isEqualTo(DEFAULT_NAME);

		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		assertThat(bean.getName()).isEqualTo(MODIFIED_NAME);
	}


	private ApplicationContext createContext(ScopedProxyMode scopedProxyMode) {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		scanner.setBeanNameGenerator((definition, registry) -> definition.getScope());
		scanner.setScopedProxyMode(scopedProxyMode);

		// Scan twice in order to find errors in the bean definition compatibility check.
		scanner.scan(getClass().getPackage().getName());
		scanner.scan(getClass().getPackage().getName());

		context.refresh();
		return context;
	}


	interface IScopedTestBean {

		String getName();

		void setName(String name);
	}


	static abstract class ScopedTestBean implements IScopedTestBean {

		private String name = DEFAULT_NAME;

		@Override
		public String getName() { return this.name; }

		@Override
		public void setName(String name) { this.name = name; }
	}


	@Component
	static class SingletonScopedTestBean extends ScopedTestBean {
	}


	interface AnotherScopeTestInterface {
	}


	@Component
	@RequestScope(proxyMode = DEFAULT)
	static class RequestScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
	}


	@Component
	@SessionScope(proxyMode = DEFAULT)
	static class SessionScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
	}

}
