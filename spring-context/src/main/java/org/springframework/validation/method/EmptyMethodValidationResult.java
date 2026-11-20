/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation.method;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSourceResolvable;

/**
 * {@link MethodValidationResult} with an empty list of results.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
final class EmptyMethodValidationResult implements MethodValidationResult {

	@Override
	public Object getTarget() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Method getMethod() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isForReturnValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ParameterValidationResult> getParameterValidationResults() {
		return Collections.emptyList();
	}

	@Override
	public List<MessageSourceResolvable> getCrossParameterValidationResults() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return "0 validation errors";
	}

}
