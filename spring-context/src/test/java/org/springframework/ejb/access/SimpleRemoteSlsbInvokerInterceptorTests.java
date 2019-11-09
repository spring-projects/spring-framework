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

package org.springframework.ejb.access;

import java.rmi.ConnectException;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jndi.JndiTemplate;
import org.springframework.remoting.RemoteAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SimpleRemoteSlsbInvokerInterceptorTests {

	private Context mockContext(
			String jndiName, RemoteInterface ejbInstance)
			throws Exception {
		SlsbHome mockHome = mock(SlsbHome.class);
		given(mockHome.create()).willReturn(ejbInstance);
		Context mockCtx = mock(Context.class);
		given(mockCtx.lookup("java:comp/env/" + jndiName)).willReturn(mockHome);
		return mockCtx;
	}

	private SimpleRemoteSlsbInvokerInterceptor configuredInterceptor(
			final Context mockCtx, String jndiName) throws Exception {

		SimpleRemoteSlsbInvokerInterceptor si = createInterceptor();
		si.setJndiTemplate(new JndiTemplate() {
			@Override
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

	protected Object configuredProxy(SimpleRemoteSlsbInvokerInterceptor si, Class<?> ifc) throws NamingException {
		si.afterPropertiesSet();
		ProxyFactory pf = new ProxyFactory(new Class<?>[] {ifc});
		pf.addAdvice(si);
		return pf.getProxy();
	}


	@Test
	public void testPerformsLookup() throws Exception {
		RemoteInterface ejb = mock(RemoteInterface.class);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);
		configuredProxy(si, RemoteInterface.class);

		verify(mockContext).close();
	}

	@Test
	public void testPerformsLookupWithAccessContext() throws Exception {
		RemoteInterface ejb = mock(RemoteInterface.class);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);
		si.setExposeAccessContext(true);
		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertThat(target.targetMethod()).isNull();

		verify(mockContext, times(2)).close();
		verify(ejb).targetMethod();

	}

	@Test
	public void testLookupFailure() throws Exception {
		final NamingException nex = new NamingException();
		final String jndiName = "foobar";
		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) throws NamingException {
				assertThat(jndiName.equals(name)).isTrue();
				throw nex;
			}
		};

		SimpleRemoteSlsbInvokerInterceptor si = new SimpleRemoteSlsbInvokerInterceptor();
		si.setJndiName("foobar");
		// default resourceRef=false should cause this to fail, as java:/comp/env will not
		// automatically be added
		si.setJndiTemplate(jt);
		assertThatExceptionOfType(NamingException.class).isThrownBy(
				si::afterPropertiesSet)
			.satisfies(ex -> assertThat(ex).isSameAs(nex));
	}

	@Test
	public void testInvokesMethodOnEjbInstance() throws Exception {
		doTestInvokesMethodOnEjbInstance(true, true);
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithLazyLookup() throws Exception {
		doTestInvokesMethodOnEjbInstance(false, true);
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithLazyLookupAndNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstance(false, false);
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstance(true, false);
	}

	private void doTestInvokesMethodOnEjbInstance(boolean lookupHomeOnStartup, boolean cacheHome) throws Exception {
		Object retVal = new Object();
		final RemoteInterface ejb = mock(RemoteInterface.class);
		given(ejb.targetMethod()).willReturn(retVal);

		int lookupCount = 1;
		if (!cacheHome) {
			lookupCount++;
			if (lookupHomeOnStartup) {
				lookupCount++;
			}
		}

		final String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);
		si.setLookupHomeOnStartup(lookupHomeOnStartup);
		si.setCacheHome(cacheHome);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertThat(target.targetMethod() == retVal).isTrue();
		assertThat(target.targetMethod() == retVal).isTrue();

		verify(mockContext, times(lookupCount)).close();
		verify(ejb, times(2)).remove();
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithRemoteException() throws Exception {
		final RemoteInterface ejb = mock(RemoteInterface.class);
		given(ejb.targetMethod()).willThrow(new RemoteException());
		ejb.remove();

		final String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertThatExceptionOfType(RemoteException.class).isThrownBy(
				target::targetMethod);

		verify(mockContext).close();
		verify(ejb, times(2)).remove();
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(true, true);
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefreshAndLazyLookup() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(false, true);
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefreshAndLazyLookupAndNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(false, false);
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithConnectExceptionWithRefreshAndNoCache() throws Exception {
		doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(true, false);
	}

	private void doTestInvokesMethodOnEjbInstanceWithConnectExceptionWithRefresh(
			boolean lookupHomeOnStartup, boolean cacheHome) throws Exception {

		final RemoteInterface ejb = mock(RemoteInterface.class);
		given(ejb.targetMethod()).willThrow(new ConnectException(""));

		int lookupCount = 2;
		if (!cacheHome) {
			lookupCount++;
			if (lookupHomeOnStartup) {
				lookupCount++;
			}
		}

		final String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);
		si.setRefreshHomeOnConnectFailure(true);
		si.setLookupHomeOnStartup(lookupHomeOnStartup);
		si.setCacheHome(cacheHome);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertThatExceptionOfType(ConnectException.class).isThrownBy(
				target::targetMethod);

		verify(mockContext, times(lookupCount)).close();
		verify(ejb, times(2)).remove();
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithBusinessInterface() throws Exception {
		Object retVal = new Object();
		final RemoteInterface ejb = mock(RemoteInterface.class);
		given(ejb.targetMethod()).willReturn(retVal);

		final String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		BusinessInterface target = (BusinessInterface) configuredProxy(si, BusinessInterface.class);
		assertThat(target.targetMethod() == retVal).isTrue();

		verify(mockContext).close();
		verify(ejb).remove();
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithBusinessInterfaceWithRemoteException() throws Exception {
		final RemoteInterface ejb = mock(RemoteInterface.class);
		given(ejb.targetMethod()).willThrow(new RemoteException());

		final String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		BusinessInterface target = (BusinessInterface) configuredProxy(si, BusinessInterface.class);
		assertThatExceptionOfType(RemoteAccessException.class).isThrownBy(
				target::targetMethod);

		verify(mockContext).close();
		verify(ejb).remove();
	}

	@Test
	public void testApplicationException() throws Exception {
		doTestException(new ApplicationException());
	}

	@Test
	public void testRemoteException() throws Exception {
		doTestException(new RemoteException());
	}

	private void doTestException(Exception expected) throws Exception {
		final RemoteInterface ejb = mock(RemoteInterface.class);
		given(ejb.targetMethod()).willThrow(expected);

		final String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		SimpleRemoteSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		RemoteInterface target = (RemoteInterface) configuredProxy(si, RemoteInterface.class);
		assertThatExceptionOfType(Exception.class).isThrownBy(
				target::targetMethod)
			.satisfies(ex -> assertThat(ex).isSameAs(expected));
		verify(mockContext).close();
		verify(ejb).remove();
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


	@SuppressWarnings("serial")
	protected class ApplicationException extends Exception {

		public ApplicationException() {
			super("appException");
		}
	}

}
