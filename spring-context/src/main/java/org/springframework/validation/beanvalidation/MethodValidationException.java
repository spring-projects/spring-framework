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

package org.springframework.validation.beanvalidation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.util.Assert;

/**
 * Extension of {@link ConstraintViolationException} that implements
 * {@link MethodValidationResult} exposing an additional list of
 * {@link ParameterValidationResult} that represents violations adapted to
 * {@link org.springframework.context.MessageSourceResolvable} and grouped by
 * method parameter.
 *
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 * @see ParameterValidationResult
 * @see ParameterErrors
 * @see MethodValidationAdapter
 */
@SuppressWarnings("serial")
public class MethodValidationException extends ConstraintViolationException implements MethodValidationResult {

	private final Object target;

	private final Method method;

	private final List<ParameterValidationResult> allValidationResults;

	private final boolean forReturnValue;


	public MethodValidationException(
			Object target, Method method, Set<? extends ConstraintViolation<?>> violations,
			List<ParameterValidationResult> validationResults, boolean forReturnValue) {

		super(violations);
		Assert.notEmpty(violations, "'violations' must not be empty");
		this.target = target;
		this.method = method;
		this.allValidationResults = validationResults;
		this.forReturnValue = forReturnValue;
	}


	/**
	 * Return the target of the method invocation to which validation was applied.
	 */
	public Object getTarget() {
		return this.target;
	}

	/**
	 * Return the method to which validation was applied.
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Whether the violations are for a return value.
	 * If true the violations are from validating a return value.
	 * If false the violations are from validating method arguments.
	 */
	public boolean isForReturnValue() {
		return this.forReturnValue;
	}

	// re-declare parent class method for NonNull treatment of interface

	@Override
	public Set<ConstraintViolation<?>> getConstraintViolations() {
		return super.getConstraintViolations();
	}

	@Override
	public List<ParameterValidationResult> getAllValidationResults() {
		return this.allValidationResults;
	}

	@Override
	public List<ParameterValidationResult> getValueResults() {
		return this.allValidationResults.stream()
				.filter(result -> !(result instanceof ParameterErrors))
				.toList();
	}

	@Override
	public List<ParameterErrors> getBeanResults() {
		return this.allValidationResults.stream()
				.filter(result -> result instanceof ParameterErrors)
				.map(result -> (ParameterErrors) result)
				.toList();
	}

	@Override
	public void throwIfViolationsPresent() {
		throw this;
	}

	@Override
	public String toString() {
		return "MethodValidationResult (" + getConstraintViolations().size() + " violations) " +
				"for " + this.method.toGenericString();
	}

}
