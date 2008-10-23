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

import java.util.List;

import javax.naming.NamingException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;

import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * WebSphere-specific PlatformTransactionManager implementation that delegates
 * to a {@link com.ibm.wsspi.uow.UOWManager} instance, obtained from WebSphere's
 * JNDI environment. This allows Spring to leverage the full power of the WebSphere
 * transaction coordinator, including transaction suspension, in a manner that is
 * perfectly compliant with officially supported WebSphere API.
 *
 * <p>The {@link CallbackPreferringPlatformTransactionManager} interface
 * implemented by this class indicates that callers should preferably pass in
 * a {@link TransactionCallback} through the {@link #execute} method, which
 * will be handled through the callback-based WebSphere UOWManager API instead
 * of through standard JTA API (UserTransaction / TransactionManager). This avoids
 * the use of the non-public <code>javax.transaction.TransactionManager</code>
 * API on WebSphere, staying within supported WebSphere API boundaries.
 *
 * <p>This transaction manager implementation derives from Spring's standard
 * {@link JtaTransactionManager}, inheriting the capability to support programmatic
 * transaction demarcation via <code>getTransaction</code> / <code>commit</code> /
 * <code>rollback</code> calls through a JTA UserTransaction handle, for callers
 * that do not use the TransactionCallback-based {@link #execute} method. However,
 * transaction suspension is <i>not</i> supported in this <code>getTransaction</code>
 * style (unless you explicitly specify a {@link #setTransactionManager} reference,
 * despite the official WebSphere recommendations). Use the {@link #execute} style
 * for any code that might require transaction suspension.
 *
 * <p>This transaction manager is compatible with WebSphere 7.0 as well as recent
 * WebSphere 6.0.x and 6.1.x versions. Check the documentation for your specific
 * WebSphere version to find out whether UOWManager support is available. If it
 * is not available, consider using Spring's standard {@link JtaTransactionManager}
 * class, if necessary specifying the {@link WebSphereTransactionManagerFactoryBean}
 * as "transactionManager" through the corresponding bean property. However, note
 * that transaction suspension is not officially supported in such a scenario
 * (despite it being known to work properly).
 *
 * <p>The default JNDI location for the UOWManager is "java:comp/websphere/UOWManager".
 * If the location happens to differ according to your WebSphere documentation,
 * simply specify the actual location through this transaction manager's
 * "uowManagerName" bean property.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setUowManager
 * @see #setUowManagerName
 * @see com.ibm.wsspi.uow.UOWManager
 */
public class WebSphereUowTransactionManager extends JtaTransactionManager
		implements CallbackPreferringPlatformTransactionManager {

	/**
	 * Default JNDI location for the WebSphere UOWManager.
	 * @see #setUowManagerName
	 */
	public static final String DEFAULT_UOW_MANAGER_NAME = "java:comp/websphere/UOWManager";


	private UOWManager uowManager;

	private String uowManagerName = DEFAULT_UOW_MANAGER_NAME;


	/**
	 * Create a new WebSphereUowTransactionManager.
	 */
	public WebSphereUowTransactionManager() {
		setAutodetectTransactionManager(false);
	}

	/**
	 * Create a new WebSphereUowTransactionManager for the given UOWManager.
	 * @param uowManager the WebSphere UOWManager to use as direct reference
	 */
	public WebSphereUowTransactionManager(UOWManager uowManager) {
		this();
		this.uowManager = uowManager;
	}


	/**
	 * Set the WebSphere UOWManager to use as direct reference.
	 * <p>Typically just used for test setups; in a J2EE environment,
	 * the UOWManager will always be fetched from JNDI.
	 * @see #setUserTransactionName
	 */
	public void setUowManager(UOWManager uowManager) {
		this.uowManager = uowManager;
	}

	/**
	 * Set the JNDI name of the WebSphere UOWManager.
	 * The default "java:comp/websphere/UOWManager" is used if not set.
	 * @see #DEFAULT_USER_TRANSACTION_NAME
	 * @see #setUowManager
	 */
	public void setUowManagerName(String uowManagerName) {
		this.uowManagerName = uowManagerName;
	}


	public void afterPropertiesSet() throws TransactionSystemException {
		initUserTransactionAndTransactionManager();

		// Fetch UOWManager handle from JNDI, if necessary.
		if (this.uowManager == null) {
			if (this.uowManagerName != null) {
				this.uowManager = lookupUowManager(this.uowManagerName);
			}
			else {
				throw new IllegalStateException("'uowManager' or 'uowManagerName' is required");
			}
		}
	}

	/**
	 * Look up the WebSphere UOWManager in JNDI via the configured name.
	 * Called by <code>afterPropertiesSet</code> if no direct UOWManager reference was set.
	 * Can be overridden in subclasses to provide a different UOWManager object.
	 * @param uowManagerName the JNDI name of the UOWManager
	 * @return the UOWManager object
	 * @throws TransactionSystemException if the JNDI lookup failed
	 * @see #setJndiTemplate
	 * @see #setUowManagerName
	 */
	protected UOWManager lookupUowManager(String uowManagerName) throws TransactionSystemException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieving WebSphere UOWManager from JNDI location [" + uowManagerName + "]");
			}
			return (UOWManager) getJndiTemplate().lookup(uowManagerName, UOWManager.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"WebSphere UOWManager is not available at JNDI location [" + uowManagerName + "]", ex);
		}
	}

	/**
	 * Registers the synchronizations as interposed JTA Synchronization on the UOWManager.
	 */
	protected void doRegisterAfterCompletionWithJtaTransaction(JtaTransactionObject txObject, List synchronizations) {
		this.uowManager.registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
	}


	public Object execute(TransactionDefinition definition, TransactionCallback callback) throws TransactionException {
		if (definition == null) {
			// Use defaults if no transaction definition given.
			definition = new DefaultTransactionDefinition();
		}

		if (definition.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", definition.getTimeout());
		}
		int pb = definition.getPropagationBehavior();
		boolean existingTx = (this.uowManager.getUOWStatus() != UOWSynchronizationRegistry.UOW_STATUS_NONE &&
				this.uowManager.getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);

		int uowType = UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION;
		boolean joinTx = false;
		boolean newSynch = false;

		if (existingTx) {
			if (pb == TransactionDefinition.PROPAGATION_NEVER) {
				throw new IllegalTransactionStateException(
						"Transaction propagation 'never' but existing transaction found");
			}
			if (pb == TransactionDefinition.PROPAGATION_NESTED) {
				throw new NestedTransactionNotSupportedException(
						"Transaction propagation 'nested' not supported for WebSphere UOW transactions");
			}
			if (pb == TransactionDefinition.PROPAGATION_SUPPORTS ||
					pb == TransactionDefinition.PROPAGATION_REQUIRED || pb == TransactionDefinition.PROPAGATION_MANDATORY) {
				joinTx = true;
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
			else if (pb == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
				uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
				newSynch = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			}
			else {
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
		}
		else {
			if (pb == TransactionDefinition.PROPAGATION_MANDATORY) {
				throw new IllegalTransactionStateException(
						"Transaction propagation 'mandatory' but no existing transaction found");
			}
			if (pb == TransactionDefinition.PROPAGATION_SUPPORTS ||
					pb == TransactionDefinition.PROPAGATION_NOT_SUPPORTED || pb == TransactionDefinition.PROPAGATION_NEVER) {
				uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
				newSynch = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			}
			else {
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
		}

		boolean debug = logger.isDebugEnabled();
		if (debug) {
			logger.debug("Creating new transaction with name [" + definition.getName() + "]: " + definition);
		}
		SuspendedResourcesHolder suspendedResources = (existingTx && !joinTx ? suspend(null) : null);
		try {
			if (definition.getTimeout() > TransactionDefinition.TIMEOUT_DEFAULT) {
				this.uowManager.setUOWTimeout(uowType, definition.getTimeout());
			}
			if (debug) {
				logger.debug("Invoking WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			UOWActionAdapter action = new UOWActionAdapter(
					definition, callback, (uowType == UOWManager.UOW_TYPE_GLOBAL_TRANSACTION), !joinTx, newSynch, debug);
			this.uowManager.runUnderUOW(uowType, joinTx, action);
			if (debug) {
				logger.debug("Returned from WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			return action.getResult();
		}
		catch (UOWException ex) {
			throw new TransactionSystemException("UOWManager transaction processing failed", ex);
		}
		catch (UOWActionException ex) {
			throw new TransactionSystemException("UOWManager threw unexpected UOWActionException", ex);
		}
		finally {
			if (suspendedResources != null) {
				resume(null, suspendedResources);
			}
		}
	}


	/**
	 * Adapter that executes the given Spring transaction within the WebSphere UOWAction shape.
	 */
	private class UOWActionAdapter implements UOWAction {

		private final TransactionDefinition definition;

		private final TransactionCallback callback;

		private final boolean actualTransaction;

		private final boolean newTransaction;

		private final boolean newSynchronization;

		private boolean debug;

		private Object result;

		public UOWActionAdapter(TransactionDefinition definition, TransactionCallback callback,
				boolean actualTransaction, boolean newTransaction, boolean newSynchronization, boolean debug) {
			this.definition = definition;
			this.callback = callback;
			this.actualTransaction = actualTransaction;
			this.newTransaction = newTransaction;
			this.newSynchronization = newSynchronization;
			this.debug = debug;
		}

		public void run() {
			DefaultTransactionStatus status = newTransactionStatus(
					this.definition, (this.actualTransaction ? this : null),
					this.newTransaction, this.newSynchronization, this.debug, null);
			try {
				this.result = this.callback.doInTransaction(status);
				triggerBeforeCommit(status);
			}
			finally {
				if (status.isLocalRollbackOnly()) {
					if (status.isDebug()) {
						logger.debug("Transactional code has requested rollback");
					}
					uowManager.setRollbackOnly();
				}
				triggerBeforeCompletion(status);
				if (status.isNewSynchronization()) {
					List synchronizations = TransactionSynchronizationManager.getSynchronizations();
					TransactionSynchronizationManager.clear();
					uowManager.registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
				}
			}
		}

		public Object getResult() {
			return this.result;
		}
	}

}
