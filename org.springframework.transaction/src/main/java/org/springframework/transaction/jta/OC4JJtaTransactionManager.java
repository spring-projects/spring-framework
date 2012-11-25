/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.jta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.ClassUtils;

/**
 * Special {@link JtaTransactionManager} variant for Oracle OC4J (10.1.3 and higher).
 * Supports the full power of Spring's transaction definitions on OC4J's
 * transaction coordinator, <i>beyond standard JTA</i>: transaction names
 * and per-transaction isolation levels.
 *
 * <p>Uses OC4J's special <code>begin(name)</code> method to start a JTA transaction,
 * in orderto make <b>Spring-driven transactions visible in OC4J's transaction
 * monitor</b>. In case of Spring's declarative transactions, the exposed name will
 * (by default) be the fully-qualified class name + "." + method name.
 *
 * <p>Supports a <b>per-transaction isolation level</b> through OC4J's corresponding
 * <code>OC4JTransaction.setTransactionIsolation(int)</code> method. This will
 * apply the specified isolation level (e.g. ISOLATION_SERIALIZABLE) to all
 * JDBC Connections that participate in the given transaction.
 *
 * <p>Automatically detects the available OC4J server version and adapts accordingly.
 * Supports the "com.evermind.server" package in OC4J 10.1.3.2 as well as the
 * "oracle.j2ee.transaction" package in later OC4J versions.
 *
 * <p>By default, the JTA UserTransaction and TransactionManager handles are
 * fetched directly from OC4J's <code>TransactionUtility</code> in 10.1.3.2+.
 * This can be overridden by specifying "userTransaction"/"userTransactionName"
 * and "transactionManager"/"transactionManagerName", passing in existing handles
 * or specifying corresponding JNDI locations to look up.
 *
 * <p>Thanks to Oracle for donating the original version of this extended OC4J
 * integration code to the Spring project!
 *
 * <p><b>NOTE:</b> This class will be deprecated as of Spring 3.2, in favor of
 * {@link WebLogicJtaTransactionManager} since Oracle end-of-lifed OC4J in favor of WebLogic.
 *
 * @author Paul Parkinson
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see org.springframework.transaction.TransactionDefinition#getName
 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel
 * @see oracle.j2ee.transaction.OC4JTransactionManager#begin(String)
 * @see oracle.j2ee.transaction.OC4JTransaction#setTransactionIsolation
 * @see oracle.j2ee.transaction.TransactionUtility
 */
public class OC4JJtaTransactionManager extends JtaTransactionManager {

	private static final String TRANSACTION_UTILITY_CLASS_NAME =
			"oracle.j2ee.transaction.TransactionUtility";

	private static final String TRANSACTION_MANAGER_CLASS_NAME =
			"oracle.j2ee.transaction.OC4JTransactionManager";

	private static final String TRANSACTION_CLASS_NAME =
			"oracle.j2ee.transaction.OC4JTransaction";

	private static final String FALLBACK_TRANSACTION_MANAGER_CLASS_NAME =
			"com.evermind.server.ApplicationServerTransactionManager";

	private static final String FALLBACK_TRANSACTION_CLASS_NAME =
			"com.evermind.server.ApplicationServerTransaction";


	private Method beginWithNameMethod;

	private Method setTransactionIsolationMethod;


	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		super.afterPropertiesSet();
		loadOC4JTransactionClasses();
	}

	@Override
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		try {
			Class transactionUtilityClass = getClass().getClassLoader().loadClass(TRANSACTION_UTILITY_CLASS_NAME);
			Method getInstanceMethod = transactionUtilityClass.getMethod("getInstance");
			Object transactionUtility = getInstanceMethod.invoke(null);
			logger.debug("Retrieving JTA UserTransaction from OC4J TransactionUtility");
			Method getUserTransactionMethod =
					transactionUtility.getClass().getMethod("getOC4JUserTransaction");
			return (UserTransaction) getUserTransactionMethod.invoke(transactionUtility);
		}
		catch (ClassNotFoundException ex) {
			logger.debug("Could not find OC4J 10.1.3.2 TransactionUtility: " + ex);
			// Return null to make the superclass perform its standard J2EE lookup,
			// which will work on earlier OC4J versions.
			return null;
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"OC4J's TransactionUtility.getOC4JUserTransaction() method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not invoke OC4J's TransactionUtility.getOC4JUserTransaction() method", ex);
		}
	}

	private void loadOC4JTransactionClasses() throws TransactionSystemException {
		// Find available OC4J API (in "oracle.j2ee.transaction" or "com.evermind.server")
		Class transactionManagerClass = null;
		Class transactionClass = null;
		try {
			transactionManagerClass = getClass().getClassLoader().loadClass(TRANSACTION_MANAGER_CLASS_NAME);
			transactionClass = getClass().getClassLoader().loadClass(TRANSACTION_CLASS_NAME);
		}
		catch (ClassNotFoundException ex) {
			try {
				transactionManagerClass = getClass().getClassLoader().loadClass(FALLBACK_TRANSACTION_MANAGER_CLASS_NAME);
				transactionClass = getClass().getClassLoader().loadClass(FALLBACK_TRANSACTION_CLASS_NAME);
			}
			catch (ClassNotFoundException ex2) {
				throw new TransactionSystemException(
						"Could not initialize OC4JJtaTransactionManager because OC4J API classes are not available", ex);
			}
		}

		// Cache reflective Method references for later use.
		if (transactionManagerClass.isInstance(getUserTransaction())) {
			this.beginWithNameMethod = ClassUtils.getMethodIfAvailable(
					transactionManagerClass, "begin", String.class);
			this.setTransactionIsolationMethod = ClassUtils.getMethodIfAvailable(
					transactionClass, "setTransactionIsolation", int.class);
			logger.info("Support for OC4J transaction names and isolation levels available");
		}
		else {
			logger.info("Support for OC4J transaction names and isolation levels not available");
		}
	}


	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
			throws NotSupportedException, SystemException {

		int timeout = determineTimeout(definition);
		applyTimeout(txObject, timeout);

		// Apply transaction name, if any, through the extended OC4J transaction begin method.
		if (this.beginWithNameMethod != null && definition.getName() != null) {
			/*
			oracle.j2ee.transaction.OC4JTransactionManager otm = (oracle.j2ee.transaction.OC4JTransactionManager) ut;
			otm.begin(definition.getName());
			*/
			try {
				this.beginWithNameMethod.invoke(txObject.getUserTransaction(), definition.getName());
			}
			catch (InvocationTargetException ex) {
				throw new TransactionSystemException(
						"OC4J's UserTransaction.begin(String) method failed", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new TransactionSystemException(
						"Could not invoke OC4J's UserTransaction.begin(String) method", ex);
			}
		}
		else {
			// No OC4J UserTransaction available or no transaction name specified
			// -> standard JTA begin call.
			txObject.getUserTransaction().begin();
		}

		// Specify isolation level, if any, through the corresponding OC4J transaction method.
		if (this.setTransactionIsolationMethod != null) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				try {
					Transaction tx = getTransactionManager().getTransaction();
					/*
					oracle.j2ee.transaction.OC4JTransaction otx = (oracle.j2ee.transaction.OC4JTransaction) tx;
					otx.setTransactionIsolation(definition.getIsolationLevel());
					*/
					this.setTransactionIsolationMethod.invoke(tx, definition.getIsolationLevel());
				}
				catch (InvocationTargetException ex) {
					throw new TransactionSystemException(
							"OC4J's Transaction.setTransactionIsolation(int) method failed", ex.getTargetException());
				}
				catch (Exception ex) {
					throw new TransactionSystemException(
							"Could not invoke OC4J's Transaction.setTransactionIsolation(int) method", ex);
				}
			}
		}
		else {
			applyIsolationLevel(txObject, definition.getIsolationLevel());
		}
	}


	@Override
	public Transaction createTransaction(String name, int timeout) throws NotSupportedException, SystemException {
		if (this.beginWithNameMethod != null && name != null) {
			UserTransaction ut = getUserTransaction();
			if (timeout >= 0) {
				ut.setTransactionTimeout(timeout);
			}
			try {
				this.beginWithNameMethod.invoke(ut, name);
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof NotSupportedException) {
					throw (NotSupportedException) ex.getTargetException();
				}
				else if (ex.getTargetException() instanceof SystemException) {
					throw (SystemException) ex.getTargetException();
				}
				else if (ex.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException) ex.getTargetException();
				}
				else {
					throw new SystemException(
							"OC4J's begin(String) method failed with an unexpected error: " + ex.getTargetException());
				}
			}
			catch (Exception ex) {
				throw new SystemException("Could not invoke OC4J's UserTransaction.begin(String) method: " + ex);
			}
			return new ManagedTransactionAdapter(getTransactionManager());
		}

		else {
			// No name specified - standard JTA is sufficient.
			return super.createTransaction(name, timeout);
		}
	}

}
