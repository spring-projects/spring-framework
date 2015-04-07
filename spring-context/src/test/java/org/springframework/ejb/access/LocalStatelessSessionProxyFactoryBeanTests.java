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

package org.springframework.ejb.access;

import java.lang.reflect.Proxy;
import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.NamingException;

import org.junit.Test;

import org.springframework.jndi.JndiTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 21.05.2003
 */
public class LocalStatelessSessionProxyFactoryBeanTests {

	@Test
	public void testInvokesMethod() throws Exception {
		final int value = 11;
		final String jndiName = "foo";

		MyEjb myEjb = mock(MyEjb.class);
		given(myEjb.getValue()).willReturn(value);

		final MyHome home = mock(MyHome.class);
		given(home.create()).willReturn(myEjb);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals("java:comp/env/" + jndiName));
				return home;
			}
		};

		LocalStatelessSessionProxyFactoryBean fb = new LocalStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setResourceRef(true);
		fb.setBusinessInterface(MyBusinessMethods.class);
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertTrue(Proxy.isProxyClass(mbm.getClass()));
		assertTrue(mbm.getValue() == value);
		verify(myEjb).remove();
	}

	@Test
	public void testInvokesMethodOnEjb3StyleBean() throws Exception {
		final int value = 11;
		final String jndiName = "foo";

		final MyEjb myEjb = mock(MyEjb.class);
		given(myEjb.getValue()).willReturn(value);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals("java:comp/env/" + jndiName));
				return myEjb;
			}
		};

		LocalStatelessSessionProxyFactoryBean fb = new LocalStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setResourceRef(true);
		fb.setBusinessInterface(MyBusinessMethods.class);
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertTrue(Proxy.isProxyClass(mbm.getClass()));
		assertTrue(mbm.getValue() == value);
	}

	@Test
	public void testCreateException() throws Exception {
		final String jndiName = "foo";

		final CreateException cex = new CreateException();
		final MyHome home = mock(MyHome.class);
		given(home.create()).willThrow(cex);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals(jndiName));
				return home;
			}
		};

		LocalStatelessSessionProxyFactoryBean fb = new LocalStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setResourceRef(false);	// no java:comp/env prefix
		fb.setBusinessInterface(MyBusinessMethods.class);
		assertEquals(fb.getBusinessInterface(), MyBusinessMethods.class);
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertTrue(Proxy.isProxyClass(mbm.getClass()));

		try {
			mbm.getValue();
			fail("Should have failed to create EJB");
		}
		catch (EjbAccessException ex) {
			assertSame(cex, ex.getCause());
		}
	}

	@Test
	public void testNoBusinessInterfaceSpecified() throws Exception {
		// Will do JNDI lookup to get home but won't call create
		// Could actually try to figure out interface from create?
		final String jndiName = "foo";

		final MyHome home = mock(MyHome.class);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) throws NamingException {
				// parameterize
				assertTrue(name.equals("java:comp/env/" + jndiName));
				return home;
			}
		};

		LocalStatelessSessionProxyFactoryBean fb = new LocalStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setResourceRef(true);
		// Don't set business interface
		fb.setJndiTemplate(jt);

		// Check it's a singleton
		assertTrue(fb.isSingleton());

		try {
			fb.afterPropertiesSet();
			fail("Should have failed to create EJB");
		}
		catch (IllegalArgumentException ex) {
			// TODO more appropriate exception?
			assertTrue(ex.getMessage().indexOf("businessInterface") != 1);
		}

		// Expect no methods on home
		verifyZeroInteractions(home);
	}


	public static interface MyHome extends EJBLocalHome {

		MyBusinessMethods create() throws CreateException;
	}


	public static interface MyBusinessMethods  {

		int getValue();
	}


	public static interface MyEjb extends EJBLocalObject, MyBusinessMethods {
	}

}
