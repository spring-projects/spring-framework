/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.transaction.support;

import io.micrometer.core.instrument.observation.Observation;
import io.micrometer.core.instrument.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * An observation representation of a {@link PlatformTransactionManager}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public final class ObservationPlatformTransactionManager implements PlatformTransactionManager {

	private static final Log log = LogFactory.getLog(ObservationPlatformTransactionManager.class);

	private final PlatformTransactionManager delegate;

	private final ObservationRegistry observationRegistry;

	private final TransactionObservationContext context;

	private final TransactionTagsProvider transactionTagsProvider;

	public ObservationPlatformTransactionManager(PlatformTransactionManager delegate, ObservationRegistry observationRegistry, TransactionObservationContext context, TransactionTagsProvider transactionTagsProvider) {
		this.delegate = delegate;
		this.observationRegistry = observationRegistry;
		this.context = context;
		this.transactionTagsProvider = transactionTagsProvider;
	}

	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		if (this.observationRegistry.isNoOp()) {
			return this.delegate.getTransaction(definition);
		}
		Observation obs = this.observationRegistry.getCurrentObservation();
		obs = obs != null ? obs : Observation.NOOP;
		try (Observation.Scope scope = obs.openScope()) {
			obs = TransactionObservation.TX_OBSERVATION.observation(this.observationRegistry, this.context)
					.tagsProvider(this.transactionTagsProvider)
					.start();
			this.context.setScope(scope);
		}
		this.context.setObservation(obs);
		try {
			TransactionStatus transaction = this.delegate.getTransaction(definition);
			this.context.setTransactionStatus(transaction);
			return transaction;
		}
		catch (Exception ex) {
			this.context.getObservation().error(ex).stop();
			throw ex;
		}
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		if (this.observationRegistry.isNoOp()) {
			this.delegate.commit(status);
			return;
		}
		try {
			if (log.isDebugEnabled()) {
				log.debug("Wrapping commit");
			}
			this.delegate.commit(status);
		}
		catch (Exception ex) {
			this.context.getObservation().error(ex);
			throw ex;
		}
		finally {
			this.context.getScope().close();
			this.context.getObservation().stop();
		}
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		if (this.observationRegistry.isNoOp()) {
			this.delegate.rollback(status);
			return;
		}
		try {
			if (log.isDebugEnabled()) {
				log.debug("Wrapping rollback");
			}
			this.delegate.rollback(status);
		}
		catch (Exception ex) {
			this.context.getObservation().error(ex);
			throw ex;
		}
		finally {
			this.context.getScope().close();
			this.context.getObservation().stop();
		}
	}

}
