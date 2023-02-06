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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

enum TransactionObservation implements ObservationDocumentation {

	/**
	 * Observation created when there was no previous transaction. If there was one, we will
	 * continue it unless propagation is required.
	 */
	TX_OBSERVATION {

		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultTransactionObservationConvention.class;
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return HighCardinalityKeyNames.values();
		}

		@Override
		public String getPrefix() {
			return "spring.tx";
		}
	};

	enum LowCardinalityKeyNames implements KeyName {

		/**
		 * Name of the TransactionManager.
		 */
		TRANSACTION_MANAGER {
			@Override
			public String asString() {
				return "spring.tx.transaction-manager";
			}
		},

		/**
		 * Whether the transaction is read-only.
		 */
		READ_ONLY {
			@Override
			public String asString() {
				return "spring.tx.read-only";
			}
		},

		/**
		 * Transaction propagation level.
		 */
		PROPAGATION_LEVEL {
			@Override
			public String asString() {
				return "spring.tx.propagation-level";
			}
		},

		/**
		 * Transaction isolation level.
		 */
		ISOLATION_LEVEL {
			@Override
			public String asString() {
				return "spring.tx.isolation-level";
			}
		},

		/**
		 * Transaction timeout.
		 */
		TIMEOUT {
			@Override
			public String asString() {
				return "spring.tx.timeout";
			}
		}

	}

	enum HighCardinalityKeyNames implements KeyName {

		/**
		 * Transaction name.
		 */
		NAME {
			@Override
			public String asString() {
				return "spring.tx.name";
			}
		}
	}
}
