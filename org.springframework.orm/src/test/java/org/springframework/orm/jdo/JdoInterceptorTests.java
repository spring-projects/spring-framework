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

package org.springframework.orm.jdo;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import junit.framework.TestCase;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.Invocation;
import org.aopalliance.intercept.MethodInvocation;
import org.easymock.MockControl;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 */
public class JdoInterceptorTests extends TestCase {

	public void testInterceptor() {
		MockControl pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
		PersistenceManagerFactory pmf = (PersistenceManagerFactory) pmfControl.getMock();
		MockControl pmControl = MockControl.createControl(PersistenceManager.class);
		PersistenceManager pm = (PersistenceManager) pmControl.getMock();
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();

		JdoInterceptor interceptor = new JdoInterceptor();
		interceptor.setPersistenceManagerFactory(pmf);
		try {
			interceptor.invoke(new TestInvocation(pmf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		pmfControl.verify();
		pmControl.verify();
	}

	public void testInterceptorWithPrebound() {
		MockControl pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
		PersistenceManagerFactory pmf = (PersistenceManagerFactory) pmfControl.getMock();
		MockControl pmControl = MockControl.createControl(PersistenceManager.class);
		PersistenceManager pm = (PersistenceManager) pmControl.getMock();
		pmfControl.replay();
		pmControl.replay();

		TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		JdoInterceptor interceptor = new JdoInterceptor();
		interceptor.setPersistenceManagerFactory(pmf);
		try {
			interceptor.invoke(new TestInvocation(pmf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(pmf);
		}

		pmfControl.verify();
		pmControl.verify();
	}


	private static class TestInvocation implements MethodInvocation {

		private PersistenceManagerFactory persistenceManagerFactory;

		public TestInvocation(PersistenceManagerFactory persistenceManagerFactory) {
			this.persistenceManagerFactory = persistenceManagerFactory;
		}

		public Object proceed() throws Throwable {
			if (!TransactionSynchronizationManager.hasResource(this.persistenceManagerFactory)) {
				throw new IllegalStateException("PersistenceManager not bound");
			}
			return null;
		}

		public Object[] getArguments() {
			return null;
		}

		public int getCurrentInterceptorIndex() {
			return 0;
		}

		public int getNumberOfInterceptors() {
			return 0;
		}

		public Interceptor getInterceptor(int i) {
			return null;
		}

		public Method getMethod() {
			return null;
		}

		public AccessibleObject getStaticPart() {
			return getMethod();
		}

		public Object getArgument(int i) {
			return null;
		}

		public void setArgument(int i, Object handler) {
		}

		public int getArgumentCount() {
			return 0;
		}

		public Object getThis() {
			return null;
		}

		public Object getProxy() {
			return null;
		}

		public Invocation cloneInstance() {
			return null;
		}

		public void release() {
		}
	}

}
