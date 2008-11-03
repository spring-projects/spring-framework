/*
 * Copyright 2002-2007 the original author or authors.
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

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jndi.JndiTemplate;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
*/
public class LocalSlsbInvokerInterceptorTests extends TestCase {

	/**
	 * Test that it performs the correct lookup.
	 */
	public void testPerformsLookup() throws Exception {
		MockControl ejbControl = MockControl.createControl(LocalInterfaceWithBusinessMethods.class);
		final LocalInterfaceWithBusinessMethods ejb = (LocalInterfaceWithBusinessMethods) ejbControl.getMock();
		ejbControl.replay();
		
		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb);
		
		LocalSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);
		
		contextControl.verify();
	}
	
	public void testLookupFailure() throws Exception {
		final NamingException nex = new NamingException();
		final String jndiName= "foobar";
		JndiTemplate jt = new JndiTemplate() {
			public Object lookup(String name) throws NamingException {
				assertTrue(jndiName.equals(name));
				throw nex;
			}
		};
	
		LocalSlsbInvokerInterceptor si = new LocalSlsbInvokerInterceptor();
		si.setJndiName("foobar");
		// default resourceRef=false should cause this to fail, as java:/comp/env will not
		// automatically be added
		si.setJndiTemplate(jt);
		try {
			si.afterPropertiesSet();
			fail("Should have failed with naming exception");
		}
		catch (NamingException ex) {
			assertTrue(ex == nex);
		}
	}
	
	public void testInvokesMethodOnEjbInstance() throws Exception {
		Object retVal = new Object();
		MockControl ejbControl = MockControl.createControl(LocalInterfaceWithBusinessMethods.class);
		final LocalInterfaceWithBusinessMethods ejb = (LocalInterfaceWithBusinessMethods) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setReturnValue(retVal, 1);
		ejb.remove();
		ejbControl.setVoidCallable(1);
		ejbControl.replay();
	
		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb);
	
		LocalSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);
	
		ProxyFactory pf = new ProxyFactory(new Class[] { BusinessMethods.class } );
		pf.addAdvice(si);
		BusinessMethods target = (BusinessMethods) pf.getProxy();
	
		assertTrue(target.targetMethod() == retVal);
	
		contextControl.verify();
		ejbControl.verify();
	}
	
	public void testInvokesMethodOnEjbInstanceWithSeparateBusinessMethods() throws Exception {
		Object retVal = new Object();
		MockControl ejbControl = MockControl.createControl(LocalInterface.class);
		final LocalInterface ejb = (LocalInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setReturnValue(retVal, 1);
		ejb.remove();
		ejbControl.setVoidCallable(1);
		ejbControl.replay();

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);

		ProxyFactory pf = new ProxyFactory(new Class[] { BusinessMethods.class } );
		pf.addAdvice(si);
		BusinessMethods target = (BusinessMethods) pf.getProxy();

		assertTrue(target.targetMethod() == retVal);

		contextControl.verify();
		ejbControl.verify();
	}

	private void testException(Exception expected) throws Exception {
		MockControl ejbControl = MockControl.createControl(LocalInterfaceWithBusinessMethods.class);
		final LocalInterfaceWithBusinessMethods ejb = (LocalInterfaceWithBusinessMethods) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setThrowable(expected);
		ejbControl.replay();

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);

		ProxyFactory pf = new ProxyFactory(new Class[] { LocalInterfaceWithBusinessMethods.class } );
		pf.addAdvice(si);
		LocalInterfaceWithBusinessMethods target = (LocalInterfaceWithBusinessMethods) pf.getProxy();

		try {
			target.targetMethod();
			fail("Should have thrown exception");
		}
		catch (Exception thrown) {
			assertTrue(thrown == expected);
		}

		contextControl.verify();
		ejbControl.verify();
	}
	
	public void testApplicationException() throws Exception {
		testException(new ApplicationException());
	}

	protected MockControl contextControl(final String jndiName, final Object ejbInstance)
			throws Exception {

		MockControl homeControl = MockControl.createControl(SlsbHome.class);
		final SlsbHome mockHome = (SlsbHome) homeControl.getMock();
		mockHome.create();
		homeControl.setReturnValue(ejbInstance, 1);
		homeControl.replay();
		
		MockControl ctxControl = MockControl.createControl(Context.class);
		final Context mockCtx = (Context) ctxControl.getMock();
		
		mockCtx.lookup("java:comp/env/" + jndiName);
		ctxControl.setReturnValue(mockHome);
		mockCtx.close();
		ctxControl.setVoidCallable();
		ctxControl.replay();
		return ctxControl;
	}
		
	protected LocalSlsbInvokerInterceptor configuredInterceptor(MockControl contextControl, final String jndiName)
			throws Exception {

		final Context mockCtx = (Context) contextControl.getMock();
		LocalSlsbInvokerInterceptor si = new LocalSlsbInvokerInterceptor();
		si.setJndiTemplate(new JndiTemplate() {
			protected Context createInitialContext() throws NamingException {
				return mockCtx;
			}
		});
		si.setJndiName(jndiName);
		si.setResourceRef(true);
		si.afterPropertiesSet();
		
		return si;
	}
	
	
	/** 
	 * Needed so that we can mock the create() method.
	 */
	private interface SlsbHome extends EJBLocalHome {

		LocalInterface create() throws CreateException;
	}


	private interface BusinessMethods {

		Object targetMethod() throws ApplicationException;
	}


	private interface LocalInterface extends EJBLocalObject {

		Object targetMethod() throws ApplicationException;
	}


	private interface LocalInterfaceWithBusinessMethods extends LocalInterface, BusinessMethods {
	}


	private class ApplicationException extends Exception {

		public ApplicationException() {
			super("appException");
		}
	}
 
}
