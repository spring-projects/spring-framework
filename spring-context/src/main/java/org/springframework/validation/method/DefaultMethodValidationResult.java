/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation.method;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.util.Assert;

/**
 * Default {@link MethodValidationResult} implementation as a simple container.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
final class DefaultMethodValidationResult implements MethodValidationResult {

	private final Object target;

	private final Method method;

	private final List<ParameterValidationResult> parameterValidationResults;

	private final List<MessageSourceResolvable> crossParamResults;

	private final boolean forReturnValue;


	DefaultMethodValidationResult(
			Object target, Method method, List<ParameterValidationResult> results,
			List<MessageSourceResolvable> crossParamResults) {

		Assert.isTrue(!results.isEmpty() || !crossParamResults.isEmpty(), "Expected validation results");
		Assert.notNull(target, "'target' is required");
		Assert.notNull(method, "Method is required");

		this.target = target;
		this.method = method;
		this.parameterValidationResults = results;
		this.crossParamResults = crossParamResults;
		this.forReturnValue = (!results.isEmpty() && results.get(0).getMethodParameter().getParameterIndex() == -1);
	}


	@Override
	public Object getTarget() {
		return this.target;
	}

	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public boolean isForReturnValue() {
		return this.forReturnValue;
	}

	@Override
	public List<ParameterValidationResult> getParameterValidationResults() {
		return this.parameterValidationResults;
	}

	@Override
	public List<MessageSourceResolvable> getCrossParameterValidationResults() {
		return this.crossParamResults;
	}

	@Override
	public String toString() {
		return getAllErrors().size() + " validation errors " +
				"for " + (isForReturnValue() ? "return value" : "arguments") + " of " +
				this.method.toGenericString();
	}

}
