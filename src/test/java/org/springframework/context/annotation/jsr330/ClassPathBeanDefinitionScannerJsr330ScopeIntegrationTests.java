/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.annotation.jsr330;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ClassPathBeanDefinitionScannerJsr330ScopeIntegrationTests {

	private static final String DEFAULT_NAME = "default";

	private static final String MODIFIED_NAME = "modified";

	private ServletRequestAttributes oldRequestAttributes;

	private ServletRequestAttributes newRequestAttributes;

	private ServletRequestAttributes oldRequestAttributesWithSession;

	private ServletRequestAttributes newRequestAttributesWithSession;


	@Before
	public void setUp() {
		this.oldRequestAttributes = new ServletRequestAttributes(new MockHttpServletRequest());
		this.newRequestAttributes = new ServletRequestAttributes(new MockHttpServletRequest());

		MockHttpServletRequest oldRequestWithSession = new MockHttpServletRequest();
		oldRequestWithSession.setSession(new MockHttpSession());
		this.oldRequestAttributesWithSession = new ServletRequestAttributes(oldRequestWithSession);

		MockHttpServletRequest newRequestWithSession = new MockHttpServletRequest();
		newRequestWithSession.setSession(new MockHttpSession());
		this.newRequestAttributesWithSession = new ServletRequestAttributes(newRequestWithSession);
	}

	@After
	public void tearDown() throws Exception {
		RequestContextHolder.setRequestAttributes(null);
	}


	@Test
	public void testPrototype() {
		ApplicationContext context = createContext(ScopedProxyMode.NO);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("prototype");
		assertNotNull(bean);
		assertTrue(context.isPrototype("prototype"));
		assertFalse(context.isSingleton("prototype"));
	}

	@Test
	public void testSingletonScopeWithNoProxy() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(ScopedProxyMode.NO);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");
		assertTrue(context.isSingleton("singleton"));
		assertFalse(context.isPrototype("singleton"));

		// should not be a proxy
		assertFalse(AopUtils.isAopProxy(bean));

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertEquals(MODIFIED_NAME, bean.getName());

		// singleton bean, so name should be modified even after lookup
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
		assertEquals(MODIFIED_NAME, bean2.getName());
	}

	@Test
	public void testSingletonScopeIgnoresProxyInterfaces() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

		// should not be a proxy
		assertFalse(AopUtils.isAopProxy(bean));

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertEquals(MODIFIED_NAME, bean.getName());

		// singleton bean, so name should be modified even after lookup
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
		assertEquals(MODIFIED_NAME, bean2.getName());
	}

	@Test
	public void testSingletonScopeIgnoresProxyTargetClass() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("singleton");

		// should not be a proxy
		assertFalse(AopUtils.isAopProxy(bean));

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertEquals(MODIFIED_NAME, bean.getName());

		// singleton bean, so name should be modified even after lookup
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("singleton");
		assertEquals(MODIFIED_NAME, bean2.getName());
	}

	@Test
	public void testRequestScopeWithNoProxy() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(ScopedProxyMode.NO);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("request");

		// should not be a proxy
		assertFalse(AopUtils.isAopProxy(bean));

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// not a proxy so this should not have changed
		assertEquals(MODIFIED_NAME, bean.getName());

		// but a newly retrieved bean should have the default name
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("request");
		assertEquals(DEFAULT_NAME, bean2.getName());
	}

	@Test
	public void testRequestScopeWithProxiedInterfaces() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

		// should be dynamic proxy, implementing both interfaces
		assertTrue(AopUtils.isJdkDynamicProxy(bean));
		assertTrue(bean instanceof AnotherScopeTestInterface);

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// this is a proxy so it should be reset to default
		assertEquals(DEFAULT_NAME, bean.getName());

		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		assertEquals(MODIFIED_NAME, bean.getName());
	}

	@Test
	public void testRequestScopeWithProxiedTargetClass() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("request");

		// should be a class-based proxy
		assertTrue(AopUtils.isCglibProxy(bean));
		assertTrue(bean instanceof RequestScopedTestBean);

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributes);
		// this is a proxy so it should be reset to default
		assertEquals(DEFAULT_NAME, bean.getName());

		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		assertEquals(MODIFIED_NAME, bean.getName());
	}

	@Test
	public void testSessionScopeWithNoProxy() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		ApplicationContext context = createContext(ScopedProxyMode.NO);
		ScopedTestBean bean = (ScopedTestBean) context.getBean("session");

		// should not be a proxy
		assertFalse(AopUtils.isAopProxy(bean));

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributesWithSession);
		// not a proxy so this should not have changed
		assertEquals(MODIFIED_NAME, bean.getName());

		// but a newly retrieved bean should have the default name
		ScopedTestBean bean2 = (ScopedTestBean) context.getBean("session");
		assertEquals(DEFAULT_NAME, bean2.getName());
	}

	@Test
	public void testSessionScopeWithProxiedInterfaces() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		ApplicationContext context = createContext(ScopedProxyMode.INTERFACES);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("session");

		// should be dynamic proxy, implementing both interfaces
		assertTrue(AopUtils.isJdkDynamicProxy(bean));
		assertTrue(bean instanceof AnotherScopeTestInterface);

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributesWithSession);
		// this is a proxy so it should be reset to default
		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
		assertEquals(MODIFIED_NAME, bean2.getName());
		bean2.setName(DEFAULT_NAME);
		assertEquals(DEFAULT_NAME, bean.getName());

		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		assertEquals(MODIFIED_NAME, bean.getName());
	}

	@Test
	public void testSessionScopeWithProxiedTargetClass() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		ApplicationContext context = createContext(ScopedProxyMode.TARGET_CLASS);
		IScopedTestBean bean = (IScopedTestBean) context.getBean("session");

		// should be a class-based proxy
		assertTrue(AopUtils.isCglibProxy(bean));
		assertTrue(bean instanceof ScopedTestBean);
		assertTrue(bean instanceof SessionScopedTestBean);

		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		RequestContextHolder.setRequestAttributes(newRequestAttributesWithSession);
		// this is a proxy so it should be reset to default
		assertEquals(DEFAULT_NAME, bean.getName());
		bean.setName(MODIFIED_NAME);

		IScopedTestBean bean2 = (IScopedTestBean) context.getBean("session");
		assertEquals(MODIFIED_NAME, bean2.getName());
		bean2.setName(DEFAULT_NAME);
		assertEquals(DEFAULT_NAME, bean.getName());

		RequestContextHolder.setRequestAttributes(oldRequestAttributesWithSession);
		assertEquals(MODIFIED_NAME, bean.getName());
	}


	private ApplicationContext createContext(final ScopedProxyMode scopedProxyMode) {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		scanner.setScopeMetadataResolver(new ScopeMetadataResolver() {
			@Override
			public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
				ScopeMetadata metadata = new ScopeMetadata();
				if (definition instanceof AnnotatedBeanDefinition) {
					AnnotatedBeanDefinition annDef = (AnnotatedBeanDefinition) definition;
					for (String type : annDef.getMetadata().getAnnotationTypes()) {
						if (type.equals(javax.inject.Singleton.class.getName())) {
							metadata.setScopeName(BeanDefinition.SCOPE_SINGLETON);
							break;
						}
						else if (annDef.getMetadata().getMetaAnnotationTypes(type).contains(javax.inject.Scope.class.getName())) {
							metadata.setScopeName(type.substring(type.length() - 13, type.length() - 6).toLowerCase());
							metadata.setScopedProxyMode(scopedProxyMode);
							break;
						}
						else if (type.startsWith("javax.inject")) {
							metadata.setScopeName(BeanDefinition.SCOPE_PROTOTYPE);
						}
					}
				}
				return metadata;
			}
		});

		// Scan twice in order to find errors in the bean definition compatibility check.
		scanner.scan(getClass().getPackage().getName());
		scanner.scan(getClass().getPackage().getName());

		context.registerAlias("classPathBeanDefinitionScannerJsr330ScopeIntegrationTests.SessionScopedTestBean", "session");
		context.refresh();
		return context;
	}


	public static interface IScopedTestBean {

		String getName();

		void setName(String name);
	}


	public static abstract class ScopedTestBean implements IScopedTestBean {

		private String name = DEFAULT_NAME;

		@Override
		public String getName() { return this.name; }

		@Override
		public void setName(String name) { this.name = name; }
	}


	@Named("prototype")
	public static class PrototypeScopedTestBean extends ScopedTestBean {
	}


	@Named("singleton")
	@Singleton
	public static class SingletonScopedTestBean extends ScopedTestBean {
	}


	public static interface AnotherScopeTestInterface {
	}


	@Named("request")
	@RequestScoped
	public static class RequestScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
	}


	@Named
	@SessionScoped
	public static class SessionScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
	}


	@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@javax.inject.Scope
	public static @interface RequestScoped {
	}


	@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@javax.inject.Scope
	public static @interface SessionScoped {
	}

}
