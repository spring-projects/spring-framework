/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.Invocation;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class JdoInterceptorTests {

	@Test
	public void testInterceptor() {
		PersistenceManagerFactory pmf = mock(PersistenceManagerFactory.class);
		PersistenceManager pm = mock(PersistenceManager.class);
		given(pmf.getPersistenceManager()).willReturn(pm);

		JdoInterceptor interceptor = new JdoInterceptor();
		interceptor.setPersistenceManagerFactory(pmf);
		try {
			interceptor.invoke(new TestInvocation(pmf));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		verify(pm).close();
	}

	@Test
	public void testInterceptorWithPrebound() {
		PersistenceManagerFactory pmf = mock(PersistenceManagerFactory.class);
		PersistenceManager pm = mock(PersistenceManager.class);

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
	}


	@SuppressWarnings("unused")
	private static class TestInvocation implements MethodInvocation {

		private PersistenceManagerFactory persistenceManagerFactory;

		public TestInvocation(PersistenceManagerFactory persistenceManagerFactory) {
			this.persistenceManagerFactory = persistenceManagerFactory;
		}

		@Override
		public Object proceed() throws Throwable {
			if (!TransactionSynchronizationManager.hasResource(this.persistenceManagerFactory)) {
				throw new IllegalStateException("PersistenceManager not bound");
			}
			return null;
		}

		@Override
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

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
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

		@Override
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
