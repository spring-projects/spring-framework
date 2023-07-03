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

package org.springframework.validation.method;

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * Contract to apply method validation and handle the results.
 * Exposes methods that return {@link MethodValidationResult}, and methods that
 * handle the results, by default raising {@link MethodValidationException}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public interface MethodValidator {

	/**
	 * Use this method to determine the validation groups.
	 * @param target the target Object
	 * @param method the target method
	 * @return the applicable validation groups as a {@code Class} array
	 */
	Class<?>[] determineValidationGroups(Object target, Method method);

	/**
	 * Validate the given method arguments and handle the result.
	 * @param target the target Object
	 * @param method the target method
	 * @param parameters the parameters, if already created and available
	 * @param arguments the candidate argument values to validate
	 * @param groups validation groups via {@link #determineValidationGroups}
	 * @throws MethodValidationException raised by default in case of validation errors.
	 * Implementations may provide alternative handling, possibly not raise an exception
	 * but for example inject errors into the method, or raise a different exception,
	 * one that also implements {@link MethodValidationResult}.
	 */
	default void applyArgumentValidation(
			Object target, Method method, @Nullable MethodParameter[] parameters,
			Object[] arguments, Class<?>[] groups) {

		MethodValidationResult result = validateArguments(target, method, parameters, arguments, groups);
		if (result.hasErrors()) {
			throw new MethodValidationException(result);
		}
	}

	/**
	 * Validate the given method arguments and return validation results.
	 * @param target the target Object
	 * @param method the target method
	 * @param parameters the parameters, if already created and available
	 * @param arguments the candidate argument values to validate
	 * @param groups validation groups from {@link #determineValidationGroups}
	 * @return the result of validation
	 */
	MethodValidationResult validateArguments(
			Object target, Method method, @Nullable MethodParameter[] parameters,
			Object[] arguments, Class<?>[] groups);

	/**
	 * Validate the given return value and handle the results.
	 * @param target the target Object
	 * @param method the target method
	 * @param returnType the return parameter, if already created and available
	 * @param returnValue the return value to validate
	 * @param groups validation groups from {@link #determineValidationGroups}
	 * @throws MethodValidationException raised by default in case of validation errors.
	 * Implementations may provide alternative handling, or raise a different exception,
	 * one that also implements {@link MethodValidationResult}.
	 */
	default void applyReturnValueValidation(
			Object target, Method method, @Nullable MethodParameter returnType,
			@Nullable Object returnValue, Class<?>[] groups) {

		MethodValidationResult result = validateReturnValue(target, method, returnType, returnValue, groups);
		if (result.hasErrors()) {
			throw new MethodValidationException(result);
		}
	}

	/**
	 * Validate the given return value and return the result of validation.
	 * @param target the target Object
	 * @param method the target method
	 * @param returnType the return parameter, if already created and available
	 * @param returnValue the return value to validate
	 * @param groups validation groups from {@link #determineValidationGroups}
	 * @return the result of validation
	 */
	MethodValidationResult validateReturnValue(
			Object target, Method method, @Nullable MethodParameter returnType,
			@Nullable Object returnValue, Class<?>[] groups);

}
