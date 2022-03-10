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
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation for {@link TransactionTagsProvider}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public class DefaultTransactionTagsProvider implements TransactionTagsProvider {

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
		if (StringUtils.hasText(transactionDefinition.getName())) {
			tags = tags.and(TransactionObservation.LowCardinalityTags.NAME.of(transactionDefinition.getName()));
		}
		return tags.and(TransactionObservation.LowCardinalityTags.TRANSACTION_MANAGER.of(ClassUtils.getQualifiedName(context.getTransactionManagerClass())),
				TransactionObservation.LowCardinalityTags.READ_ONLY.of(String.valueOf(transactionDefinition.isReadOnly())),
				TransactionObservation.LowCardinalityTags.PROPAGATION_LEVEL.of(propagationLevel(transactionDefinition)),
				TransactionObservation.LowCardinalityTags.ISOLATION_LEVEL.of(isolationLevel(transactionDefinition)));
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof TransactionObservationContext;
	}


	private static String propagationLevel(TransactionDefinition def) {
		switch (def.getPropagationBehavior()) {
		case 0:
			return "PROPAGATION_REQUIRED";
		case 1:
			return "PROPAGATION_SUPPORTS";
		case 2:
			return "PROPAGATION_MANDATORY";
		case 3:
			return "PROPAGATION_REQUIRES_NEW";
		case 4:
			return "PROPAGATION_NOT_SUPPORTED";
		case 5:
			return "PROPAGATION_NEVER";
		case 6:
			return "PROPAGATION_NESTED";
		default:
			return String.valueOf(def.getPropagationBehavior());
		}
	}

	private static String isolationLevel(TransactionDefinition def) {
		switch (def.getIsolationLevel()) {
		case -1:
			return "ISOLATION_DEFAULT";
		case 1:
			return "ISOLATION_READ_UNCOMMITTED";
		case 2:
			return "ISOLATION_READ_COMMITTED";
		case 4:
			return "ISOLATION_REPEATABLE_READ";
		case 8:
			return "ISOLATION_SERIALIZABLE";
		default:
			return String.valueOf(def.getIsolationLevel());
		}
	}
}
