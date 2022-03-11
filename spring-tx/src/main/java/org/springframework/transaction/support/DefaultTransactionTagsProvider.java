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

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.observation.Observation;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation for {@link TransactionTagsProvider}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public class DefaultTransactionTagsProvider implements TransactionTagsProvider {

	private static final Propagation[] PROPAGATIONS = Propagation.values();

	private static final Isolation[] ISOLATIONS = Isolation.values();

	@Override
	public Tags getLowCardinalityTags(TransactionObservationContext context) {
		if (!context.getTransactionStatus().isNewTransaction()) {
			return Tags.empty();
		}
		TransactionDefinition transactionDefinition = context.getTransactionDefinition();
		Tags tags = Tags.empty();
		if (transactionDefinition.getTimeout() > 0) {
			tags = tags.and(TransactionObservation.LowCardinalityTags.TIMEOUT.of(String.valueOf(transactionDefinition.getTimeout())));
		}
		return tags.and(TransactionObservation.LowCardinalityTags.TRANSACTION_MANAGER.of(ClassUtils.getQualifiedName(context.getTransactionManagerClass())),
				TransactionObservation.LowCardinalityTags.READ_ONLY.of(String.valueOf(transactionDefinition.isReadOnly())),
				TransactionObservation.LowCardinalityTags.PROPAGATION_LEVEL.of(propagationLevel(transactionDefinition)),
				TransactionObservation.LowCardinalityTags.ISOLATION_LEVEL.of(isolationLevel(transactionDefinition)));
	}

	@Override
	public Tags getHighCardinalityTags(TransactionObservationContext context) {
		TransactionDefinition transactionDefinition = context.getTransactionDefinition();
		if (StringUtils.hasText(transactionDefinition.getName())) {
			return Tags.of(TransactionObservation.HighCardinalityTags.NAME.of(transactionDefinition.getName()));
		}
		return Tags.empty();
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof TransactionObservationContext;
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
