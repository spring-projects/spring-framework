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

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * Contract to apply method validation without directly using
 * {@link MethodValidationAdapter}. For use in components where Jakarta Bean
 * Validation is an optional dependency and may or may not be present on the
 * classpath. If that's not a concern, use {@code MethodValidationAdapter}
 * directly.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 * @see DefaultMethodValidator
 */
public interface MethodValidator {

	/**
	 * Use this method determine the validation groups to pass into
	 * {@link #validateArguments(Object, Method, MethodParameter[], Object[], Class[])} and
	 * {@link #validateReturnValue(Object, Method, MethodParameter, Object, Class[])}.
	 * @param target the target Object
	 * @param method the target method
	 * @return the applicable validation groups as a {@code Class} array
	 * @see MethodValidationAdapter#determineValidationGroups(Object, Method)
	 */
	Class<?>[] determineValidationGroups(Object target, Method method);

	/**
	 * Validate the given method arguments and return the result of validation.
	 * @param target the target Object
	 * @param method the target method
	 * @param parameters the parameters, if already created and available
	 * @param arguments the candidate argument values to validate
	 * @param groups groups for validation determined via
	 * {@link #determineValidationGroups(Object, Method)}
	 * @throws MethodValidationException should be raised in case of validation
	 * errors unless the implementation handles those errors otherwise (e.g.
	 * by injecting {@code BindingResult} into the method).
	 */
	void validateArguments(
			Object target, Method method, @Nullable MethodParameter[] parameters, Object[] arguments,
			Class<?>[] groups);

	/**
	 * Validate the given return value and return the result of validation.
	 * @param target the target Object
	 * @param method the target method
	 * @param returnType the return parameter, if already created and available
	 * @param returnValue the return value to validate
	 * @param groups groups for validation determined via
	 * {@link #determineValidationGroups(Object, Method)}
	 * @throws MethodValidationException in case of validation errors
	 */
	void validateReturnValue(
			Object target, Method method, @Nullable MethodParameter returnType, @Nullable Object returnValue,
			Class<?>[] groups);

}
