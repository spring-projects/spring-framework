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
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.PropertyPermission;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.SecurityContextProvider;
import org.springframework.beans.factory.support.security.support.ConstructorBean;
import org.springframework.beans.factory.support.security.support.CustomCallbackBean;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * @author Costin Leau
 */
public class CallbacksSecurityTest extends TestCase {

	private XmlBeanFactory beanFactory;
	private SecurityContextProvider provider;

	public CallbacksSecurityTest() {
		// setup security
		if (System.getSecurityManager() == null) {
			Policy policy = Policy.getPolicy();
			URL policyURL = getClass().getResource("/org/springframework/beans/factory/support/security/policy.all");
			System.setProperty("java.security.policy", policyURL.toString());
			System.setProperty("policy.allowSystemProperty", "true");
			policy.refresh();

			System.setSecurityManager(new SecurityManager());
		}
	}

	@Override
	protected void setUp() throws Exception {

		final ProtectionDomain empty = new ProtectionDomain(null, new Permissions());

		provider = new SecurityContextProvider() {
			private final AccessControlContext acc = new AccessControlContext(new ProtectionDomain[] { empty });

			public AccessControlContext getAccessControlContext() {
				return acc;
			}
		};

		DefaultResourceLoader drl = new DefaultResourceLoader();
		Resource config = drl.getResource("/org/springframework/beans/factory/support/security/callbacks.xml");
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
			AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

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
			beanFactory.getBean("trusted-factory-method");
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
}