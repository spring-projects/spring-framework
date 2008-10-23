/*
 * Copyright 2002-2007 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;

/**
 * Special {@link JtaTransactionManager} variant for BEA WebLogic (7.0, 8.1 and higher).
 * Supports the full power of Spring's transaction definitions on WebLogic's
 * transaction coordinator, <i>beyond standard JTA</i>: transaction names,
 * per-transaction isolation levels, and proper resuming of transactions in all cases.
 *
 * <p>Uses WebLogic's special <code>begin(name)</code> method to start a JTA transaction,
 * in order to make <b>Spring-driven transactions visible in WebLogic's transaction
 * monitor</b>. In case of Spring's declarative transactions, the exposed name will
 * (by default) be the fully-qualified class name + "." + method name.
 *
 * <p>Supports a <b>per-transaction isolation level</b> through WebLogic's corresponding
 * JTA transaction property "ISOLATION LEVEL". This will apply the specified isolation
 * level (e.g. ISOLATION_SERIALIZABLE) to all JDBC Connections that participate in the
 * given transaction.
 *
 * <p>Invokes WebLogic's special <code>forceResume</code> method if standard JTA resume
 * failed, to <b>also resume if the target transaction was marked rollback-only</b>.
 * If you're not relying on this feature of transaction suspension in the first
 * place, Spring's standard JtaTransactionManager will behave properly too.
 *
 * <p>Automatically detects WebLogic Server 7.0 or 8.1+ and adapts accordingly.
 * Usage on a WebLogic client is also supported, although with restricted
 * functionality: transaction names cannot be applied there.
 *
 * <p>By default, the JTA UserTransaction and TransactionManager handles are
 * fetched directly from WebLogic's <code>TransactionHelper</code> (on 8.1+)
 * or <code>TxHelper</code> (on 7.0). This can be overridden by specifying
 * "userTransaction"/"userTransactionName" and "transactionManager"/"transactionManagerName",
 * passing in existing handles or specifying corresponding JNDI locations to look up.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.transaction.TransactionDefinition#getName
 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel
 * @see weblogic.transaction.UserTransaction#begin(String)
 * @see weblogic.transaction.Transaction#setProperty
 * @see weblogic.transaction.TransactionManager#forceResume
 * @see weblogic.transaction.TransactionHelper
 * @see weblogic.transaction.TxHelper
 */
public class WebLogicJtaTransactionManager extends JtaTransactionManager {

	private static final String USER_TRANSACTION_CLASS_NAME = "weblogic.transaction.UserTransaction";

	private static final String CLIENT_TRANSACTION_MANAGER_CLASS_NAME = "weblogic.transaction.ClientTransactionManager";

	private static final String TRANSACTION_MANAGER_CLASS_NAME = "weblogic.transaction.TransactionManager";

	private static final String TRANSACTION_CLASS_NAME = "weblogic.transaction.Transaction";

	private static final String TRANSACTION_HELPER_CLASS_NAME = "weblogic.transaction.TransactionHelper";

	private static final String TX_HELPER_CLASS_NAME = "weblogic.transaction.TxHelper";

	private static final String ISOLATION_LEVEL_KEY = "ISOLATION LEVEL";


	private boolean weblogicUserTransactionAvailable;

	private Method beginWithNameMethod;

	private Method beginWithNameAndTimeoutMethod;

	private boolean weblogicTransactionManagerAvailable;

	private Method forceResumeMethod;

	private Method setPropertyMethod;

	private Class transactionHelperClass;

	private Object transactionHelper;


	public void afterPropertiesSet() throws TransactionSystemException {
		super.afterPropertiesSet();
		loadWebLogicTransactionClasses();
	}

	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		loadWebLogicTransactionHelperClass();
		try {
			logger.debug("Retrieving JTA UserTransaction from WebLogic TransactionHelper/TxHelper");
			Method getUserTransactionMethod =
					this.transactionHelperClass.getMethod("getUserTransaction", new Class[0]);
			return (UserTransaction) getUserTransactionMethod.invoke(this.transactionHelper, new Object[0]);
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"WebLogic's TransactionHelper/TxHelper.getUserTransaction() method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not invoke WebLogic's TransactionHelper/TxHelper.getUserTransaction() method", ex);
		}
	}

	protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
		loadWebLogicTransactionHelperClass();
		try {
			logger.debug("Retrieving JTA TransactionManager from WebLogic TransactionHelper/TxHelper");
			Method getTransactionManagerMethod =
					this.transactionHelperClass.getMethod("getTransactionManager", new Class[0]);
			return (TransactionManager) getTransactionManagerMethod.invoke(this.transactionHelper, new Object[0]);
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"WebLogic's TransactionHelper/TxHelper.getTransactionManager() method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not invoke WebLogic's TransactionHelper/TxHelper.getTransactionManager() method", ex);
		}
	}


	private void loadWebLogicTransactionHelperClass() throws TransactionSystemException {
		if (this.transactionHelperClass == null) {
			try {
				try {
					this.transactionHelperClass =
							getClass().getClassLoader().loadClass(TRANSACTION_HELPER_CLASS_NAME);
					Method getTransactionHelperMethod =
							this.transactionHelperClass.getMethod("getTransactionHelper", new Class[0]);
					this.transactionHelper = getTransactionHelperMethod.invoke(null, new Object[0]);
					logger.debug("WebLogic 8.1+ TransactionHelper found");
				}
				catch (ClassNotFoundException ex) {
					this.transactionHelperClass =
							getClass().getClassLoader().loadClass(TX_HELPER_CLASS_NAME);
					logger.debug("WebLogic 7.0 TxHelper found");
				}
			}
			catch (InvocationTargetException ex) {
				throw new TransactionSystemException(
						"WebLogic's TransactionHelper.getTransactionHelper() method failed", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new TransactionSystemException(
						"Could not initialize WebLogicJtaTransactionManager because WebLogic API classes are not available",
						ex);
			}
		}
	}

	private void loadWebLogicTransactionClasses() throws TransactionSystemException {
		try {
			Class userTransactionClass =
			    getClass().getClassLoader().loadClass(USER_TRANSACTION_CLASS_NAME);
			this.weblogicUserTransactionAvailable =
					userTransactionClass.isInstance(getUserTransaction());
			if (this.weblogicUserTransactionAvailable) {
				this.beginWithNameMethod =
						userTransactionClass.getMethod("begin", new Class[] {String.class});
				this.beginWithNameAndTimeoutMethod =
						userTransactionClass.getMethod("begin", new Class[] {String.class, int.class});
				logger.info("Support for WebLogic transaction names available");
			}
			else {
				logger.info("Support for WebLogic transaction names not available");
			}

			Class transactionManagerClass = null;
			try {
				// Try WebLogic 8.1 ClientTransactionManager interface.
				transactionManagerClass =
						getClass().getClassLoader().loadClass(CLIENT_TRANSACTION_MANAGER_CLASS_NAME);
				logger.debug("WebLogic 8.1+ ClientTransactionManager found");
			}
			catch (ClassNotFoundException ex) {
				// Fall back to WebLogic TransactionManager interface.
				transactionManagerClass =
						getClass().getClassLoader().loadClass(TRANSACTION_MANAGER_CLASS_NAME);
				logger.debug("WebLogic 7.0 TransactionManager found");
			}

			this.weblogicTransactionManagerAvailable =
					transactionManagerClass.isInstance(getTransactionManager());
			if (this.weblogicTransactionManagerAvailable) {
				Class transactionClass = getClass().getClassLoader().loadClass(TRANSACTION_CLASS_NAME);
				this.forceResumeMethod =
						transactionManagerClass.getMethod("forceResume", new Class[] {Transaction.class});
				this.setPropertyMethod =
						transactionClass.getMethod("setProperty", new Class[] {String.class, Serializable.class});
				logger.debug("Support for WebLogic forceResume available");
			}
			else {
				logger.warn("Support for WebLogic forceResume not available");
			}
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not initialize WebLogicJtaTransactionManager because WebLogic API classes are not available",
			    ex);
		}
	}


	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition)
			throws NotSupportedException, SystemException {

		int timeout = determineTimeout(definition);

		// Apply transaction name (if any) to WebLogic transaction.
		if (this.weblogicUserTransactionAvailable && definition.getName() != null) {
			try {
				if (timeout > TransactionDefinition.TIMEOUT_DEFAULT) {
					/*
					weblogic.transaction.UserTransaction wut = (weblogic.transaction.UserTransaction) ut;
					wut.begin(definition.getName(), timeout);
					*/
					this.beginWithNameAndTimeoutMethod.invoke(txObject.getUserTransaction(),
							new Object[] {definition.getName(), new Integer(timeout)});
				}
				else {
					/*
					weblogic.transaction.UserTransaction wut = (weblogic.transaction.UserTransaction) ut;
					wut.begin(definition.getName());
					*/
					this.beginWithNameMethod.invoke(txObject.getUserTransaction(),
							new Object[] {definition.getName()});
				}
			}
			catch (InvocationTargetException ex) {
				throw new TransactionSystemException(
						"WebLogic's UserTransaction.begin() method failed", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new TransactionSystemException(
						"Could not invoke WebLogic's UserTransaction.begin() method", ex);
			}
		}
		else {
			// No WebLogic UserTransaction available or no transaction name specified
			// -> standard JTA begin call.
			applyTimeout(txObject, timeout);
			txObject.getUserTransaction().begin();
		}

		// Specify isolation level, if any, through corresponding WebLogic transaction property.
		if (this.weblogicTransactionManagerAvailable) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				try {
					Transaction tx = getTransactionManager().getTransaction();
					Integer isolationLevel = new Integer(definition.getIsolationLevel());
					/*
					weblogic.transaction.Transaction wtx = (weblogic.transaction.Transaction) tx;
					wtx.setProperty(ISOLATION_LEVEL_KEY, isolationLevel);
					*/
					this.setPropertyMethod.invoke(tx, new Object[] {ISOLATION_LEVEL_KEY, isolationLevel});
				}
				catch (InvocationTargetException ex) {
					throw new TransactionSystemException(
							"WebLogic's Transaction.setProperty(String, Serializable) method failed", ex.getTargetException());
				}
				catch (Exception ex) {
					throw new TransactionSystemException(
							"Could not invoke WebLogic's Transaction.setProperty(String, Serializable) method", ex);
				}
			}
		}
		else {
			applyIsolationLevel(txObject, definition.getIsolationLevel());
		}
	}

	protected void doJtaResume(JtaTransactionObject txObject, Object suspendedTransaction)
			throws InvalidTransactionException, SystemException {

		try {
			getTransactionManager().resume((Transaction) suspendedTransaction);
		}
		catch (InvalidTransactionException ex) {
			if (!this.weblogicTransactionManagerAvailable) {
				throw ex;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Standard JTA resume threw InvalidTransactionException: " + ex.getMessage() +
				    " - trying WebLogic JTA forceResume");
			}
			/*
			weblogic.transaction.TransactionManager wtm =
					(weblogic.transaction.TransactionManager) getTransactionManager();
			wtm.forceResume(suspendedTransaction);
			*/
			try {
				this.forceResumeMethod.invoke(getTransactionManager(), new Object[] {suspendedTransaction});
			}
			catch (InvocationTargetException ex2) {
				throw new TransactionSystemException(
						"WebLogic's TransactionManager.forceResume(Transaction) method failed", ex2.getTargetException());
			}
			catch (Exception ex2) {
				throw new TransactionSystemException(
						"Could not access WebLogic's TransactionManager.forceResume(Transaction) method", ex2);
			}
		}
	}


	public Transaction createTransaction(String name, int timeout) throws NotSupportedException, SystemException {
		if (this.weblogicUserTransactionAvailable && name != null) {
			try {
				if (timeout >= 0) {
					this.beginWithNameAndTimeoutMethod.invoke(getUserTransaction(), new Object[] {name, new Integer(timeout)});
				}
				else {
					this.beginWithNameMethod.invoke(getUserTransaction(), new Object[] {name});
				}
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
							"WebLogic's begin() method failed with an unexpected error: " + ex.getTargetException());
				}
			}
			catch (Exception ex) {
				throw new SystemException("Could not invoke WebLogic's UserTransaction.begin() method: " + ex);
			}
			return getTransactionManager().getTransaction();
		}

		else {
			// No name specified - standard JTA is sufficient.
			return super.createTransaction(name, timeout);
		}
	}

}
