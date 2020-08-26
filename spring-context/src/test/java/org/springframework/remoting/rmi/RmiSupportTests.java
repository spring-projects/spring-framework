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

package org.springframework.remoting.rmi;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.rmi.UnknownHostException;
import java.rmi.UnmarshalException;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.remoting.support.RemoteInvocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 * @since 16.05.2003
 */
public class RmiSupportTests {

	@Test
	public void rmiProxyFactoryBean() throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IRemoteBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		assertThat(factory.isSingleton()).as("Correct singleton value").isTrue();
		boolean condition = factory.getObject() instanceof IRemoteBean;
		assertThat(condition).isTrue();
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		proxy.setName("myName");
		assertThat(RemoteBean.name).isEqualTo("myName");
		assertThat(factory.counter).isEqualTo(1);
	}

	@Test
	public void rmiProxyFactoryBeanWithRemoteException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(RemoteException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithConnectException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(ConnectException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithConnectIOException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(ConnectIOException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithUnknownHostException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(UnknownHostException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithNoSuchObjectException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(NoSuchObjectException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithStubNotFoundException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(StubNotFoundException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithMarshalException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(MarshalException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithUnmarshalException() throws Exception {
		doTestRmiProxyFactoryBeanWithException(UnmarshalException.class);
	}

	private void doTestRmiProxyFactoryBeanWithException(Class<? extends Throwable> exceptionClass) throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IRemoteBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		boolean condition = factory.getObject() instanceof IRemoteBean;
		assertThat(condition).isTrue();
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		assertThatExceptionOfType(exceptionClass).isThrownBy(() ->
				proxy.setName(exceptionClass.getName()));
		assertThat(factory.counter).isEqualTo(1);
	}

	@Test
	public void rmiProxyFactoryBeanWithConnectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(ConnectException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithConnectIOExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(ConnectIOException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithUnknownHostExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(UnknownHostException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithNoSuchObjectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(NoSuchObjectException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithStubNotFoundExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithExceptionAndRefresh(StubNotFoundException.class);
	}

	private void doTestRmiProxyFactoryBeanWithExceptionAndRefresh(Class<? extends Throwable> exceptionClass) throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IRemoteBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.setRefreshStubOnConnectFailure(true);
		factory.afterPropertiesSet();
		boolean condition = factory.getObject() instanceof IRemoteBean;
		assertThat(condition).isTrue();
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		assertThatExceptionOfType(exceptionClass).isThrownBy(() ->
				proxy.setName(exceptionClass.getName()));
		assertThat(factory.counter).isEqualTo(2);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterface() throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		boolean condition = factory.getObject() instanceof IBusinessBean;
		assertThat(condition).isTrue();
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		boolean condition1 = proxy instanceof IRemoteBean;
		assertThat(condition1).isFalse();
		proxy.setName("myName");
		assertThat(RemoteBean.name).isEqualTo("myName");
		assertThat(factory.counter).isEqualTo(1);
	}

	@Test
	public void rmiProxyFactoryBeanWithWrongBusinessInterface() throws Exception {
		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IWrongBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		boolean condition = factory.getObject() instanceof IWrongBusinessBean;
		assertThat(condition).isTrue();
		IWrongBusinessBean proxy = (IWrongBusinessBean) factory.getObject();
		boolean condition1 = proxy instanceof IRemoteBean;
		assertThat(condition1).isFalse();
		assertThatExceptionOfType(RemoteProxyFailureException.class).isThrownBy(() ->
				proxy.setOtherName("name"))
			.withCauseInstanceOf(NoSuchMethodException.class)
			.withMessageContaining("setOtherName")
			.withMessageContaining("IWrongBusinessBean");
		assertThat(factory.counter).isEqualTo(1);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndRemoteException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				RemoteException.class, RemoteAccessException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndConnectException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				ConnectException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndConnectIOException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				ConnectIOException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndUnknownHostException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				UnknownHostException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndNoSuchObjectExceptionException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				NoSuchObjectException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndStubNotFoundException() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
				StubNotFoundException.class, RemoteConnectFailureException.class);
	}

	private void doTestRmiProxyFactoryBeanWithBusinessInterfaceAndException(
			Class<?> rmiExceptionClass, Class<? extends Throwable> springExceptionClass) throws Exception {

		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.afterPropertiesSet();
		boolean condition = factory.getObject() instanceof IBusinessBean;
		assertThat(condition).isTrue();
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		boolean condition1 = proxy instanceof IRemoteBean;
		assertThat(condition1).isFalse();
		assertThatExceptionOfType(springExceptionClass).isThrownBy(() ->
				proxy.setName(rmiExceptionClass.getName()));
		assertThat(factory.counter).isEqualTo(1);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndRemoteExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				RemoteException.class, RemoteAccessException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndConnectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				ConnectException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndConnectIOExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				ConnectIOException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndUnknownHostExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				UnknownHostException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndNoSuchObjectExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				NoSuchObjectException.class, RemoteConnectFailureException.class);
	}

	@Test
	public void rmiProxyFactoryBeanWithBusinessInterfaceAndStubNotFoundExceptionAndRefresh() throws Exception {
		doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
				StubNotFoundException.class, RemoteConnectFailureException.class);
	}

	private void doTestRmiProxyFactoryBeanWithBusinessInterfaceAndExceptionAndRefresh(
			Class<?> rmiExceptionClass, Class<? extends Throwable> springExceptionClass) throws Exception {

		CountingRmiProxyFactoryBean factory = new CountingRmiProxyFactoryBean();
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl("rmi://localhost:1090/test");
		factory.setRefreshStubOnConnectFailure(true);
		factory.afterPropertiesSet();
		boolean condition = factory.getObject() instanceof IBusinessBean;
		assertThat(condition).isTrue();
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		boolean condition1 = proxy instanceof IRemoteBean;
		assertThat(condition1).isFalse();
		assertThatExceptionOfType(springExceptionClass).isThrownBy(() ->
				proxy.setName(rmiExceptionClass.getName()));
		boolean isRemoteConnectFailure = RemoteConnectFailureException.class.isAssignableFrom(springExceptionClass);
		assertThat(factory.counter).isEqualTo(isRemoteConnectFailure ? 2 : 1);
	}

	@Test
	public void rmiClientInterceptorRequiresUrl() throws Exception{
		RmiClientInterceptor client = new RmiClientInterceptor();
		client.setServiceInterface(IRemoteBean.class);
		assertThatIllegalArgumentException().isThrownBy(client::afterPropertiesSet);
	}

	@Test
	public void remoteInvocation() throws NoSuchMethodException {
		// let's see if the remote invocation object works

		final RemoteBean rb = new RemoteBean();
		final Method setNameMethod = rb.getClass().getDeclaredMethod("setName", String.class);

		MethodInvocation mi = new MethodInvocation() {
			@Override
			public Method getMethod() {
				return setNameMethod;
			}
			@Override
			public Object[] getArguments() {
				return new Object[] {"bla"};
			}
			@Override
			public Object proceed() throws Throwable {
				throw new UnsupportedOperationException();
			}
			@Override
			public Object getThis() {
				return rb;
			}
			@Override
			public AccessibleObject getStaticPart() {
				return setNameMethod;
			}
		};

		RemoteInvocation inv = new RemoteInvocation(mi);

		assertThat(inv.getMethodName()).isEqualTo("setName");
		assertThat(inv.getArguments()[0]).isEqualTo("bla");
		assertThat(inv.getParameterTypes()[0]).isEqualTo(String.class);

		// this is a bit BS, but we need to test it
		inv = new RemoteInvocation();
		inv.setArguments(new Object[] { "bla" });
		assertThat(inv.getArguments()[0]).isEqualTo("bla");
		inv.setMethodName("setName");
		assertThat(inv.getMethodName()).isEqualTo("setName");
		inv.setParameterTypes(new Class<?>[] {String.class});
		assertThat(inv.getParameterTypes()[0]).isEqualTo(String.class);

		inv = new RemoteInvocation("setName", new Class<?>[] {String.class}, new Object[] {"bla"});
		assertThat(inv.getArguments()[0]).isEqualTo("bla");
		assertThat(inv.getMethodName()).isEqualTo("setName");
		assertThat(inv.getParameterTypes()[0]).isEqualTo(String.class);
	}

	@Test
	public void rmiInvokerWithSpecialLocalMethods() throws Exception {
		String serviceUrl = "rmi://localhost:1090/test";
		RmiProxyFactoryBean factory = new RmiProxyFactoryBean() {
			@Override
			protected Remote lookupStub() {
				return new RmiInvocationHandler() {
					@Override
					public String getTargetInterfaceName() {
						return null;
					}
					@Override
					public Object invoke(RemoteInvocation invocation) throws RemoteException {
						throw new RemoteException();
					}
				};
			}
		};
		factory.setServiceInterface(IBusinessBean.class);
		factory.setServiceUrl(serviceUrl);
		factory.afterPropertiesSet();
		IBusinessBean proxy = (IBusinessBean) factory.getObject();

		// shouldn't go through to remote service
		assertThat(proxy.toString().contains("RMI invoker")).isTrue();
		assertThat(proxy.toString().contains(serviceUrl)).isTrue();
		assertThat(proxy.hashCode()).isEqualTo(proxy.hashCode());
		assertThat(proxy.equals(proxy)).isTrue();

		// should go through
		assertThatExceptionOfType(RemoteAccessException.class).isThrownBy(() ->
				proxy.setName("test"));
	}


	private static class CountingRmiProxyFactoryBean extends RmiProxyFactoryBean {

		private int counter = 0;

		@Override
		protected Remote lookupStub() {
			counter++;
			return new RemoteBean();
		}
	}


	public interface IBusinessBean {

		void setName(String name);
	}


	public interface IWrongBusinessBean {

		void setOtherName(String name);
	}


	public interface IRemoteBean extends Remote {

		void setName(String name) throws RemoteException;
	}


	public static class RemoteBean implements IRemoteBean {

		private static String name;

		@Override
		public void setName(String nam) throws RemoteException {
			if (nam != null && nam.endsWith("Exception")) {
				RemoteException rex;
				try {
					Class<?> exClass = Class.forName(nam);
					Constructor<?> ctor = exClass.getConstructor(String.class);
					rex = (RemoteException) ctor.newInstance("myMessage");
				}
				catch (Exception ex) {
					throw new RemoteException("Illegal exception class name: " + nam, ex);
				}
				throw rex;
			}
			name = nam;
		}
	}

}
