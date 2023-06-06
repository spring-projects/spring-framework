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

/**
 * Extension of {@link ConstraintViolationException} that exposes an additional
 * list of {@link ParameterValidationResult} with violations adapted to
 * {@link org.springframework.context.MessageSourceResolvable} and grouped by
 * method parameter.
 *
 * <p>For {@link jakarta.validation.Valid @Valid}-annotated, Object method
 * parameters or return types with cascaded violations, the {@link ParameterErrors}
 * subclass of {@link ParameterValidationResult} implements
 * {@link org.springframework.validation.Errors} and exposes
 * {@link org.springframework.validation.FieldError field errors}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 * @see ParameterValidationResult
 * @see ParameterErrors
 * @see MethodValidationAdapter
 */
@SuppressWarnings("serial")
public class MethodValidationException extends ConstraintViolationException {

	private final Object target;

	private final Method method;

	private final List<ParameterValidationResult> allValidationResults;


	public MethodValidationException(
			Object target, Method method,
			List<ParameterValidationResult> validationResults,
			Set<? extends ConstraintViolation<?>> violations) {

		super(violations);
		this.target = target;
		this.method = method;
		this.allValidationResults = validationResults;
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
	 * Return all validation results. This includes method parameters with
	 * constraints declared on them, as well as
	 * {@link jakarta.validation.Valid @Valid} method parameters with
	 * cascaded constraints.
	 * @see #getValueResults()
	 * @see #getBeanResults()
	 */
	public List<ParameterValidationResult> getAllValidationResults() {
		return this.allValidationResults;
	}

	/**
	 * Return only validation results for method parameters with constraints
	 * declared directly on them. This excludes
	 * {@link jakarta.validation.Valid @Valid} method parameters with cascaded
	 * constraints.
	 * @see #getAllValidationResults()
	 */
	public List<ParameterValidationResult> getValueResults() {
		return this.allValidationResults.stream()
				.filter(result -> !(result instanceof ParameterErrors))
				.toList();
	}

	/**
	 * Return only validation results for {@link jakarta.validation.Valid @Valid}
	 * method parameters with cascaded constraints. This excludes method
	 * parameters with constraints declared directly on them.
	 * @see #getAllValidationResults()
	 */
	public List<ParameterErrors> getBeanResults() {
		return this.allValidationResults.stream()
				.filter(result -> result instanceof ParameterErrors)
				.map(result -> (ParameterErrors) result)
				.toList();
	}

}
