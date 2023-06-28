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

/**
 * Container for method validation results where underlying
 * {@link ConstraintViolation violations} have been adapted to
 * {@link ParameterValidationResult} each containing a list of
 * {@link org.springframework.context.MessageSourceResolvable} grouped by method
 * parameter.
 *
 * <p>For {@link jakarta.validation.Valid @Valid}-annotated, Object method
 * parameters or return types with cascaded violations, the {@link ParameterErrors}
 * subclass of {@link ParameterValidationResult} implements
 * {@link org.springframework.validation.Errors} and exposes
 * {@link org.springframework.validation.FieldError field errors}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public interface MethodValidationResult {

	/**
	 * Return the target of the method invocation to which validation was applied.
	 */
	Object getTarget();

	/**
	 * Return the method to which validation was applied.
	 */
	Method getMethod();

	/**
	 * Whether the violations are for a return value.
	 * If true the violations are from validating a return value.
	 * If false the violations are from validating method arguments.
	 */
	boolean isForReturnValue();

	/**
	 * Whether the result contains any {@link ConstraintViolation}s.
	 */
	default boolean hasViolations() {
		return !getConstraintViolations().isEmpty();
	}

	/**
	 * Returns the set of constraint violations reported during a validation.
	 * @return the {@code Set} of {@link ConstraintViolation}s, or an empty Set
	 */
	Set<ConstraintViolation<?>> getConstraintViolations();

	/**
	 * Return all validation results. This includes method parameters with
	 * constraints declared on them, as well as
	 * {@link jakarta.validation.Valid @Valid} method parameters with
	 * cascaded constraints.
	 * @see #getValueResults()
	 * @see #getBeanResults()
	 */
	List<ParameterValidationResult> getAllValidationResults();

	/**
	 * Return only validation results for method parameters with constraints
	 * declared directly on them. This excludes
	 * {@link jakarta.validation.Valid @Valid} method parameters with cascaded
	 * constraints.
	 * @see #getAllValidationResults()
	 */
	List<ParameterValidationResult> getValueResults();

	/**
	 * Return only validation results for {@link jakarta.validation.Valid @Valid}
	 * method parameters with cascaded constraints. This excludes method
	 * parameters with constraints declared directly on them.
	 * @see #getAllValidationResults()
	 */
	List<ParameterErrors> getBeanResults();

}
