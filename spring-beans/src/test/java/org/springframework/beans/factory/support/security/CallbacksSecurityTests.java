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
import java.util.function.Consumer;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SecurityContextProvider;
import org.springframework.beans.factory.support.security.support.ConstructorBean;
import org.springframework.beans.factory.support.security.support.CustomCallbackBean;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Security test case. Checks whether the container uses its privileges for its
 * internal work but does not leak them when touching/calling user code.
 *
 * <p>The first half of the test case checks that permissions are downgraded when
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
			assertThat(getCurrentSubjectName()).isEqualTo(expectedName);
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
			assertThat(getCurrentSubjectName()).isEqualTo(expectedName);
		}
	}

	@SuppressWarnings({ "unused", "rawtypes" })
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
			assertThat(getCurrentSubjectName()).isEqualTo(expectedName);
		}
	}

	@SuppressWarnings("unused")
	private static class NonPrivilegedFactory {

		private final String expectedName;

		public NonPrivilegedFactory(String expected) {
			this.expectedName = expected;
			assertThat(getCurrentSubjectName()).isEqualTo(expectedName);
		}

		public static Object makeStaticInstance(String expectedName) {
			assertThat(getCurrentSubjectName()).isEqualTo(expectedName);
			return new Object();
		}

		public Object makeInstance() {
			assertThat(getCurrentSubjectName()).isEqualTo(expectedName);
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

		@Override
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

		@Override
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

	@BeforeEach
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
		assertThatExceptionOfType(SecurityException.class).as(
				"Acc should not have any permissions").isThrownBy(() ->
				acc.checkPermission(new PropertyPermission("*", "read")));

		CustomCallbackBean bean = new CustomCallbackBean();
		Method method = bean.getClass().getMethod("destroy");
		method.setAccessible(true);

		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						method.invoke(bean);
						return null;
					}, acc));

		Class<ConstructorBean> cl = ConstructorBean.class;
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
						cl.newInstance(), acc));
	}

	@Test
	public void testSpringInitBean() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("spring-init"))
			.withCauseInstanceOf(SecurityException.class);
	}

	@Test
	public void testCustomInitBean() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("custom-init"))
			.withCauseInstanceOf(SecurityException.class);
	}

	@Test
	public void testSpringDestroyBean() throws Exception {
		beanFactory.getBean("spring-destroy");
		beanFactory.destroySingletons();
		assertThat(System.getProperty("security.destroy")).isNull();
	}

	@Test
	public void testCustomDestroyBean() throws Exception {
		beanFactory.getBean("custom-destroy");
		beanFactory.destroySingletons();
		assertThat(System.getProperty("security.destroy")).isNull();
	}

	@Test
	public void testCustomFactoryObject() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("spring-factory"))
			.withCauseInstanceOf(SecurityException.class);
	}

	@Test
	public void testCustomFactoryType() throws Exception {
		assertThat(beanFactory.getType("spring-factory")).isNull();
		assertThat(System.getProperty("factory.object.type")).isNull();
	}

	@Test
	public void testCustomStaticFactoryMethod() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("custom-static-factory-method"))
			.satisfies(ex -> assertThat(ex.getMostSpecificCause()).isInstanceOf(SecurityException.class));
	}

	@Test
	public void testCustomInstanceFactoryMethod() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("custom-factory-method"))
			.satisfies(ex -> assertThat(ex.getMostSpecificCause()).isInstanceOf(SecurityException.class));
	}

	@Test
	public void testTrustedFactoryMethod() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("privileged-static-factory-method"))
			.satisfies(mostSpecificCauseOf(SecurityException.class));
	}

	@Test
	public void testConstructor() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("constructor"))
			.satisfies(mostSpecificCauseOf(SecurityException.class));
	}

	@Test
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
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				beanFactory.getBean("property-injection"))
			.withMessageContaining("security");
		beanFactory.getBean("working-property-injection");
	}

	@Test
	public void testInitSecurityAwarePrototypeBean() {
		final DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		BeanDefinitionBuilder bdb = BeanDefinitionBuilder
				.genericBeanDefinition(NonPrivilegedBean.class).setScope(
						BeanDefinition.SCOPE_PROTOTYPE)
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
		assertThat(bean).isNotNull();
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
				assertThat(getCurrentSubjectName()).isEqualTo("user1");
				assertThat(NonPrivilegedBean.destroyed).isEqualTo(false);

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
				assertThat(NonPrivilegedBean.destroyed).isEqualTo(true);
				return null;
			}
		}, provider.getAccessControlContext());
	}

	private <E extends NestedRuntimeException> Consumer<E> mostSpecificCauseOf(Class<? extends Throwable> type) {
		return ex -> assertThat(ex.getMostSpecificCause()).isInstanceOf(type);

	}

}
