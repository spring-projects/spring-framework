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

import io.micrometer.core.instrument.docs.DocumentedObservation;
import io.micrometer.core.instrument.docs.TagKey;

enum TransactionObservation implements DocumentedObservation {

	/**
	 * Observation created when there was no previous transaction. If there was one, we will
	 * continue it unless propagation is required.
	 */
	TX_OBSERVATION {
		@Override
		public String getName() {
			return "spring.tx";
		}

		@Override
		public String getContextualName() {
			return "tx";
		}

		@Override
		public TagKey[] getLowCardinalityTagKeys() {
			return LowCardinalityTags.values();
		}

		@Override
		public TagKey[] getHighCardinalityTagKeys() {
			return HighCardinalityTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.tx";
		}
	};

	enum LowCardinalityTags implements TagKey {

		/**
		 * Name of the TransactionManager.
		 */
		TRANSACTION_MANAGER {
			@Override
			public String getKey() {
				return "spring.tx.transaction-manager";
			}
		},

		/**
		 * Whether the transaction is read-only.
		 */
		READ_ONLY {
			@Override
			public String getKey() {
				return "spring.tx.read-only";
			}
		},

		/**
		 * Transaction propagation level.
		 */
		PROPAGATION_LEVEL {
			@Override
			public String getKey() {
				return "spring.tx.propagation-level";
			}
		},

		/**
		 * Transaction isolation level.
		 */
		ISOLATION_LEVEL {
			@Override
			public String getKey() {
				return "spring.tx.isolation-level";
			}
		},

		/**
		 * Transaction timeout.
		 */
		TIMEOUT {
			@Override
			public String getKey() {
				return "spring.tx.timeout";
			}
		}

	}

	enum HighCardinalityTags implements TagKey {

		/**
		 * Transaction name.
		 */
		NAME {
			@Override
			public String getKey() {
				return "spring.tx.name";
			}
		}
	}
}
