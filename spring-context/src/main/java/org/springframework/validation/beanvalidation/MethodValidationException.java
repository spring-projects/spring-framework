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
import java.util.Collections;
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


	/**
	 * Package private constructor for {@link MethodValidationAdapter}.
	 */
	MethodValidationException(
			Object target, Method method, boolean forReturnValue,
			Set<? extends ConstraintViolation<?>> violations, List<ParameterValidationResult> results) {

		super(violations);

		Assert.notNull(violations, "'violations' is required");
		Assert.notNull(results, "'results' is required");

		this.target = target;
		this.method = method;
		this.allValidationResults = results;
		this.forReturnValue = forReturnValue;
	}

	/**
	 * Private constructor copying from another {@code MethodValidationResult}.
	 */
	private MethodValidationException(MethodValidationResult other) {
		this(other.getTarget(), other.getMethod(), other.isForReturnValue(),
				other.getConstraintViolations(), other.getAllValidationResults());
	}


	// re-declare getConstraintViolations as NonNull

	@Override
	public Set<ConstraintViolation<?>> getConstraintViolations() {
		return super.getConstraintViolations();
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
	public String toString() {
		return "MethodValidationResult (" + getConstraintViolations().size() + " violations) " +
				"for " + this.method.toGenericString();
	}


	/**
	 * Create an exception copying from the given result, or return the same
	 * instance if it is a {@code MethodValidationException} already.
	 */
	public static MethodValidationException forResult(MethodValidationResult result) {
		return (result instanceof MethodValidationException ex ? ex : new MethodValidationException(result));
	}

	/**
	 * Create an exception for validation without errors.
	 */
	public static MethodValidationException forEmptyResult(Object target, Method method, boolean forReturnValue) {
		return new MethodValidationException(
				target, method, forReturnValue, Collections.emptySet(), Collections.emptyList());
	}


}
