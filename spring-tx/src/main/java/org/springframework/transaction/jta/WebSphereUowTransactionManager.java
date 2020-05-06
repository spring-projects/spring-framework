/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.List;

import javax.naming.NamingException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

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
 * the use of the non-public {@code javax.transaction.TransactionManager}
 * API on WebSphere, staying within supported WebSphere API boundaries.
 *
 * <p>This transaction manager implementation derives from Spring's standard
 * {@link JtaTransactionManager}, inheriting the capability to support programmatic
 * transaction demarcation via {@code getTransaction} / {@code commit} /
 * {@code rollback} calls through a JTA UserTransaction handle, for callers
 * that do not use the TransactionCallback-based {@link #execute} method. However,
 * transaction suspension is <i>not</i> supported in this {@code getTransaction}
 * style (unless you explicitly specify a {@link #setTransactionManager} reference,
 * despite the official WebSphere recommendations). Use the {@link #execute} style
 * for any code that might require transaction suspension.
 *
 * <p>This transaction manager is compatible with WebSphere 6.1.0.9 and above.
 * The default JNDI location for the UOWManager is "java:comp/websphere/UOWManager".
 * If the location happens to differ according to your WebSphere documentation,
 * simply specify the actual location through this transaction manager's
 * "uowManagerName" bean property.
 *
 * <p><b>NOTE: This JtaTransactionManager is intended to refine specific transaction
 * demarcation behavior on Spring's side. It will happily co-exist with independently
 * configured WebSphere transaction strategies in your persistence provider, with no
 * need to specifically connect those setups in any way.</b>
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setUowManager
 * @see #setUowManagerName
 * @see com.ibm.wsspi.uow.UOWManager
 */
@SuppressWarnings("serial")
public class WebSphereUowTransactionManager extends JtaTransactionManager
		implements CallbackPreferringPlatformTransactionManager {

	/**
	 * Default JNDI location for the WebSphere UOWManager.
	 * @see #setUowManagerName
	 */
	public static final String DEFAULT_UOW_MANAGER_NAME = "java:comp/websphere/UOWManager";


	@Nullable
	private UOWManager uowManager;

	@Nullable
	private String uowManagerName;


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
	 * <p>Typically just used for test setups; in a Java EE environment,
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


	@Override
	public void afterPropertiesSet() throws TransactionSystemException {
		initUserTransactionAndTransactionManager();

		// Fetch UOWManager handle from JNDI, if necessary.
		if (this.uowManager == null) {
			if (this.uowManagerName != null) {
				this.uowManager = lookupUowManager(this.uowManagerName);
			}
			else {
				this.uowManager = lookupDefaultUowManager();
			}
		}
	}

	/**
	 * Look up the WebSphere UOWManager in JNDI via the configured name.
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
			return getJndiTemplate().lookup(uowManagerName, UOWManager.class);
		}
		catch (NamingException ex) {
			throw new TransactionSystemException(
					"WebSphere UOWManager is not available at JNDI location [" + uowManagerName + "]", ex);
		}
	}

	/**
	 * Obtain the WebSphere UOWManager from the default JNDI location
	 * "java:comp/websphere/UOWManager".
	 * @return the UOWManager object
	 * @throws TransactionSystemException if the JNDI lookup failed
	 * @see #setJndiTemplate
	 */
	protected UOWManager lookupDefaultUowManager() throws TransactionSystemException {
		try {
			logger.debug("Retrieving WebSphere UOWManager from default JNDI location [" + DEFAULT_UOW_MANAGER_NAME + "]");
			return getJndiTemplate().lookup(DEFAULT_UOW_MANAGER_NAME, UOWManager.class);
		}
		catch (NamingException ex) {
			logger.debug("WebSphere UOWManager is not available at default JNDI location [" +
					DEFAULT_UOW_MANAGER_NAME + "] - falling back to UOWManagerFactory lookup");
			return UOWManagerFactory.getUOWManager();
		}
	}

	private UOWManager obtainUOWManager() {
		Assert.state(this.uowManager != null, "No UOWManager set");
		return this.uowManager;
	}


	/**
	 * Registers the synchronizations as interposed JTA Synchronization on the UOWManager.
	 */
	@Override
	protected void doRegisterAfterCompletionWithJtaTransaction(
			JtaTransactionObject txObject, List<TransactionSynchronization> synchronizations) {

		obtainUOWManager().registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
	}

	/**
	 * Returns {@code true} since WebSphere ResourceAdapters (as exposed in JNDI)
	 * implicitly perform transaction enlistment if the MessageEndpointFactory's
	 * {@code isDeliveryTransacted} method returns {@code true}.
	 * In that case we'll simply skip the {@link #createTransaction} call.
	 * @see javax.resource.spi.endpoint.MessageEndpointFactory#isDeliveryTransacted
	 * @see org.springframework.jca.endpoint.AbstractMessageEndpointFactory
	 * @see TransactionFactory#createTransaction
	 */
	@Override
	public boolean supportsResourceAdapterManagedTransactions() {
		return true;
	}


	@Override
	@Nullable
	public <T> T execute(@Nullable TransactionDefinition definition, TransactionCallback<T> callback)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
		}

		UOWManager uowManager = obtainUOWManager();
		int pb = def.getPropagationBehavior();
		boolean existingTx = (uowManager.getUOWStatus() != UOWSynchronizationRegistry.UOW_STATUS_NONE &&
				uowManager.getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);

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
					pb == TransactionDefinition.PROPAGATION_REQUIRED ||
					pb == TransactionDefinition.PROPAGATION_MANDATORY) {
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
					pb == TransactionDefinition.PROPAGATION_NOT_SUPPORTED ||
					pb == TransactionDefinition.PROPAGATION_NEVER) {
				uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;
				newSynch = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
			}
			else {
				newSynch = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			}
		}

		boolean debug = logger.isDebugEnabled();
		if (debug) {
			logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
		}
		SuspendedResourcesHolder suspendedResources = (!joinTx ? suspend(null) : null);
		UOWActionAdapter<T> action = null;
		try {
			if (def.getTimeout() > TransactionDefinition.TIMEOUT_DEFAULT) {
				uowManager.setUOWTimeout(uowType, def.getTimeout());
			}
			if (debug) {
				logger.debug("Invoking WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			action = new UOWActionAdapter<>(
					def, callback, (uowType == UOWManager.UOW_TYPE_GLOBAL_TRANSACTION), !joinTx, newSynch, debug);
			uowManager.runUnderUOW(uowType, joinTx, action);
			if (debug) {
				logger.debug("Returned from WebSphere UOW action: type=" + uowType + ", join=" + joinTx);
			}
			return action.getResult();
		}
		catch (UOWException | UOWActionException ex) {
			TransactionSystemException tse =
					new TransactionSystemException("UOWManager transaction processing failed", ex);
			Throwable appEx = action.getException();
			if (appEx != null) {
				logger.error("Application exception overridden by rollback exception", appEx);
				tse.initApplicationException(appEx);
			}
			throw tse;
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
	private class UOWActionAdapter<T> implements UOWAction, SmartTransactionObject {

		private final TransactionDefinition definition;

		private final TransactionCallback<T> callback;

		private final boolean actualTransaction;

		private final boolean newTransaction;

		private final boolean newSynchronization;

		private boolean debug;

		@Nullable
		private T result;

		@Nullable
		private Throwable exception;

		public UOWActionAdapter(TransactionDefinition definition, TransactionCallback<T> callback,
				boolean actualTransaction, boolean newTransaction, boolean newSynchronization, boolean debug) {

			this.definition = definition;
			this.callback = callback;
			this.actualTransaction = actualTransaction;
			this.newTransaction = newTransaction;
			this.newSynchronization = newSynchronization;
			this.debug = debug;
		}

		@Override
		public void run() {
			UOWManager uowManager = obtainUOWManager();
			DefaultTransactionStatus status = prepareTransactionStatus(
					this.definition, (this.actualTransaction ? this : null),
					this.newTransaction, this.newSynchronization, this.debug, null);
			try {
				this.result = this.callback.doInTransaction(status);
				triggerBeforeCommit(status);
			}
			catch (Throwable ex) {
				this.exception = ex;
				if (status.isDebug()) {
					logger.debug("Rolling back on application exception from transaction callback", ex);
				}
				uowManager.setRollbackOnly();
			}
			finally {
				if (status.isLocalRollbackOnly()) {
					if (status.isDebug()) {
						logger.debug("Transaction callback has explicitly requested rollback");
					}
					uowManager.setRollbackOnly();
				}
				triggerBeforeCompletion(status);
				if (status.isNewSynchronization()) {
					List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
					TransactionSynchronizationManager.clear();
					if (!synchronizations.isEmpty()) {
						uowManager.registerInterposedSynchronization(new JtaAfterCompletionSynchronization(synchronizations));
					}
				}
			}
		}

		@Nullable
		public T getResult() {
			if (this.exception != null) {
				ReflectionUtils.rethrowRuntimeException(this.exception);
			}
			return this.result;
		}

		@Nullable
		public Throwable getException() {
			return this.exception;
		}

		@Override
		public boolean isRollbackOnly() {
			return obtainUOWManager().getRollbackOnly();
		}

		@Override
		public void flush() {
			TransactionSynchronizationUtils.triggerFlush();
		}
	}

}
