/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.transaction.jta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Adapter for a JTA UserTransaction handle, taking a JTA
 * {@link jakarta.transaction.TransactionManager} reference and creating
 * a JTA {@link jakarta.transaction.UserTransaction} handle for it.
 *
 * <p>The JTA UserTransaction interface is an exact subset of the JTA
 * TransactionManager interface. Unfortunately, it does not serve as
 * super-interface of TransactionManager, though, which requires an
 * adapter such as this class to be used when intending to talk to
 * a TransactionManager handle through the UserTransaction interface.
 *
 * <p>Used internally by Spring's {@link JtaTransactionManager} for certain
 * scenarios. Not intended for direct use in application code.
 *
 * <p>As of Spring Framework 7.1, this adapter supports the JTA 2.1
 * read-only methods as well.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 */
public class UserTransactionAdapter implements UserTransaction {

	// JTA 2.1 TransactionManager#begin(boolean) method available?
	private static final @Nullable Method beginWithReadOnlyMethod =
			ClassUtils.getMethodIfAvailable(TransactionManager.class, "begin", boolean.class);

	// JTA 2.1 Transaction#begin(boolean) method available?
	private static final @Nullable Method isReadOnlyMethod =
			ClassUtils.getMethodIfAvailable(Transaction.class, "isReadOnly");

	private final TransactionManager transactionManager;


	/**
	 * Create a new UserTransactionAdapter for the given TransactionManager.
	 * @param transactionManager the JTA TransactionManager to wrap
	 */
	public UserTransactionAdapter(TransactionManager transactionManager) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		this.transactionManager = transactionManager;
	}

	/**
	 * Return the JTA TransactionManager that this adapter delegates to.
	 */
	public final TransactionManager getTransactionManager() {
		return this.transactionManager;
	}


	@Override
	public void setTransactionTimeout(int timeout) throws SystemException {
		this.transactionManager.setTransactionTimeout(timeout);
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		this.transactionManager.begin();
	}

	/**
	 * JTA 2.1 begin(boolean) method.
	 * @since 7.1
	 */
	// @Override - on JTA 2.1
	public void begin(boolean isReadOnly) throws NotSupportedException, SystemException {
		if (beginWithReadOnlyMethod == null) {
			if (isReadOnly) {
				throw new NotSupportedException("begin(true) requires JTA 2.1");
			}
			this.transactionManager.begin();
			return;
		}

		try {
			beginWithReadOnlyMethod.invoke(this.transactionManager, isReadOnly);
		}
		catch (Exception ex) {
			if (ex instanceof InvocationTargetException ite) {
				if (ite.getTargetException() instanceof NotSupportedException nse) {
					throw nse;
				}
				if (ite.getTargetException() instanceof SystemException se) {
					throw se;
				}
			}
			ReflectionUtils.handleReflectionException(ex);
		}
	}

	@Override
	public void commit()
			throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, SystemException {

		this.transactionManager.commit();
	}

	@Override
	public void rollback() throws SecurityException, SystemException {
		this.transactionManager.rollback();
	}

	@Override
	public void setRollbackOnly() throws SystemException {
		this.transactionManager.setRollbackOnly();
	}

	@Override
	public int getStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}

	/**
	 * JTA 2.1 isReadOnly() method.
	 * @since 7.1
	 */
	// @Override - on JTA 2.1
	public boolean isReadOnly() throws SystemException {
		if (isReadOnlyMethod != null) {
			Transaction transaction = this.transactionManager.getTransaction();
			try {
				return (Boolean) isReadOnlyMethod.invoke(transaction);
			}
			catch (Exception ex) {
				if (ex instanceof InvocationTargetException ite &&
						ite.getTargetException() instanceof SystemException se) {
					throw se;
				}
				ReflectionUtils.handleReflectionException(ex);
			}
		}

		return false;
	}

}
