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
 * Exception that is a {@link MethodValidationResult}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 * @see MethodValidator
 */
@SuppressWarnings("serial")
public class MethodValidationException extends RuntimeException implements MethodValidationResult {

	private final MethodValidationResult validationResult;


	public MethodValidationException(MethodValidationResult validationResult) {
		super(validationResult.toString());
		Assert.notNull(validationResult, "MethodValidationResult is required");
		this.validationResult = validationResult;
	}


	@Override
	public Object getTarget() {
		return this.validationResult.getTarget();
	}

	@Override
	public Method getMethod() {
		return this.validationResult.getMethod();
	}

	@Override
	public boolean isForReturnValue() {
		return this.validationResult.isForReturnValue();
	}

	@Override
	public List<ParameterValidationResult> getParameterValidationResults() {
		return this.validationResult.getParameterValidationResults();
	}

	@Override
	public List<MessageSourceResolvable> getCrossParameterValidationResults() {
		return this.validationResult.getCrossParameterValidationResults();
	}

}
