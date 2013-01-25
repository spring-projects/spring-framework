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

package org.springframework.beans.factory.support.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
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
public class CallbacksSecurityTests {

	private DefaultListableBeanFactory beanFactory;
	private SecurityContextProvider provider;

	@SuppressWarnings("unused")
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

		public void setListProperty(Object value) {
			checkCurrentContext();
		}

		public Object getListProperty() {
			checkCurrentContext();
			return null;
		}

		private void checkCurrentContext() {
			assertEquals(expectedName, getCurrentSubjectName());
		}
	}

	@SuppressWarnings("unused")
	private static class NonPrivilegedSpringCallbacksBean implements
			InitializingBean, DisposableBean, BeanClassLoaderAware,
			BeanFactoryAware, BeanNameAware {

		private String expectedName;
		public static boolean destroyed = false;

		public NonPrivilegedSpringCallbacksBean(String expected) {
			this.expectedName = expected;
			checkCurrentContext();
		}

		@Override
		public void afterPropertiesSet() {
			checkCurrentContext();
		}

		@Override
		public void destroy() {
			checkCurrentContext();
			destroyed = true;
		}

		@Override
		public void setBeanName(String name) {
			checkCurrentContext();
		}

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			checkCurrentContext();
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory)
				throws BeansException {
			checkCurrentContext();
		}

		private void checkCurrentContext() {
			assertEquals(expectedName, getCurrentSubjectName());
		}
	}

	@SuppressWarnings("unused")
	private static class NonPrivilegedFactoryBean implements SmartFactoryBean {
		private String expectedName;

		public NonPrivilegedFactoryBean(String expected) {
			this.expectedName = expected;
			checkCurrentContext();
		}

		@Override
		public boolean isEagerInit() {
			checkCurrentContext();
			return false;
		}

		@Override
		public boolean isPrototype() {
			checkCurrentContext();
			return true;
		}

		@Override
		public Object getObject() throws Exception {
			checkCurrentContext();
			return new Object();
		}

		@Override
		public Class getObjectType() {
			checkCurrentContext();
			return Object.class;
		}

		@Override
		public boolean isSingleton() {
			checkCurrentContext();
			return false;
		}

		private void checkCurrentContext() {
			assertEquals(expectedName, getCurrentSubjectName());
		}
	}

	@SuppressWarnings("unused")
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

			@Override
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

		@Override
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

	@Before
	public void setUp() throws Exception {

		final ProtectionDomain empty = new ProtectionDomain(null,
				new Permissions());

		provider = new SecurityContextProvider() {
			private final AccessControlContext acc = new AccessControlContext(
					new ProtectionDomain[] { empty });

			@Override
			public AccessControlContext getAccessControlContext() {
				return acc;
			}
		};

		DefaultResourceLoader drl = new DefaultResourceLoader();
		Resource config = drl
				.getResource("/org/springframework/beans/factory/support/security/callbacks.xml");
		beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(config);
		beanFactory.setSecurityContextProvider(provider);
	}

	@Test
	public void testSecuritySanity() throws Exception {
		AccessControlContext acc = provider.getAccessControlContext();
		try {
			acc.checkPermission(new PropertyPermission("*", "read"));
			fail("Acc should not have any permissions");
		} catch (SecurityException se) {
			// expected
		}

		final CustomCallbackBean bean = new CustomCallbackBean();
		final Method method = bean.getClass().getMethod("destroy");
		method.setAccessible(true);

		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

				@Override
				public Object run() throws Exception {
					method.invoke(bean);
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

						@Override
						public Object run() throws Exception {
							return cl.newInstance();
						}
					}, acc);
			fail("expected security exception");
		} catch (Exception ex) {
		}
	}

	@Test
	public void testSpringInitBean() throws Exception {
		try {
			beanFactory.getBean("spring-init");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof SecurityException);
		}
	}

	@Test
	public void testCustomInitBean() throws Exception {
		try {
			beanFactory.getBean("custom-init");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof SecurityException);
		}
	}

	@Test
	public void testSpringDestroyBean() throws Exception {
		beanFactory.getBean("spring-destroy");
		beanFactory.destroySingletons();
		assertNull(System.getProperty("security.destroy"));
	}

	@Test
	public void testCustomDestroyBean() throws Exception {
		beanFactory.getBean("custom-destroy");
		beanFactory.destroySingletons();
		assertNull(System.getProperty("security.destroy"));
	}

	@Test
	public void testCustomFactoryObject() throws Exception {
		try {
			beanFactory.getBean("spring-factory");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof SecurityException);
		}

	}

	@Test
	public void testCustomFactoryType() throws Exception {
		assertNull(beanFactory.getType("spring-factory"));
		assertNull(System.getProperty("factory.object.type"));
	}

	@Test
	public void testCustomStaticFactoryMethod() throws Exception {
		try {
			beanFactory.getBean("custom-static-factory-method");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	@Test
	public void testCustomInstanceFactoryMethod() throws Exception {
		try {
			beanFactory.getBean("custom-factory-method");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	@Test
	public void testTrustedFactoryMethod() throws Exception {
		try {
			beanFactory.getBean("privileged-static-factory-method");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	@Test
	public void testConstructor() throws Exception {
		try {
			beanFactory.getBean("constructor");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.getMostSpecificCause() instanceof SecurityException);
		}
	}

	@Test
	@Ignore("passes under Eclipse, but fails under Gradle with https://gist.github.com/1664133")
	// TODO [SPR-10074] passes under Eclipse, but fails under Gradle with
	// https://gist.github.com/1664133
	public void testContainerPrivileges() throws Exception {
		AccessControlContext acc = provider.getAccessControlContext();

		AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

			@Override
			public Object run() throws Exception {
				beanFactory.getBean("working-factory-method");
				beanFactory.getBean("container-execution");
				return null;
			}
		}, acc);
	}

	@Test
	public void testPropertyInjection() throws Exception {
		try {
			beanFactory.getBean("property-injection");
			fail("expected security exception");
		} catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("security"));
		}

		beanFactory.getBean("working-property-injection");
	}

	@Test
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

		NonPrivilegedBean bean = Subject.doAsPrivileged(
				subject, new PrivilegedAction<NonPrivilegedBean>() {
					@Override
					public NonPrivilegedBean run() {
						return lbf.getBean("test", NonPrivilegedBean.class);
					}
				}, null);
		assertNotNull(bean);
	}

	@Test
	public void testTrustedExecution() throws Exception {
		beanFactory.setSecurityContextProvider(null);

		Permissions perms = new Permissions();
		perms.add(new AuthPermission("getSubject"));
		ProtectionDomain pd = new ProtectionDomain(null, perms);

		new AccessControlContext(new ProtectionDomain[] { pd });

		final Subject subject = new Subject();
		subject.getPrincipals().add(new TestPrincipal("user1"));

		// request the beans from non-privileged code
		Subject.doAsPrivileged(subject, new PrivilegedAction<Object>() {

			@Override
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
