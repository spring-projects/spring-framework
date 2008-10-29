/*
 * Copyright 2002-2005 the original author or authors.
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

import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.servlet.ServletException;
import javax.transaction.TransactionRolledbackException;

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInvocation;
import org.easymock.MockControl;

import org.springframework.aop.ThrowsAdvice;
import org.springframework.aop.framework.MethodCounter;

/**
 * @author Rod Johnson
 */
public class ThrowsAdviceInterceptorTests extends TestCase {

	public void testNoHandlerMethods() {
		Object o = new Object();
		try {
			new ThrowsAdviceInterceptor(o);
			fail("Should require one handler method at least");
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}
	
	public void testNotInvoked() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		Object ret = new Object();
		MockControl mc = MockControl.createControl(MethodInvocation.class);
		MethodInvocation mi = (MethodInvocation) mc.getMock();
		mi.proceed();
		mc.setReturnValue(ret, 1);
		mc.replay();
		assertEquals(ret, ti.invoke(mi));
		assertEquals(0, th.getCalls());
		mc.verify();
	}
	
	public void testNoHandlerMethodForThrowable() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		assertEquals(2, ti.getHandlerMethodCount());
		Exception ex = new Exception();
		MockControl mc = MockControl.createControl(MethodInvocation.class);
		MethodInvocation mi = (MethodInvocation) mc.getMock();
		mi.proceed();
		mc.setThrowable(ex);
		mc.replay();
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}
		assertEquals(0, th.getCalls());
		mc.verify();
	}
	
	public void testCorrectHandlerUsed() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		ServletException ex = new ServletException();
		MockControl mc = MockControl.createControl(MethodInvocation.class);
		MethodInvocation mi = (MethodInvocation) mc.getMock();
		mi.getMethod();
		mc.setReturnValue(Object.class.getMethod("hashCode", (Class[]) null), 1);
		mi.getArguments();
		mc.setReturnValue(null);
		mi.getThis();
		mc.setReturnValue(new Object());
		mi.proceed();
		mc.setThrowable(ex);
		mc.replay();
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}
		assertEquals(1, th.getCalls());
		assertEquals(1, th.getCalls("servletException"));
		mc.verify();
	}
	
	public void testCorrectHandlerUsedForSubclass() throws Throwable {
		MyThrowsHandler th = new MyThrowsHandler();
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		// Extends RemoteException
		TransactionRolledbackException ex = new TransactionRolledbackException();
		MockControl mc = MockControl.createControl(MethodInvocation.class);
		MethodInvocation mi = (MethodInvocation) mc.getMock();
		mi.proceed();
		mc.setThrowable(ex);
		mc.replay();
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Exception caught) {
			assertEquals(ex, caught);
		}
		assertEquals(1, th.getCalls());
		assertEquals(1, th.getCalls("remoteException"));
		mc.verify();
	}
	
	public void testHandlerMethodThrowsException() throws Throwable {
		final Throwable t = new Throwable();
		MyThrowsHandler th = new MyThrowsHandler() {
			public void afterThrowing(RemoteException ex) throws Throwable {
				super.afterThrowing(ex);
				throw t;
			}
		};
		ThrowsAdviceInterceptor ti = new ThrowsAdviceInterceptor(th);
		// Extends RemoteException
		TransactionRolledbackException ex = new TransactionRolledbackException();
		MockControl mc = MockControl.createControl(MethodInvocation.class);
		MethodInvocation mi = (MethodInvocation) mc.getMock();
		mi.proceed();
		mc.setThrowable(ex);
		mc.replay();
		try {
			ti.invoke(mi);
			fail();
		}
		catch (Throwable caught) {
			assertEquals(t, caught);
		}
		assertEquals(1, th.getCalls());
		assertEquals(1, th.getCalls("remoteException"));
		mc.verify();
	}
	
	public static class MyThrowsHandler extends MethodCounter implements ThrowsAdvice {
		// Full method signature
		 public void afterThrowing(Method m, Object[] args, Object target, ServletException ex) {
		 	count("servletException");
		 }
		public void afterThrowing(RemoteException ex) throws Throwable {
			count("remoteException");
		 }
		
		/** Not valid, wrong number of arguments */
		public void afterThrowing(Method m, Exception ex) throws Throwable {
			throw new UnsupportedOperationException("Shouldn't be called");
		 }
	}
	
	public interface IEcho {
		int echoException(int i, Throwable t) throws Throwable;
		int getA();
		void setA(int a);
	}
	
	public static class Echo implements IEcho {
		private int a;
		
		public int echoException(int i, Throwable t) throws Throwable {
			if (t != null)
				throw t;
			return i;
		}
		public void setA(int a) {
			this.a = a;
		}
		public int getA() {
			return a;
		}
	}

}
