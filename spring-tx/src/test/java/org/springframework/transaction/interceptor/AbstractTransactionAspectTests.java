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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.TransactionAspectSupport.TransactionInfo;
import org.springframework.transaction.testfixture.MockCallbackPreferringTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Mock object based tests for transaction aspects. A true unit test in that it
 * tests how the transaction aspect uses the PlatformTransactionManager helper,
 * rather than indirectly testing the helper implementation.
 *
 * <p>This is a superclass to allow testing both the AOP Alliance MethodInterceptor
 * and the AspectJ aspect.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.03.2003
 */
public abstract class AbstractTransactionAspectTests {

	protected Method getNameMethod;

	protected Method setNameMethod;

	protected Method exceptionalMethod;


	@BeforeEach
	void setup() throws Exception {
		getNameMethod = ITestBean.class.getMethod("getName");
		setNameMethod = ITestBean.class.getMethod("setName", String.class);
		exceptionalMethod = ITestBean.class.getMethod("exceptional", Throwable.class);
	}


	@Test
	void noTransaction() throws Exception {
		PlatformTransactionManager ptm = mock();

		TestBean tb = new TestBean();
		TransactionAttributeSource tas = new MapTransactionAttributeSource();

		// All the methods in this class use the advised() template method
		// to obtain a transaction object, configured with the given PlatformTransactionManager
		// and transaction attribute source
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		checkTransactionStatus(false);
		itb.getName();
		checkTransactionStatus(false);

		// expect no calls
		verifyNoInteractions(ptm);
	}

	/**
	 * Check that a transaction is created and committed.
	 */
	@Test
	void transactionShouldSucceed() throws Exception {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(getNameMethod, txatt);

		TransactionStatus status = mock();
		PlatformTransactionManager ptm = mock();
		// expect a transaction
		given(ptm.getTransaction(txatt)).willReturn(status);

		TestBean tb = new TestBean();
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		checkTransactionStatus(false);
		itb.getName();
		checkTransactionStatus(false);

		verify(ptm).commit(status);
	}

	/**
	 * Check that a transaction is created and committed using
	 * CallbackPreferringPlatformTransactionManager.
	 */
	@Test
	void transactionShouldSucceedWithCallbackPreference() throws Exception {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(getNameMethod, txatt);

		MockCallbackPreferringTransactionManager ptm = new MockCallbackPreferringTransactionManager();

		TestBean tb = new TestBean();
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		checkTransactionStatus(false);
		itb.getName();
		checkTransactionStatus(false);

		assertThat(ptm.getDefinition()).isSameAs(txatt);
		assertThat(ptm.getStatus().isRollbackOnly()).isFalse();
	}

	@Test
	void transactionExceptionPropagatedWithCallbackPreference() throws Throwable {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(exceptionalMethod, txatt);

		MockCallbackPreferringTransactionManager ptm = new MockCallbackPreferringTransactionManager();

		TestBean tb = new TestBean();
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		checkTransactionStatus(false);
		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() ->
				itb.exceptional(new OptimisticLockingFailureException("")));
		checkTransactionStatus(false);

		assertThat(ptm.getDefinition()).isSameAs(txatt);
		assertThat(ptm.getStatus().isRollbackOnly()).isFalse();
	}

	/**
	 * Check that two transactions are created and committed.
	 */
	@Test
	void twoTransactionsShouldSucceed() throws Exception {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		MapTransactionAttributeSource tas1 = new MapTransactionAttributeSource();
		tas1.register(getNameMethod, txatt);
		MapTransactionAttributeSource tas2 = new MapTransactionAttributeSource();
		tas2.register(setNameMethod, txatt);

		TransactionStatus status = mock();
		PlatformTransactionManager ptm = mock();
		// expect a transaction
		given(ptm.getTransaction(txatt)).willReturn(status);

		TestBean tb = new TestBean();
		ITestBean itb = (ITestBean) advised(tb, ptm, new TransactionAttributeSource[] {tas1, tas2});

		checkTransactionStatus(false);
		itb.getName();
		checkTransactionStatus(false);
		itb.setName("myName");
		checkTransactionStatus(false);

		verify(ptm, times(2)).commit(status);
	}

	/**
	 * Check that a transaction is created and committed.
	 */
	@Test
	void transactionShouldSucceedWithNotNew() throws Exception {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(getNameMethod, txatt);

		TransactionStatus status = mock();
		PlatformTransactionManager ptm = mock();
		// expect a transaction
		given(ptm.getTransaction(txatt)).willReturn(status);

		TestBean tb = new TestBean();
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		checkTransactionStatus(false);
		// verification!?
		itb.getName();
		checkTransactionStatus(false);

		verify(ptm).commit(status);
	}

	@Test
	void enclosingTransactionWithNonTransactionMethodOnAdvisedInside() throws Throwable {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(exceptionalMethod, txatt);

		TransactionStatus status = mock();
		PlatformTransactionManager ptm = mock();
		// Expect a transaction
		given(ptm.getTransaction(txatt)).willReturn(status);

		final String spouseName = "innerName";

		TestBean outer = new TestBean() {
			@Override
			public void exceptional(Throwable t) {
				TransactionInfo ti = TransactionAspectSupport.currentTransactionInfo();
				assertThat(ti.hasTransaction()).isTrue();
				assertThat(getSpouse().getName()).isEqualTo(spouseName);
			}
		};
		TestBean inner = new TestBean() {
			@Override
			public String getName() {
				// Assert that we're in the inner proxy
				TransactionInfo ti = TransactionAspectSupport.currentTransactionInfo();
				assertThat(ti.hasTransaction()).isFalse();
				return spouseName;
			}
		};

		ITestBean outerProxy = (ITestBean) advised(outer, ptm, tas);
		ITestBean innerProxy = (ITestBean) advised(inner, ptm, tas);
		outer.setSpouse(innerProxy);

		checkTransactionStatus(false);

		// Will invoke inner.getName, which is non-transactional
		outerProxy.exceptional(null);

		checkTransactionStatus(false);

		verify(ptm).commit(status);
	}

	@Test
	void enclosingTransactionWithNestedTransactionOnAdvisedInside() throws Throwable {
		final TransactionAttribute outerTxatt = new DefaultTransactionAttribute();
		final TransactionAttribute innerTxatt = new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_NESTED);

		Method outerMethod = exceptionalMethod;
		Method innerMethod = getNameMethod;
		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(outerMethod, outerTxatt);
		tas.register(innerMethod, innerTxatt);

		TransactionStatus outerStatus = mock();
		TransactionStatus innerStatus = mock();

		PlatformTransactionManager ptm = mock();
		// Expect a transaction
		given(ptm.getTransaction(outerTxatt)).willReturn(outerStatus);
		given(ptm.getTransaction(innerTxatt)).willReturn(innerStatus);

		final String spouseName = "innerName";

		TestBean outer = new TestBean() {
			@Override
			public void exceptional(Throwable t) {
				TransactionInfo ti = TransactionAspectSupport.currentTransactionInfo();
				assertThat(ti.hasTransaction()).isTrue();
				assertThat(ti.getTransactionAttribute()).isEqualTo(outerTxatt);
				assertThat(getSpouse().getName()).isEqualTo(spouseName);
			}
		};
		TestBean inner = new TestBean() {
			@Override
			public String getName() {
				// Assert that we're in the inner proxy
				TransactionInfo ti = TransactionAspectSupport.currentTransactionInfo();
				// Has nested transaction
				assertThat(ti.hasTransaction()).isTrue();
				assertThat(ti.getTransactionAttribute()).isEqualTo(innerTxatt);
				return spouseName;
			}
		};

		ITestBean outerProxy = (ITestBean) advised(outer, ptm, tas);
		ITestBean innerProxy = (ITestBean) advised(inner, ptm, tas);
		outer.setSpouse(innerProxy);

		checkTransactionStatus(false);

		// Will invoke inner.getName, which is non-transactional
		outerProxy.exceptional(null);

		checkTransactionStatus(false);

		verify(ptm).commit(innerStatus);
		verify(ptm).commit(outerStatus);
	}

	@Test
	void rollbackOnCheckedException() throws Throwable {
		doTestRollbackOnException(new Exception(), true, false);
	}

	@Test
	void noRollbackOnCheckedException() throws Throwable {
		doTestRollbackOnException(new Exception(), false, false);
	}

	@Test
	void rollbackOnUncheckedException() throws Throwable {
		doTestRollbackOnException(new RuntimeException(), true, false);
	}

	@Test
	void noRollbackOnUncheckedException() throws Throwable {
		doTestRollbackOnException(new RuntimeException(), false, false);
	}

	@Test
	void rollbackOnCheckedExceptionWithRollbackException() throws Throwable {
		doTestRollbackOnException(new Exception(), true, true);
	}

	@Test
	void noRollbackOnCheckedExceptionWithRollbackException() throws Throwable {
		doTestRollbackOnException(new Exception(), false, true);
	}

	@Test
	void rollbackOnUncheckedExceptionWithRollbackException() throws Throwable {
		doTestRollbackOnException(new RuntimeException(), true, true);
	}

	@Test
	void noRollbackOnUncheckedExceptionWithRollbackException() throws Throwable {
		doTestRollbackOnException(new RuntimeException(), false, true);
	}

	/**
	 * Check that the given exception thrown by the target can produce the
	 * desired behavior with the appropriate transaction attribute.
	 * @param ex exception to be thrown by the target
	 * @param shouldRollback whether this should cause a transaction rollback
	 */
	protected void doTestRollbackOnException(
			final Exception ex, final boolean shouldRollback, boolean rollbackException) throws Exception {

		TransactionAttribute txatt = new DefaultTransactionAttribute() {
			@Override
			public boolean rollbackOn(Throwable t) {
				assertThat(t).isSameAs(ex);
				return shouldRollback;
			}
		};

		Method m = exceptionalMethod;
		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(m, txatt);

		TransactionStatus status = mock();
		PlatformTransactionManager ptm = mock();
		// Gets additional call(s) from TransactionControl

		given(ptm.getTransaction(txatt)).willReturn(status);

		TransactionSystemException tex = new TransactionSystemException("system exception");
		if (rollbackException) {
			if (shouldRollback) {
				willThrow(tex).given(ptm).rollback(status);
			}
			else {
				willThrow(tex).given(ptm).commit(status);
			}
		}

		TestBean tb = new TestBean();
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		try {
			itb.exceptional(ex);
			fail("Should have thrown exception");
		}
		catch (Throwable t) {
			if (rollbackException) {
				assertThat(t).as("Caught wrong exception").isEqualTo(tex);
			}
			else {
				assertThat(t).as("Caught wrong exception").isEqualTo(ex);
			}
		}

		if (!rollbackException) {
			if (shouldRollback) {
				verify(ptm).rollback(status);
			}
			else {
				verify(ptm).commit(status);
			}
		}
	}

	/**
	 * Test that TransactionStatus.setRollbackOnly works.
	 */
	@Test
	void programmaticRollback() throws Exception {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		Method m = getNameMethod;
		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(m, txatt);

		TransactionStatus status = mock();
		PlatformTransactionManager ptm = mock();

		given(ptm.getTransaction(txatt)).willReturn(status);

		final String name = "jenny";
		TestBean tb = new TestBean() {
			@Override
			public String getName() {
				TransactionStatus txStatus = TransactionInterceptor.currentTransactionStatus();
				txStatus.setRollbackOnly();
				return name;
			}
		};

		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		// verification!?
		assertThat(itb.getName()).isEqualTo(name);

		verify(ptm).commit(status);
	}

	/**
	 * Simulate a transaction infrastructure failure.
	 * Shouldn't invoke target method.
	 */
	@Test
	void cannotCreateTransaction() throws Exception {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		Method m = getNameMethod;
		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(m, txatt);

		PlatformTransactionManager ptm = mock();
		// Expect a transaction
		CannotCreateTransactionException ex = new CannotCreateTransactionException("foobar", null);
		given(ptm.getTransaction(txatt)).willThrow(ex);

		TestBean tb = new TestBean() {
			@Override
			public String getName() {
				throw new UnsupportedOperationException(
						"Shouldn't have invoked target method when couldn't create transaction for transactional method");
			}
		};
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		try {
			itb.getName();
			fail("Shouldn't have invoked method");
		}
		catch (CannotCreateTransactionException thrown) {
			assertThat(thrown).isSameAs(ex);
		}
	}

	/**
	 * Simulate failure of the underlying transaction infrastructure to commit.
	 * Check that the target method was invoked, but that the transaction
	 * infrastructure exception was thrown to the client
	 */
	@Test
	void cannotCommitTransaction() throws Exception {
		TransactionAttribute txatt = new DefaultTransactionAttribute();

		Method m = setNameMethod;
		MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
		tas.register(m, txatt);
		// Method m2 = getNameMethod;
		// No attributes for m2

		PlatformTransactionManager ptm = mock();

		TransactionStatus status = mock();
		given(ptm.getTransaction(txatt)).willReturn(status);
		UnexpectedRollbackException ex = new UnexpectedRollbackException("foobar", null);
		willThrow(ex).given(ptm).commit(status);

		TestBean tb = new TestBean();
		ITestBean itb = (ITestBean) advised(tb, ptm, tas);

		String name = "new name";
		try {
			itb.setName(name);
			fail("Shouldn't have succeeded");
		}
		catch (UnexpectedRollbackException thrown) {
			assertThat(thrown).isSameAs(ex);
		}

		// Should have invoked target and changed name
		assertThat(itb.getName()).isSameAs(name);
	}

	protected void checkTransactionStatus(boolean expected) {
		try {
			TransactionInterceptor.currentTransactionStatus();
			if (!expected) {
				fail("Should have thrown NoTransactionException");
			}
		}
		catch (NoTransactionException ex) {
			if (expected) {
				fail("Should have current TransactionStatus");
			}
		}
	}


	protected Object advised(
			Object target, PlatformTransactionManager ptm, TransactionAttributeSource[] tas) throws Exception {

		return advised(target, ptm, new CompositeTransactionAttributeSource(tas));
	}

	/**
	 * Subclasses must implement this to create an advised object based on the
	 * given target. In the case of AspectJ, the  advised object will already
	 * have been created, as there's no distinction between target and proxy.
	 * In the case of Spring's own AOP framework, a proxy must be created
	 * using a suitably configured transaction interceptor
	 * @param target the target if there's a distinct target. If not (AspectJ),
	 * return target.
	 * @return transactional advised object
	 */
	protected abstract Object advised(
			Object target, PlatformTransactionManager ptm, TransactionAttributeSource tas) throws Exception;

}
