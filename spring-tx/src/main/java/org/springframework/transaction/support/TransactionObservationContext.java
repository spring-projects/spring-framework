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

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;

/**
 * A {@link Observation.Context} for Spring Transaction.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public class TransactionObservationContext extends Observation.Context {

	private final TransactionDefinition transactionDefinition;

	@Nullable
	private final Class<?> transactionManagerClass;

	private TransactionStatus status;

	private TransactionStatus transactionStatus;

	public TransactionObservationContext(@Nullable TransactionDefinition transactionDefinition, TransactionManager transactionManager) {
		this.transactionDefinition = transactionDefinition != null ? transactionDefinition : TransactionDefinition.withDefaults();
		this.transactionManagerClass = transactionManager != null ? transactionManager.getClass() : null;
	}

	public void setStatus(TransactionStatus status) {
		this.status = status;
	}

	public TransactionStatus getStatus() {
		return this.status;
	}

	public TransactionDefinition getTransactionDefinition() {
		return this.transactionDefinition;
	}

	@Nullable
	public Class<?> getTransactionManagerClass() {
		return this.transactionManagerClass;
	}

	public TransactionStatus getTransactionStatus() {
		return this.transactionStatus;
	}

	public void setTransactionStatus(TransactionStatus transactionStatus) {
		this.transactionStatus = transactionStatus;
	}
}
