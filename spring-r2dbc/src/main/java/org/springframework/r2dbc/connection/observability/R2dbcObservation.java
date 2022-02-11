/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.observability;

import io.micrometer.api.instrument.docs.DocumentedObservation;
import io.micrometer.api.instrument.docs.TagKey;

enum R2dbcObservation implements DocumentedObservation {

	/**
	 * Observation created for a R2DBC query.
	 */
	R2DBC_QUERY_OBSERVATION {
		@Override
		public String getName() {
			return "r2dbc.query";
		}

		@Override
		public String getContextualName() {
			return "query";
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
			return "r2dbc.";
		}
	};

	enum LowCardinalityTags implements TagKey {

		/**
		 * Name of the R2DBC connection.
		 */
		CONNECTION {
			@Override
			public String getKey() {
				return "r2dbc.connection";
			}
		},

		/**
		 * R2DBC url.
		 */
		URL {
			@Override
			public String getKey() {
				return "r2dbc.url";
			}
		},

		/**
		 * Bean name of a ConnectionFactory.
		 */
		BEAN_NAME {
			@Override
			public String getKey() {
				return "r2dbc.connection.bean-name";
			}
		},

		/**
		 * Name of the R2DBC thread.
		 */
		THREAD {
			@Override
			public String getKey() {
				return "r2dbc.thread";
			}
		}
	}

	enum HighCardinalityTags implements TagKey {
		/**
		 * Name of the R2DBC query.
		 */
		QUERY {
			@Override
			public String getKey() {
				return "r2dbc.query[%s]";
			}
		},

	}

}
