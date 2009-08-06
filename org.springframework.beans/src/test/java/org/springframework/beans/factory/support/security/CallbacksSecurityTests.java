/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.beans.factory.support.security;

import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.Policy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.PropertyPermission;
import java.util.Set;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;

import junit.framework.TestCase;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SecurityContextProvider;
import org.springframework.beans.factory.support.security.support.ConstructorBean;
import org.springframework.beans.factory.support.security.support.CustomCallbackBean;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Security test case. Checks whether the container uses its privileges for its
 * internal work but does not leak them when touching/calling user code.
 * 
 *t The first half of the test case checks that permissions are downgraded when
 * calling user code while the second half that the caller code permission get
 * through and Spring doesn't override the permission stack.
 * 
 * @author Costin Leau
 */
public class CallbacksSecurityTests extends TestCase {

	private XmlBeanFactory beanFactory;
	private SecurityContextProvider provider;

	private static class NonPrivilegedBean {

		private String expectedName;
		public static boolean destroyed = false;

		public NonPrivilegedBean(String expected) {
			this.expectedName = expected;
			checkCurrentContext();
		}

		public void init() {
			checkCurrentContext();
		}

		public void destroy() {
			checkCurrentContext();
			destroyed = true;
		}

		public void setProperty(Object value) {
			checkCurrentContext();
		}

		public Object getProperty() {
			checkCurrentContext();
			return null;
		}

		private void checkCurrentContext() {
			assertEquals(expectedName, getCurrentSubjectName());
		}
	}

	private static class NonPrivilegedSpringCallbacksBean implements
			InitializingBean, DisposableBean, BeanClassLoaderAware,
			BeanFactoryAware, BeanNameAware {

		private String expectedName;
		public static boolean destroyed = false;

		public NonPrivilegedSpringCallbacksBean(String expected) {
			this.expectedName = expected;
			checkCurrentContext();
		}

		public void afterPropertiesSet() {
			checkCurrentContext();
		}

		public void destroy() {
			checkCurrentContext();
			destroyed = true;
		}

		public void setBeanName(String name) {
			checkCurrentContext();
		}

		public void setBeanClassLoader(ClassLoader classLoader) {
			checkCurrentContext();
		}

		public void setBeanFactory(BeanFactory beanFactory)
				throws BeansException {
			checkCurrentContext();
		}

		private void checkCurrentContext() {
			assertEquals(expectedName, getCurrentSubjectName());
		}
	}

	private static class NonPrivilegedFactoryBean implements SmartFactoryBean {
		private String expectedName;

		public NonPrivilegedFactoryBean(String expected) {
			this.expectedName = expected;
			checkCurrentContext();
		}

		public boolean isEagerInit() {
			checkCurrentContext();
			return false;
		}

		public boolean isPrototype() {
			checkCurrentContext();
			return true;
		}

		public Object getObject() throws Exception {
			checkCurrentContext();
			return new Object();
		}

		public Class getObjectType() {
			checkCurrentContext();
			return Object.class;
		}

		public boolean isSingleton() {
			checkCurrentContext();
			return false;
		}

		private void checkCurrentContext() {
			assertEquals(expectedName, getCurrentSubjectName());
		}
	}

	private static class NonPrivilegedFactory {

		private final String expectedName;

		public NonPrivilegedFactory(String expected) {
			this.expectedName = expected;
			assertEquals(expectedName, getCurrentSubjectName());
		}

		public static Object makeStaticInstance(String expectedName) {
			assertEquals(expectedName, getCurrentSubjectName());
			return new Object();
		}

		public Object makeInstance() {
			assertEquals(expectedName, getCurrentSubjectName());
			return new Object();
		}
	}

	private static String getCurrentSubjectName() {
		final AccessControlContext acc = AccessController.getContext();

		return AccessController.doPrivileged(new PrivilegedAction<String>() {

			public String run() {
				Subject subject = Subject.getSubject(acc);
				if (subject == null) {
					return null;
				}

				Set<Principal> principals = subject.getPrincipals();

				if (principals == null) {
					return null;
				}
				for (Principal p : principals) {
					return p.getName();
				}
				return null;
			}
		});
	}

	private static class TestPrincipal implements Principal {

		private String name;

		public TestPrincipal(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof TestPrincipal)) {
				return false;
			}
			TestPrincipal p = (TestPrincipal) obj;
			return this.name.equals(p.name);
		}

		public int hashCode() {
			return this.name.hashCode();
		}
	}

	public CallbacksSecurityTests() {
		// setup security
		if (System.getSecurityManager() == null) {
			Policy policy = Policy.getPolicy();
			URL policyURL = getClass()
					.getResource(
							"/org/springframework/beans/factory/support/security/policy.all");
			System.setProperty("java.security.policy", policyURL.toString());
			System.setProperty("policy.allowSystemProperty", "true");
			policy.refresh();

			System.setSecurityManager(new SecurityManager());
		}
	}

	@Override
	protected void setUp() throws Exception {

		final ProtectionDomain empty = new ProtectionDomain(null,
				new Permissions());

		provider = new SecurityContextProvider() {
			private final AccessControlContext acc = new AccessControlContext(
					new ProtectionDomain[] { empty });

			public AccessControlContext getAccessControlContext() {
				return acc;
			}
		};

		DefaultResourceLoader drl = new DefaultResourceLoader();
		Resource config = drl
				.getResource("/org/springframework/beans/factory/support/security/callbacks.xml");
		beanFactory = new XmlBeanFactory(config);
		beanFactory.setSecurityContextProvider(provider);
	}

	public void testSecuritySanity() throws Exception {
		AccessControlContext acc = provider.getAccessControlContext();
		try {
			acc.checkPermission(new PropertyPermission("*", "read"));
			fail("Acc should not have any permissions");
		} catch (SecurityException se) {
			// expected
		}

		final CustomCallbackBean bean = new CustomCallbackBean();
		final Method method = bean.getClass().getMethod("destroy", null);
		method.setAccessible(true);

		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {

				public Object run() throws Exception {
					method.invoke(bean, null);
					return null;
				}
			}, acc);
			fail("expected security exception");
		} catch (Exception ex) {
		}

		final Class<ConstructorBean> cl = ConstructorBean.class;
		try {
			AccessController.doPrivileged(
					new PrivilegedExceptionAction<Object>() {

						public Object run() throws Exception {
							return cl.newInstance();
						}
					}, acc);
			fail("expected security exception");
		} catch (Exception ex) {
		}
	}

	public void testSpringInitBean() throws Exception {
		try {
			beanFactory.getBean("spring-init");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof SecurityException);
		}
	}

	public void testCustomInitBean() throws Exception {
		try {
			beanFactory.getBean("custom-init");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof SecurityException);
		}
	}

	public void testSpringDestroyBean() throws Exception {
		beanFactory.getBean("spring-destroy");
		beanFactory.destroySingletons();
		assertNull(System.getProperty("security.destroy"));
	}

	public void testCustomDestroyBean() throws Exception {
		beanFactory.getBean("custom-destroy");
		beanFactory.destroySingletons();
		assertNull(System.getProperty("security.destroy"));
	}

	public void testCustomFactoryObject() throws Exception {
		try {
			beanFactory.getBean("spring-factory");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof SecurityException);
		}

	}

	public void testCustomFactoryType() throws Exception {
		assertNull(beanFactory.getType("spring-factory"));
		assertNull(System.getProperty("factory.object.type"));
	}

	public void testCustomStaticFactoryMethod() throws Exception {
		try {
			beanFactory.getBean("custom-static-factory-method");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	public void testCustomInstanceFactoryMethod() throws Exception {
		try {
			beanFactory.getBean("custom-factory-method");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	public void testTrustedFactoryMethod() throws Exception {
		try {
			beanFactory.getBean("privileged-static-factory-method");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	public void testConstructor() throws Exception {
		try {
			beanFactory.getBean("constructor");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	public void testContainerPriviledges() throws Exception {
		AccessControlContext acc = provider.getAccessControlContext();

		AccessController.doPrivileged(new PrivilegedExceptionAction() {

			public Object run() throws Exception {
				beanFactory.getBean("working-factory-method");
				beanFactory.getBean("container-execution");
				return null;
			}
		}, acc);
	}

	public void testPropertyInjection() throws Exception {
		try {
			beanFactory.getBean("property-injection");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("security"));
		}

		beanFactory.getBean("working-property-injection");
	}

	public void testInitSecurityAwarePrototypeBean() {
		final DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder
				.genericBeanDefinition(NonPrivilegedBean.class).setScope(
						ConfigurableBeanFactory.SCOPE_PROTOTYPE)
				.setInitMethodName("init").setDestroyMethodName("destroy")
				.addConstructorArgValue("user1");
		lbf.registerBeanDefinition("test", bdb.getBeanDefinition());
		final Subject subject = new Subject();
		subject.getPrincipals().add(new TestPrincipal("user1"));

		NonPrivilegedBean bean = (NonPrivilegedBean) Subject.doAsPrivileged(
				subject, new PrivilegedAction() {
					public Object run() {
						return lbf.getBean("test");
					}
				}, null);
		assertNotNull(bean);
	}

	public void testTrustedExecution() throws Exception {
		beanFactory.setSecurityContextProvider(null);

		Permissions perms = new Permissions();
		perms.add(new AuthPermission("getSubject"));
		ProtectionDomain pd = new ProtectionDomain(null, perms);

		AccessControlContext acc = new AccessControlContext(
				new ProtectionDomain[] { pd });

		final Subject subject = new Subject();
		subject.getPrincipals().add(new TestPrincipal("user1"));

		// request the beans from non-privileged code
		Subject.doAsPrivileged(subject, new PrivilegedAction<Object>() {

			public Object run() {
				// sanity check
				assertEquals("user1", getCurrentSubjectName());
				assertEquals(false, NonPrivilegedBean.destroyed);

				beanFactory.getBean("trusted-spring-callbacks");
				beanFactory.getBean("trusted-custom-init-destroy");
				// the factory is a prototype - ask for multiple instances
				beanFactory.getBean("trusted-spring-factory");
				beanFactory.getBean("trusted-spring-factory");
				beanFactory.getBean("trusted-spring-factory");

				beanFactory.getBean("trusted-factory-bean");
				beanFactory.getBean("trusted-static-factory-method");
				beanFactory.getBean("trusted-factory-method");
				beanFactory.getBean("trusted-property-injection");
				beanFactory.getBean("trusted-working-property-injection");

				beanFactory.destroySingletons();
				assertEquals(true, NonPrivilegedBean.destroyed);
				return null;
			}
		}, provider.getAccessControlContext());
	}
}