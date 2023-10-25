/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.scheduling.support;

import io.micrometer.common.KeyValue;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documented {@link io.micrometer.common.KeyValue KeyValues} for the observations on
 * executions of {@link org.springframework.scheduling.annotation.Scheduled scheduled tasks}
 *
 * <p>This class is used by automated tools to document KeyValues attached to the
 * {@code @Scheduled} observations.
 *
 * @author Brian Clozel
 * @since 6.1
 */
public enum ScheduledTaskObservationDocumentation implements ObservationDocumentation {

	/**
	 * Observations on executions of {@link org.springframework.scheduling.annotation.Scheduled} tasks.
	 */
	TASKS_SCHEDULED_EXECUTION {
		@Override
		public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
			return DefaultScheduledTaskObservationConvention.class;
		}
		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return LowCardinalityKeyNames.values();
		}
		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return new KeyName[] {};
		}
	};


	public enum LowCardinalityKeyNames implements KeyName {

		/**
		 * Name of the method that is executed for the scheduled task.
		 */
		CODE_FUNCTION {
			@Override
			public String asString() {
				return "code.function";
			}
		},

		/**
		 * {@link Class#getCanonicalName() Canonical name} of the target type that owns the scheduled method.
		 */
		CODE_NAMESPACE {
			@Override
			public String asString() {
				return "code.namespace";
			}
		},

		/**
		 * Name of the exception thrown during task execution, or {@value KeyValue#NONE_VALUE} if no exception was thrown.
		 */
		EXCEPTION {
			@Override
			public String asString() {
				return "exception";
			}
		},

		/**
		 * Outcome of the scheduled task execution.
		 */
		OUTCOME {
			@Override
			public String asString() {
				return "outcome";
			}
		}

	}

}
