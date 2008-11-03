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

package org.springframework.ejb.access;

import java.lang.reflect.Proxy;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jndi.JndiTemplate;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 21.05.2003
 */
public class LocalStatelessSessionProxyFactoryBeanTests extends TestCase {

	public void testInvokesMethod() throws Exception {
		final int value = 11;
		final String jndiName = "foo";
		
		MockControl ec = MockControl.createControl(MyEjb.class);
		MyEjb myEjb = (MyEjb) ec.getMock();
		myEjb.getValue();
		ec.setReturnValue(value, 1);
		myEjb.remove();
		ec.setVoidCallable(1);
		ec.replay();
		
		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		home.create();
		mc.setReturnValue(myEjb, 1);
		mc.replay();
		
		JndiTemplate jt = new JndiTemplate() {
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
		mc.verify();	
		ec.verify();	
	}
	
	public void testInvokesMethodOnEjb3StyleBean() throws Exception {
		final int value = 11;
		final String jndiName = "foo";

		MockControl ec = MockControl.createControl(MyEjb.class);
		final MyEjb myEjb = (MyEjb) ec.getMock();
		myEjb.getValue();
		ec.setReturnValue(value, 1);
		ec.replay();

		JndiTemplate jt = new JndiTemplate() {
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
		ec.verify();
	}

	public void testCreateException() throws Exception {
		final String jndiName = "foo";
	
		final CreateException cex = new CreateException();
		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		home.create();
		mc.setThrowable(cex);
		mc.replay();
	
		JndiTemplate jt = new JndiTemplate() {
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
		
		mc.verify();	
	}
	
	public void testNoBusinessInterfaceSpecified() throws Exception {
		// Will do JNDI lookup to get home but won't call create
		// Could actually try to figure out interface from create?
		final String jndiName = "foo";

		MockControl mc = MockControl.createControl(MyHome.class);
		final MyHome home = (MyHome) mc.getMock();
		mc.replay();

		JndiTemplate jt = new JndiTemplate() {
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
		mc.verify();	
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
