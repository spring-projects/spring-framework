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

package org.springframework.context.annotation;

import junit.framework.TestCase;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class ClassPathBeanDefinitionScannerScopeTests extends TestCase {

	private static final String DEFAULT_NAME = "default";
	
	private static final String MODIFIED_NAME = "modified";
	
	private ServletRequestAttributes oldRequestAttributes;
	
	private ServletRequestAttributes newRequestAttributes; 
	
	private ServletRequestAttributes oldRequestAttributesWithSession;
	
	private ServletRequestAttributes newRequestAttributesWithSession;
	
	
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

	protected void tearDown() throws Exception {
		RequestContextHolder.setRequestAttributes(null);
	}


	public void testSingletonScopeWithNoProxy() {
		RequestContextHolder.setRequestAttributes(oldRequestAttributes);
		ApplicationContext context = createContext(ScopedProxyMode.NO);
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


	private ApplicationContext createContext(ScopedProxyMode scopedProxyMode) {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ScopeTestComponent.class));
		scanner.setBeanNameGenerator(new BeanNameGenerator() {
			public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
				String beanClassName = ClassUtils.getShortName(definition.getBeanClassName());
				int begin = beanClassName.lastIndexOf('.') + 1;
				int end = beanClassName.lastIndexOf("ScopedTestBean");
				return beanClassName.substring(begin, end).toLowerCase();
			}
		});
		scanner.setScopedProxyMode(scopedProxyMode);

		// Scan twice in order to find errors in the bean definition compatibility check.
		scanner.scan(getClass().getPackage().getName());
		scanner.scan(getClass().getPackage().getName());

		context.refresh();
		return context;
	}
	
	
 	public static @interface ScopeTestComponent {
 	}


 	public static interface IScopedTestBean {

 		String getName();

 		void setName(String name);
 	}
 	

	public static abstract class ScopedTestBean implements IScopedTestBean {

		private String name = DEFAULT_NAME;

		public String getName() { return this.name; }

		public void setName(String name) { this.name = name; }
	}
	

	@ScopeTestComponent
	public static class SingletonScopedTestBean extends ScopedTestBean { 
	}
	

	public static interface AnotherScopeTestInterface {
	}
	

	@Scope("request")
	@ScopeTestComponent
	public static class RequestScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface { 
	}


	@Scope("session")
	@ScopeTestComponent
	public static class SessionScopedTestBean extends ScopedTestBean implements AnotherScopeTestInterface {
	}

}
