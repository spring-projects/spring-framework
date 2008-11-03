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

import java.rmi.ConnectException;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jndi.JndiTemplate;
import org.springframework.remoting.RemoteAccessException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class SimpleRemoteSlsbInvokerInterceptorTests extends TestCase {

	private MockControl contextControl(
			String jndiName, RemoteInterface ejbInstance, int createCount, int lookupCount, int closeCount)
			throws Exception {

		MockControl homeControl = MockControl.createControl(SlsbHome.class);
		final SlsbHome mockHome = (SlsbHome) homeControl.getMock();
		mockHome.create();
		homeControl.setReturnValue(ejbInstance, createCount);
		homeControl.replay();

		MockControl ctxControl = MockControl.createControl(Context.class);
		final Context mockCtx = (Context) ctxControl.getMock();

		mockCtx.lookup("java:comp/env/" + jndiName);
		ctxControl.setReturnValue(mockHome, lookupCount);
		mockCtx.close();
		ctxControl.setVoidCallable(closeCount);
		ctxControl.replay();

		return ctxControl;
	}

	private SimpleRemoteSlsbInvokerInterceptor configuredInterceptor(
			MockControl ctxControl, String jndiName) throws Exception {

		final Context mockCtx = (Context) ctxControl.getMock();
		SimpleRemoteSlsbInvokerInterceptor si = createInterceptor();
		si.setJndiTemplate(new JndiTemplate() {
			protected Context createInitialContext() {
				return mockCtx;
			}
		});
		si.setResourceRef(true);
		si.setJndiName(jndiName);

		return si;
	}

	protected SimpleRemoteSlsbInvokerInterceptor createInterceptor() {
		return new SimpleRemoteSlsbInvokerInterceptor();
	}

	protected Object configuredProxy(SimpleRemoteSlsbInvokerInterceptor si, Class ifc) throws NamingException {
		si.afterPropertiesSet();
		ProxyFactory pf = new ProxyFactory(new Class[] {ifc});
		pf.addAdvice(si);
		return pf.getProxy();
	}


	public void testPerformsLookup() throws Exception {
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejbControl.replay();

		String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 1, 1, 1);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);
		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);

		contextControl.verify();
	}

	public void testPerformsLookupWithAccessContext() throws Exception {
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setReturnValue(null);
		ejbControl.replay();

		String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 1, 1, 2);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);
		si.setExposeAccessContext(true);
		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertNull(target.targetMethod());

		contextControl.verify();
	}

	public void testLookupFailure() throws Exception {
		final NamingException nex = new NamingException();
		final String jndiName = "foobar";
		JndiTemplate jt = new JndiTemplate() {
			public Object lookup(String name) throws NamingException {
				assertTrue(jndiName.equals(name));
				throw nex;
			}
		};

		SimpleRemoteSlsbInvokerInterceptor si = new SimpleRemoteSlsbInvokerInterceptor();
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
		doTestInvokesMethodOnEjbInstance(true, true);
	}

	public void testInvokesMethodOnEjbInstanceWithLazyLookup() throws Exception {
		doTestInvokesMethodOnEjbInstance(false, true);
	}

	public void testInvokesMethodOnEjbInstanceWithLazyLookupAndNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstance(false, false);
	}

	public void testInvokesMethodOnEjbInstanceWithNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstance(true, false);
	}

	private void doTestInvokesMethodOnEjbInstance(boolean lookupHomeOnStartup, boolean cacheHome) throws Exception {
		Object retVal = new Object();
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		final RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setReturnValue(retVal, 2);
		ejb.remove();
		ejbControl.setVoidCallable(2);
		ejbControl.replay();

		int lookupCount = 1;
		if (!cacheHome) {
			lookupCount++;
			if (lookupHomeOnStartup) {
				lookupCount++;
			}
		}

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 2, lookupCount, lookupCount);
	
		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);
		si.setLookupHomeOnStartup(lookupHomeOnStartup);
		si.setCacheHome(cacheHome);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertTrue(target.targetMethod() == retVal);
		assertTrue(target.targetMethod() == retVal);

		contextControl.verify();
		ejbControl.verify();
	}

	public void testInvokesMethodOnEjbInstanceWithHomeInterface() throws Exception {
		Object retVal = new Object();
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		final RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setReturnValue(retVal, 1);
		ejb.remove();
		ejbControl.setVoidCallable(1);
		ejbControl.replay();

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 1, 1, 1);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);
		si.setHomeInterface(SlsbHome.class);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertTrue(target.targetMethod() == retVal);

		contextControl.verify();
		ejbControl.verify();
	}

	public void testInvokesMethodOnEjbInstanceWithRemoteException() throws Exception {
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		final RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setThrowable(new RemoteException(), 1);
		ejb.remove();
		ejbControl.setVoidCallable(1);
		ejbControl.replay();

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 1, 1, 1);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		try {
			target.targetMethod();
			fail("Should have thrown RemoteException");
		}
		catch (RemoteException ex) {
			// expected
		}

		contextControl.verify();
		ejbControl.verify();
	}

	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(true, true);
	}

	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefreshAndLazyLookup() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(false, true);
	}

	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefreshAndLazyLookupAndNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(false, false);
	}

	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefreshAndNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(true, false);
	}

	private void doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(
			boolean lookupHomeOnStartup, boolean cacheHome) throws Exception {

		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		final RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setThrowable(new ConnectException(""), 2);
		ejb.remove();
		ejbControl.setVoidCallable(2);
		ejbControl.replay();

		int lookupCount = 2;
		if (!cacheHome) {
			lookupCount++;
			if (lookupHomeOnStartup) {
				lookupCount++;
			}
		}

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 2, lookupCount, lookupCount);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);
		si.setRefreshHomeOnConnectFailure(true);
		si.setLookupHomeOnStartup(lookupHomeOnStartup);
		si.setCacheHome(cacheHome);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		try {
			target.targetMethod();
			fail("Should have thrown RemoteException");
		}
		catch (ConnectException ex) {
			// expected
		}

		contextControl.verify();
		ejbControl.verify();
	}

	public void testInvokesMethodOnEjbInstanceWithBusinessInterface() throws Exception {
		Object retVal = new Object();
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		final RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setReturnValue(retVal, 1);
		ejb.remove();
		ejbControl.setVoidCallable(1);
		ejbControl.replay();

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 1, 1, 1);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);

		BusinessInterface target = (BusinessInterface) configuredProxy(si, BusinessInterface.class);
		assertTrue(target.targetMethod() == retVal);

		contextControl.verify();
		ejbControl.verify();
	}

	public void testInvokesMethodOnEjbInstanceWithBusinessInterfaceWithRemoteException() throws Exception {
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		final RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setThrowable(new RemoteException(), 1);
		ejb.remove();
		ejbControl.setVoidCallable(1);
		ejbControl.replay();

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 1, 1, 1);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);

		BusinessInterface target = (BusinessInterface) configuredProxy(si, BusinessInterface.class);
		try {
			target.targetMethod();
			fail("Should have thrown RemoteAccessException");
		}
		catch (RemoteAccessException ex) {
			// expected
		}

		contextControl.verify();
		ejbControl.verify();
	}

	public void testApplicationException() throws Exception {
		doTestException(new ApplicationException());
	}

	public void testRemoteException() throws Exception {
		doTestException(new RemoteException());
	}

	private void doTestException(Exception expected) throws Exception {
		MockControl ejbControl = MockControl.createControl(RemoteInterface.class);
		final RemoteInterface ejb = (RemoteInterface) ejbControl.getMock();
		ejb.targetMethod();
		ejbControl.setThrowable(expected);
		ejb.remove();
		ejbControl.setVoidCallable(1);
		ejbControl.replay();

		final String jndiName= "foobar";
		MockControl contextControl = contextControl(jndiName, ejb, 1, 1, 1);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(contextControl, jndiName);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		try {
			target.targetMethod();
			fail("Should have thrown remote exception");
		}
		catch (Exception thrown) {
			assertTrue(thrown == expected);
		}

		contextControl.verify();
		ejbControl.verify();
	}


	/**
	 * Needed so that we can mock create() method.
	 */
	protected interface SlsbHome extends EJBHome {

		EJBObject create() throws RemoteException, CreateException;
	}


	protected interface RemoteInterface extends EJBObject {

		// Also business exception!?
		Object targetMethod() throws RemoteException, ApplicationException;
	}


	protected interface BusinessInterface {

		Object targetMethod() throws ApplicationException;
	}


	protected class ApplicationException extends Exception {

		public ApplicationException() {
			super("appException");
		}
	}

}
