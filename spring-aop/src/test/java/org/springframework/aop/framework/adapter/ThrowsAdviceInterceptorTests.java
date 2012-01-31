/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.aop.framework.adapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import javax.transaction.TransactionRolledbackException;

import org.aopalliance.intercept.MethodInvocation;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Test;
import test.aop.MethodCounter;

import org.springframework.aop.ThrowsAdvice;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class ThrowsAdviceInterceptorTests {

	@Test(expected=IllegalArgumentException.class)
	public void testNoHandlerMethods() {
		// should require one handler method at least
		new ThrowsAdviceInterceptor(new Object());
	}

	@Test
	public void testNotInvoked() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		Object ret = new Object();
		MethodInvocation mi = createMock(MethodInvocation.class);
		expect(mi.proceed()).andReturn(ret);
		replay(mi);
		assertEquals(ret, ti.invoke(mi));
		assertEquals(0, th.getCalls());
		verify(mi);
	}

	@Test
	public void testNoHandlerMethodForThrowable() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		assertEquals(2, ti.getHandlerMethodCount());
		Exception ex = new Exception();
		MethodInvocation mi = createMock(MethodInvocation.class);
		expect(mi.proceed()).andThrow(ex);
		replay(mi);
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}
		assertEquals(0, th.getCalls());
		verify(mi);
	}

	@Test
	public void testCorrectHandlerUsed() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		FileNotFoundException ex = new FileNotFoundException();
		MethodInvocation mi = createMock(MethodInvocation.class);
		expect(mi.getMethod()).andReturn(Object.class.getMethod("hashCode", (Class[]) null));
		expect(mi.getArguments()).andReturn(null);
		expect(mi.getThis()).andReturn(new Object());
		expect(mi.proceed()).andThrow(ex);
		replay(mi);
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}
		assertEquals(1, th.getCalls());
		assertEquals(1, th.getCalls("ioException"));
		verify(mi);
	}

	@Test
	public void testCorrectHandlerUsedForSubclass() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		// Extends RemoteException
		TransactionRolledbackException ex = new TransactionRolledbackException();
		MethodInvocation mi = createMock(MethodInvocation.class);
		expect(mi.proceed()).andThrow(ex);
		replay(mi);
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}
		assertEquals(1, th.getCalls());
		assertEquals(1, th.getCalls("remoteException"));
		verify(mi);
	}

	@Test
	public void testHandlerMethodThrowsException() throws Throwable {
		final Throwable t = new Throwable();

		@SuppressWarnings("serial")
		MyThrowsHandler th = new MyThrowsHandler() {
			public void afterThrowing(RemoteException ex) throws Throwable {
				super.afterThrowing(ex);
				throw t;
			}
		};

		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		// Extends RemoteException
		TransactionRolledbackException ex = new TransactionRolledbackException();
		MethodInvocation mi = createMock(MethodInvocation.class);
		expect(mi.proceed()).andThrow(ex);
		replay(mi);
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Throwable caught) {
			assertEquals(t, caught);
		}
		assertEquals(1, th.getCalls());
		assertEquals(1, th.getCalls("remoteException"));
		verify(mi);
	}

	@SuppressWarnings("serial")
	private static class MyThrowsHandler extends MethodCounter implements ThrowsAdvice {
		// Full method signature
		public void afterThrowing(Method m, Object[] args, Object target, IOException ex) {
			count("ioException");
		}
		public void afterThrowing(RemoteException ex) throws Throwable {
			count("remoteException");
		}

		/** Not valid, wrong number of arguments */
		public void afterThrowing(Method m, Exception ex) throws Throwable {
			throw new UnsupportedOperationException("Shouldn't be called");
		}
	}

}
