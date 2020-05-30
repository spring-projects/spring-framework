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

import java.lang.reflect.Proxy;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.naming.NamingException;

import org.junit.jupiter.api.Test;

import org.springframework.jndi.JndiTemplate;
import org.springframework.remoting.RemoteAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 21.05.2003
 */
public class SimpleRemoteStatelessSessionProxyFactoryBeanTests extends SimpleRemoteSlsbInvokerInterceptorTests {

	@Override
	protected SimpleRemoteSlsbInvokerInterceptor createInterceptor() {
		return new SimpleRemoteStatelessSessionProxyFactoryBean();
	}

	@Override
	protected Object configuredProxy(SimpleRemoteSlsbInvokerInterceptor si, Class<?> ifc) throws NamingException {
		SimpleRemoteStatelessSessionProxyFactoryBean fb = (SimpleRemoteStatelessSessionProxyFactoryBean) si;
		fb.setBusinessInterface(ifc);
		fb.afterPropertiesSet();
		return fb.getObject();
	}

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
			public Object lookup(String name) {
				// parameterize
				assertThat(name.equals("java:comp/env/" + jndiName)).isTrue();
				return home;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setResourceRef(true);
		fb.setBusinessInterface(MyBusinessMethods.class);
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertThat(Proxy.isProxyClass(mbm.getClass())).isTrue();
		assertThat(mbm.getValue()).as("Returns expected value").isEqualTo(value);
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
			public Object lookup(String name) {
				// parameterize
				assertThat(name.equals("java:comp/env/" + jndiName)).isTrue();
				return myEjb;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setResourceRef(true);
		fb.setBusinessInterface(MyBusinessMethods.class);
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertThat(Proxy.isProxyClass(mbm.getClass())).isTrue();
		assertThat(mbm.getValue()).as("Returns expected value").isEqualTo(value);
	}

	@Override
	@Test
	public void testRemoteException() throws Exception {
		final RemoteException rex = new RemoteException();
		final String jndiName = "foo";

		MyEjb myEjb = mock(MyEjb.class);
		given(myEjb.getValue()).willThrow(rex);
		// TODO might want to control this behaviour...
		// Do we really want to call remove after a remote exception?

		final MyHome home = mock(MyHome.class);
		given(home.create()).willReturn(myEjb);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) {
				// parameterize
				assertThat(name.equals("java:comp/env/" + jndiName)).isTrue();
				return home;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		fb.setResourceRef(true);
		fb.setBusinessInterface(MyBusinessMethods.class);
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertThat(Proxy.isProxyClass(mbm.getClass())).isTrue();
		assertThatExceptionOfType(RemoteException.class).isThrownBy(
				mbm::getValue)
			.satisfies(ex -> assertThat(ex).isSameAs(rex));
		verify(myEjb).remove();
	}

	@Test
	public void testCreateException() throws Exception {
		final String jndiName = "foo";

		final CreateException cex = new CreateException();
		final MyHome home = mock(MyHome.class);
		given(home.create()).willThrow(cex);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) {
				// parameterize
				assertThat(name.equals(jndiName)).isTrue();
				return home;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		// rely on default setting of resourceRef=false, no auto addition of java:/comp/env prefix
		fb.setBusinessInterface(MyBusinessMethods.class);
		assertThat(MyBusinessMethods.class).isEqualTo(fb.getBusinessInterface());
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyBusinessMethods mbm = (MyBusinessMethods) fb.getObject();
		assertThat(Proxy.isProxyClass(mbm.getClass())).isTrue();
		assertThatExceptionOfType(RemoteException.class).isThrownBy(mbm::getValue);
	}

	@Test
	public void testCreateExceptionWithLocalBusinessInterface() throws Exception {
		final String jndiName = "foo";

		final CreateException cex = new CreateException();
		final MyHome home = mock(MyHome.class);
		given(home.create()).willThrow(cex);

		JndiTemplate jt = new JndiTemplate() {
			@Override
			public Object lookup(String name) {
				// parameterize
				assertThat(name.equals(jndiName)).isTrue();
				return home;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		// rely on default setting of resourceRef=false, no auto addition of java:/comp/env prefix
		fb.setBusinessInterface(MyLocalBusinessMethods.class);
		assertThat(MyLocalBusinessMethods.class).isEqualTo(fb.getBusinessInterface());
		fb.setJndiTemplate(jt);

		// Need lifecycle methods
		fb.afterPropertiesSet();

		MyLocalBusinessMethods mbm = (MyLocalBusinessMethods) fb.getObject();
		assertThat(Proxy.isProxyClass(mbm.getClass())).isTrue();
		assertThatExceptionOfType(RemoteAccessException.class).isThrownBy(
				mbm::getValue)
			.withCause(cex);
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
				assertThat(name.equals(jndiName)).isTrue();
				return home;
			}
		};

		SimpleRemoteStatelessSessionProxyFactoryBean fb = new SimpleRemoteStatelessSessionProxyFactoryBean();
		fb.setJndiName(jndiName);
		// rely on default setting of resourceRef=false, no auto addition of java:/comp/env prefix
		// Don't set business interface
		fb.setJndiTemplate(jt);

		// Check it's a singleton
		assertThat(fb.isSingleton()).isTrue();

		assertThatIllegalArgumentException().isThrownBy(
				fb::afterPropertiesSet)
			.withMessageContaining("businessInterface");

		// Expect no methods on home
		verifyNoInteractions(home);
	}


	protected interface MyHome extends EJBHome {

		MyBusinessMethods create() throws CreateException, RemoteException;
	}


	protected interface MyBusinessMethods  {

		int getValue() throws RemoteException;
	}


	protected interface MyLocalBusinessMethods  {

		int getValue();
	}


	protected interface MyEjb extends EJBObject, MyBusinessMethods {
	}

}
