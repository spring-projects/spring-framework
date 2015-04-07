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

package org.springframework.jca.endpoint;

import java.lang.reflect.Method;
import javax.resource.ResourceException;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.transaction.jta.SimpleTransactionFactory;
import org.springframework.transaction.jta.TransactionFactory;

/**
 * Abstract base implementation of the JCA 1.5/1.6/1.7
 * {@link javax.resource.spi.endpoint.MessageEndpointFactory} interface,
 * providing transaction management capabilities as well as ClassLoader
 * exposure for endpoint invocations.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setTransactionManager
 */
public abstract class AbstractMessageEndpointFactory implements MessageEndpointFactory, BeanNameAware {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private TransactionFactory transactionFactory;

	private String transactionName;

	private int transactionTimeout = -1;

	private String beanName;


	/**
	 * Set the the XA transaction manager to use for wrapping endpoint
	 * invocations, enlisting the endpoint resource in each such transaction.
	 * <p>The passed-in object may be a transaction manager which implements
	 * Spring's {@link org.springframework.transaction.jta.TransactionFactory}
	 * interface, or a plain {@link javax.transaction.TransactionManager}.
	 * <p>If no transaction manager is specified, the endpoint invocation
	 * will simply not be wrapped in an XA transaction. Check out your
	 * resource provider's ActivationSpec documentation for local
	 * transaction options of your particular provider.
	 * @see #setTransactionName
	 * @see #setTransactionTimeout
	 */
	public void setTransactionManager(Object transactionManager) {
		if (transactionManager instanceof TransactionFactory) {
			this.transactionFactory = (TransactionFactory) transactionManager;
		}
		else if (transactionManager instanceof TransactionManager) {
			this.transactionFactory = new SimpleTransactionFactory((TransactionManager) transactionManager);
		}
		else {
			throw new IllegalArgumentException("Transaction manager [" + transactionManager +
					"] is neither a [org.springframework.transaction.jta.TransactionFactory} nor a " +
					"[javax.transaction.TransactionManager]");
		}
	}

	/**
	 * Set the Spring TransactionFactory to use for wrapping endpoint
	 * invocations, enlisting the endpoint resource in each such transaction.
	 * <p>Alternatively, specify an appropriate transaction manager through
	 * the {@link #setTransactionManager "transactionManager"} property.
	 * <p>If no transaction factory is specified, the endpoint invocation
	 * will simply not be wrapped in an XA transaction. Check out your
	 * resource provider's ActivationSpec documentation for local
	 * transaction options of your particular provider.
	 * @see #setTransactionName
	 * @see #setTransactionTimeout
	 */
	public void setTransactionFactory(TransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
	}

	/**
	 * Specify the name of the transaction, if any.
	 * <p>Default is none. A specified name will be passed on to the transaction
	 * manager, allowing to identify the transaction in a transaction monitor.
	 */
	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}

	/**
	 * Specify the transaction timeout, if any.
	 * <p>Default is -1: rely on the transaction manager's default timeout.
	 * Specify a concrete timeout to restrict the maximum duration of each
	 * endpoint invocation.
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * Set the name of this message endpoint. Populated with the bean name
	 * automatically when defined within Spring's bean factory.
	 */
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	/**
	 * Implementation of the JCA 1.7 {@code #getActivationName()} method,
	 * returning the bean name as set on this MessageEndpointFactory.
	 * @see #setBeanName
	 */
	public String getActivationName() {
		return this.beanName;
	}

	/**
	 * This implementation returns {@code true} if a transaction manager
	 * has been specified; {@code false} otherwise.
	 * @see #setTransactionManager
	 * @see #setTransactionFactory
	 */
	@Override
	public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
		return (this.transactionFactory != null);
	}

	/**
	 * The standard JCA 1.5 version of {@code createEndpoint}.
	 * <p>This implementation delegates to {@link #createEndpointInternal()},
	 * initializing the endpoint's XAResource before the endpoint gets invoked.
	 */
	@Override
	public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
		AbstractMessageEndpoint endpoint = createEndpointInternal();
		endpoint.initXAResource(xaResource);
		return endpoint;
	}

	/**
	 * The alternative JCA 1.6 version of {@code createEndpoint}.
	 * <p>This implementation delegates to {@link #createEndpointInternal()},
	 * ignoring the specified timeout. It is only here for JCA 1.6 compliance.
	 */
	public MessageEndpoint createEndpoint(XAResource xaResource, long timeout) throws UnavailableException {
		AbstractMessageEndpoint endpoint = createEndpointInternal();
		endpoint.initXAResource(xaResource);
		return endpoint;
	}

	/**
	 * Create the actual endpoint instance, as a subclass of the
	 * {@link AbstractMessageEndpoint} inner class of this factory.
	 * @return the actual endpoint instance (never {@code null})
	 * @throws UnavailableException if no endpoint is available at present
	 */
	protected abstract AbstractMessageEndpoint createEndpointInternal() throws UnavailableException;


	/**
	 * Inner class for actual endpoint implementations, based on template
	 * method to allow for any kind of concrete endpoint implementation.
	 */
	protected abstract class AbstractMessageEndpoint implements MessageEndpoint {

		private TransactionDelegate transactionDelegate;

		private boolean beforeDeliveryCalled = false;

		private ClassLoader previousContextClassLoader;

		/**
		 * Initialize this endpoint's TransactionDelegate.
		 * @param xaResource the XAResource for this endpoint
		 */
		void initXAResource(XAResource xaResource) {
			this.transactionDelegate = new TransactionDelegate(xaResource);
		}

		/**
		 * This {@code beforeDelivery} implementation starts a transaction,
		 * if necessary, and exposes the endpoint ClassLoader as current
		 * thread context ClassLoader.
		 * <p>Note that the JCA 1.5 specification does not require a ResourceAdapter
		 * to call this method before invoking the concrete endpoint. If this method
		 * has not been called (check {@link #hasBeforeDeliveryBeenCalled()}), the
		 * concrete endpoint method should call {@code beforeDelivery} and its
		 * sibling {@link #afterDelivery()} explicitly, as part of its own processing.
		 */
		@Override
		public void beforeDelivery(Method method) throws ResourceException {
			this.beforeDeliveryCalled = true;
			try {
				this.transactionDelegate.beginTransaction();
			}
			catch (Throwable ex) {
				throw new ApplicationServerInternalException("Failed to begin transaction", ex);
			}
			Thread currentThread = Thread.currentThread();
			this.previousContextClassLoader = currentThread.getContextClassLoader();
			currentThread.setContextClassLoader(getEndpointClassLoader());
		}

		/**
		 * Template method for exposing the endpoint's ClassLoader
		 * (typically the ClassLoader that the message listener class
		 * has been loaded with).
		 * @return the endpoint ClassLoader (never {@code null})
		 */
		protected abstract ClassLoader getEndpointClassLoader();

		/**
		 * Return whether the {@link #beforeDelivery} method of this endpoint
		 * has already been called.
		 */
		protected final boolean hasBeforeDeliveryBeenCalled() {
			return this.beforeDeliveryCalled;
		}

		/**
		 * Callback method for notifying the endpoint base class
		 * that the concrete endpoint invocation led to an exception.
		 * <p>To be invoked by subclasses in case of the concrete
		 * endpoint throwing an exception.
		 * @param ex the exception thrown from the concrete endpoint
		 */
		protected final void onEndpointException(Throwable ex) {
			this.transactionDelegate.setRollbackOnly();
		}

		/**
		 * This {@code afterDelivery} implementation resets the thread context
		 * ClassLoader and completes the transaction, if any.
		 * <p>Note that the JCA 1.5 specification does not require a ResourceAdapter
		 * to call this method after invoking the concrete endpoint. See the
		 * explanation in {@link #beforeDelivery}'s javadoc.
		 */
		@Override
		public void afterDelivery() throws ResourceException {
			this.beforeDeliveryCalled = false;
			Thread.currentThread().setContextClassLoader(this.previousContextClassLoader);
			this.previousContextClassLoader = null;
			try {
				this.transactionDelegate.endTransaction();
			}
			catch (Throwable ex) {
				throw new ApplicationServerInternalException("Failed to complete transaction", ex);
			}
		}

		@Override
		public void release() {
			try {
				this.transactionDelegate.setRollbackOnly();
				this.transactionDelegate.endTransaction();
			}
			catch (Throwable ex) {
				logger.error("Could not complete unfinished transaction on endpoint release", ex);
			}
		}
	}


	/**
	 * Private inner class that performs the actual transaction handling,
	 * including enlistment of the endpoint's XAResource.
	 */
	private class TransactionDelegate {

		private final XAResource xaResource;

		private Transaction transaction;

		private boolean rollbackOnly;

		public TransactionDelegate(XAResource xaResource) {
			if (xaResource == null) {
				if (transactionFactory != null && !transactionFactory.supportsResourceAdapterManagedTransactions()) {
					throw new IllegalStateException("ResourceAdapter-provided XAResource is required for " +
							"transaction management. Check your ResourceAdapter's configuration.");
				}
			}
			this.xaResource = xaResource;
		}

		public void beginTransaction() throws Exception {
			if (transactionFactory != null && this.xaResource != null) {
				this.transaction = transactionFactory.createTransaction(transactionName, transactionTimeout);
				this.transaction.enlistResource(this.xaResource);
			}
		}

		public void setRollbackOnly() {
			if (this.transaction != null) {
				this.rollbackOnly = true;
			}
		}

		public void endTransaction() throws Exception {
			if (this.transaction != null) {
				try {
					if (this.rollbackOnly) {
						this.transaction.rollback();
					}
					else {
						this.transaction.commit();
					}
				}
				finally {
					this.transaction = null;
					this.rollbackOnly = false;
				}
			}
		}
	}

}
