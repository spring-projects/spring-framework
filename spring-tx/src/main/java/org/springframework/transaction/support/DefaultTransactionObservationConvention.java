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

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation for {@link TransactionObservationConvention}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public class DefaultTransactionObservationConvention implements TransactionObservationConvention {

	private static final Propagation[] PROPAGATIONS = Propagation.values();

	private static final Isolation[] ISOLATIONS = Isolation.values();

	@Override
	public KeyValues getLowCardinalityKeyValues(TransactionObservationContext context) {
		if (context.getTransactionStatus() == null || !context.getTransactionStatus().isNewTransaction()) {
			return KeyValues.empty();
		}
		TransactionDefinition transactionDefinition = context.getTransactionDefinition();
		KeyValues tags = KeyValues.empty();
		if (transactionDefinition.getTimeout() > 0) {
			tags = tags.and(TransactionObservation.LowCardinalityKeyNames.TIMEOUT.withValue(String.valueOf(transactionDefinition.getTimeout())));
		}
		return tags.and(TransactionObservation.LowCardinalityKeyNames.TRANSACTION_MANAGER.withValue(ClassUtils.getQualifiedName(context.getTransactionManagerClass())),
				TransactionObservation.LowCardinalityKeyNames.READ_ONLY.withValue(String.valueOf(transactionDefinition.isReadOnly())),
				TransactionObservation.LowCardinalityKeyNames.PROPAGATION_LEVEL.withValue(propagationLevel(transactionDefinition)),
				TransactionObservation.LowCardinalityKeyNames.ISOLATION_LEVEL.withValue(isolationLevel(transactionDefinition)));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(TransactionObservationContext context) {
		if (context.getTransactionStatus() == null || !context.getTransactionStatus().isNewTransaction()) {
			return KeyValues.empty();
		}
		TransactionDefinition transactionDefinition = context.getTransactionDefinition();
		if (StringUtils.hasText(transactionDefinition.getName())) {
			return KeyValues.of(TransactionObservation.HighCardinalityKeyNames.NAME.withValue(transactionDefinition.getName()));
		}
		return KeyValues.empty();
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof TransactionObservationContext;
	}

	@Override
	public String getName() {
		return "spring.tx";
	}

	@Override
	public String getContextualName(TransactionObservationContext context) {
		return "tx";
	}

	private static String propagationLevel(TransactionDefinition def) {
		if (def.getPropagationBehavior() < 0 || def.getPropagationBehavior() >= PROPAGATIONS.length) {
			return String.valueOf(def.getPropagationBehavior());
		}
		return PROPAGATIONS[def.getPropagationBehavior()].name();
	}

	private static String isolationLevel(TransactionDefinition def) {
		if (def.getIsolationLevel() < 0 || def.getIsolationLevel() >= ISOLATIONS.length) {
			return String.valueOf(def.getIsolationLevel());
		}
		return ISOLATIONS[def.getIsolationLevel()].name();
	}
}
