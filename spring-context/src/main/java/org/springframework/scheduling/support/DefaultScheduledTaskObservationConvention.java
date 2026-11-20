/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.support;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.scheduling.support.ScheduledTaskObservationDocumentation.LowCardinalityKeyNames;
import org.springframework.util.StringUtils;

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

	private static final KeyValue CODE_NAMESPACE_ANONYMOUS = KeyValue.of(LowCardinalityKeyNames.CODE_NAMESPACE, "ANONYMOUS");

	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	@Override
	public String getContextualName(ScheduledTaskObservationContext context) {
		return "task " + StringUtils.uncapitalize(context.getTargetClass().getSimpleName()) +
				"." + context.getMethod().getName();
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ScheduledTaskObservationContext context) {
		return KeyValues.of(codeFunction(context), codeNamespace(context), exception(context), outcome(context));
	}

	protected KeyValue codeFunction(ScheduledTaskObservationContext context) {
		return KeyValue.of(LowCardinalityKeyNames.CODE_FUNCTION, context.getMethod().getName());
	}

	protected KeyValue codeNamespace(ScheduledTaskObservationContext context) {
		if (context.getTargetClass().getCanonicalName() != null) {
			return KeyValue.of(LowCardinalityKeyNames.CODE_NAMESPACE, context.getTargetClass().getCanonicalName());
		}
		return CODE_NAMESPACE_ANONYMOUS;
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
