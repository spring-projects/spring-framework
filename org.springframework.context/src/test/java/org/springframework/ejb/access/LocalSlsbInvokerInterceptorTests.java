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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jndi.JndiTemplate;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
*/
public class LocalSlsbInvokerInterceptorTests {

	/**
	 * Test that it performs the correct lookup.
	 */
	@Test
	public void testPerformsLookup() throws Exception {
		LocalInterfaceWithBusinessMethods ejb = createMock(LocalInterfaceWithBusinessMethods.class);
		replay(ejb);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		configuredInterceptor(mockContext, jndiName);

		verify(mockContext);
	}

	@Test
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

	@Test
	public void testInvokesMethodOnEjbInstance() throws Exception {
		Object retVal = new Object();
		LocalInterfaceWithBusinessMethods ejb = createMock(LocalInterfaceWithBusinessMethods.class);
		expect(ejb.targetMethod()).andReturn(retVal);
		ejb.remove();
		replay(ejb);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		ProxyFactory pf = new ProxyFactory(new Class[] { BusinessMethods.class } );
		pf.addAdvice(si);
		BusinessMethods target = (BusinessMethods) pf.getProxy();

		assertTrue(target.targetMethod() == retVal);

		verify(mockContext);
		verify(ejb);
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithSeparateBusinessMethods() throws Exception {
		Object retVal = new Object();
		LocalInterface ejb = createMock(LocalInterface.class);
		expect(ejb.targetMethod()).andReturn(retVal);
		ejb.remove();
		replay(ejb);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		ProxyFactory pf = new ProxyFactory(new Class[] { BusinessMethods.class } );
		pf.addAdvice(si);
		BusinessMethods target = (BusinessMethods) pf.getProxy();

		assertTrue(target.targetMethod() == retVal);

		verify(mockContext);
		verify(ejb);
	}

	private void testException(Exception expected) throws Exception {
		LocalInterfaceWithBusinessMethods ejb = createMock(LocalInterfaceWithBusinessMethods.class);
		expect(ejb.targetMethod()).andThrow(expected);
		replay(ejb);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

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

		verify(mockContext);
		verify(ejb);
	}

	@Test
	public void testApplicationException() throws Exception {
		testException(new ApplicationException());
	}

	protected Context mockContext(final String jndiName, final Object ejbInstance)
			throws Exception {

		final SlsbHome mockHome = createMock(SlsbHome.class);
		expect(mockHome.create()).andReturn((LocalInterface)ejbInstance);
		replay(mockHome);

		final Context mockCtx = createMock(Context.class);

		expect(mockCtx.lookup("java:comp/env/" + jndiName)).andReturn(mockHome);
		mockCtx.close();
		replay(mockCtx);
		return mockCtx;
	}

	protected LocalSlsbInvokerInterceptor configuredInterceptor(final Context mockCtx, final String jndiName)
			throws Exception {

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


	@SuppressWarnings("serial")
	private class ApplicationException extends Exception {

		public ApplicationException() {
			super("appException");
		}
	}

}
