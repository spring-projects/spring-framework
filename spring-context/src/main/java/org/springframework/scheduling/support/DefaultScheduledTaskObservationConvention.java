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
import io.micrometer.common.KeyValues;

import org.springframework.util.StringUtils;

import static org.springframework.scheduling.support.ScheduledTaskObservationDocumentation.LowCardinalityKeyNames;

/**
 * Default implementation for {@link ScheduledTaskObservationConvention}.
 * @author Brian Clozel
 * @since 6.1
 */
public class DefaultScheduledTaskObservationConvention implements ScheduledTaskObservationConvention {

	private static final String DEFAULT_NAME = "tasks.scheduled.execution";

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(LowCardinalityKeyNames.EXCEPTION, KeyValue.NONE_VALUE);

	private static final KeyValue OUTCOME_SUCCESS = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "SUCCESS");

	private static final KeyValue OUTCOME_ERROR = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "ERROR");

	private static final KeyValue OUTCOME_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	@Override
	public String getContextualName(ScheduledTaskObservationContext context) {
		return "task " + StringUtils.uncapitalize(context.getTargetClass().getSimpleName())
				+ "." + context.getMethod().getName();
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ScheduledTaskObservationContext context) {
		return KeyValues.of(codeFunction(context), codeNamespace(context), exception(context), outcome(context));
	}

	protected KeyValue codeFunction(ScheduledTaskObservationContext context) {
		return KeyValue.of(LowCardinalityKeyNames.CODE_FUNCTION, context.getMethod().getName());
	}

	protected KeyValue codeNamespace(ScheduledTaskObservationContext context) {
		return KeyValue.of(LowCardinalityKeyNames.CODE_NAMESPACE, context.getTargetClass().getCanonicalName());
	}

	protected KeyValue exception(ScheduledTaskObservationContext context) {
		if (context.getError() != null) {
			return KeyValue.of(LowCardinalityKeyNames.EXCEPTION, context.getError().getClass().getSimpleName());
		}
		return EXCEPTION_NONE;
	}

	protected KeyValue outcome(ScheduledTaskObservationContext context) {
		if (context.getError() != null) {
			return OUTCOME_ERROR;
		}
		if (!context.isComplete()) {
			return OUTCOME_UNKNOWN;
		}
		return OUTCOME_SUCCESS;
	}

}
