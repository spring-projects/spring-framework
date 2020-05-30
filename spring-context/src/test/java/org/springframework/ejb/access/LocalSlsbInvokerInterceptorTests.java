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

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.Context;
import javax.naming.NamingException;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jndi.JndiTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
		LocalInterfaceWithBusinessMethods ejb = mock(LocalInterfaceWithBusinessMethods.class);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		configuredInterceptor(mockContext, jndiName);

		verify(mockContext).close();
	}

	@Test
	public void testLookupFailure() throws Exception {
		final NamingException nex = new NamingException();
		final String jndiName= "foobar";
		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) throws NamingException {
				assertThat(jndiName.equals(name)).isTrue();
				throw nex;
			}
		};

		LocalSlsbInvokerInterceptor si = new LocalSlsbInvokerInterceptor();
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
		Object retVal = new Object();
		LocalInterfaceWithBusinessMethods ejb = mock(LocalInterfaceWithBusinessMethods.class);
		given(ejb.targetMethod()).willReturn(retVal);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		ProxyFactory pf = new ProxyFactory(new Class<?>[] { BusinessMethods.class });
		pf.addAdvice(si);
		BusinessMethods target = (BusinessMethods) pf.getProxy();

		assertThat(target.targetMethod() == retVal).isTrue();

		verify(mockContext).close();
		verify(ejb).remove();
	}

	@Test
	public void testInvokesMethodOnEjbInstanceWithSeparateBusinessMethods() throws Exception {
		Object retVal = new Object();
		LocalInterface ejb = mock(LocalInterface.class);
		given(ejb.targetMethod()).willReturn(retVal);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		ProxyFactory pf = new ProxyFactory(new Class<?>[] { BusinessMethods.class });
		pf.addAdvice(si);
		BusinessMethods target = (BusinessMethods) pf.getProxy();

		assertThat(target.targetMethod() == retVal).isTrue();

		verify(mockContext).close();
		verify(ejb).remove();
	}

	private void testException(Exception expected) throws Exception {
		LocalInterfaceWithBusinessMethods ejb = mock(LocalInterfaceWithBusinessMethods.class);
		given(ejb.targetMethod()).willThrow(expected);

		String jndiName= "foobar";
		Context mockContext = mockContext(jndiName, ejb);

		LocalSlsbInvokerInterceptor si = configuredInterceptor(mockContext, jndiName);

		ProxyFactory pf = new ProxyFactory(new Class<?>[] { LocalInterfaceWithBusinessMethods.class });
		pf.addAdvice(si);
		LocalInterfaceWithBusinessMethods target = (LocalInterfaceWithBusinessMethods) pf.getProxy();

		assertThatExceptionOfType(Exception.class).isThrownBy(
				target::targetMethod)
			.satisfies(ex -> assertThat(ex).isSameAs(expected));

		verify(mockContext).close();
	}

	@Test
	public void testApplicationException() throws Exception {
		testException(new ApplicationException());
	}

	protected Context mockContext(final String jndiName, final Object ejbInstance)
			throws Exception {
		SlsbHome mockHome = mock(SlsbHome.class);
		given(mockHome.create()).willReturn((LocalInterface)ejbInstance);
		Context mockCtx = mock(Context.class);
		given(mockCtx.lookup("java:comp/env/" + jndiName)).willReturn(mockHome);
		return mockCtx;
	}

	protected LocalSlsbInvokerInterceptor configuredInterceptor(final Context mockCtx, final String jndiName)
			throws Exception {

		LocalSlsbInvokerInterceptor si = new LocalSlsbInvokerInterceptor();
		si.setJndiTemplate(new JndiTemplate() {
			@Override
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
