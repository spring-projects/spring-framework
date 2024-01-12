/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.framework.adapter;

import java.io.FileNotFoundException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.testfixture.advice.MyThrowsHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class ThrowsAdviceInterceptorTests {

	@Test
	void testNoHandlerMethods() {
		// should require one handler method at least
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
				new ThrowsAdviceInterceptor(new Object()));
	}

	@Test
	void testNotInvoked() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		Object ret = new Object();
		MethodInvocation mi = mock();
		given(mi.proceed()).willReturn(ret);
		assertThat(ti.invoke(mi)).isEqualTo(ret);
		assertThat(th.getCalls()).isEqualTo(0);
	}

	@Test
	void testNoHandlerMethodForThrowable() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		assertThat(ti.getHandlerMethodCount()).isEqualTo(2);
		Exception ex = new Exception();
		MethodInvocation mi = mock();
		given(mi.proceed()).willThrow(ex);
		assertThatException().isThrownBy(() -> ti.invoke(mi)).isSameAs(ex);
		assertThat(th.getCalls()).isEqualTo(0);
	}

	@Test
	void testCorrectHandlerUsed() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		FileNotFoundException ex = new FileNotFoundException();
		MethodInvocation mi = mock();
		given(mi.getMethod()).willReturn(Object.class.getMethod("hashCode"));
		given(mi.getThis()).willReturn(new Object());
		given(mi.proceed()).willThrow(ex);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> ti.invoke(mi)).isSameAs(ex);
		assertThat(th.getCalls()).isEqualTo(1);
		assertThat(th.getCalls("ioException")).isEqualTo(1);
	}

	@Test
	void testCorrectHandlerUsedForSubclass() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		// Extends RemoteException
		ConnectException ex = new ConnectException("");
		MethodInvocation mi = mock();
		given(mi.proceed()).willThrow(ex);
		assertThatExceptionOfType(ConnectException.class).isThrownBy(() -> ti.invoke(mi)).isSameAs(ex);
		assertThat(th.getCalls()).isEqualTo(1);
		assertThat(th.getCalls("remoteException")).isEqualTo(1);
	}

	@Test
	void testHandlerMethodThrowsException() throws Throwable {
		final Throwable t = new Throwable();

		MyThrowsHandler th = new MyThrowsHandler() {
			@Override
			public void afterThrowing(RemoteException ex) throws Throwable {
				super.afterThrowing(ex);
				throw t;
			}
		};

		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		// Extends RemoteException
		ConnectException ex = new ConnectException("");
		MethodInvocation mi = mock();
		given(mi.proceed()).willThrow(ex);
		assertThatExceptionOfType(Throwable.class).isThrownBy(() -> ti.invoke(mi)).isSameAs(t);
		assertThat(th.getCalls()).isEqualTo(1);
		assertThat(th.getCalls("remoteException")).isEqualTo(1);
	}

}
