/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.validation;

import org.springframework.lang.Nullable;

/**
 * Extended variant of the {@link Validator} interface, adding support for
 * validation 'hints'.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public interface SmartValidator extends Validator {

	/**
	 * Validate the supplied {@code target} object, which must be of a type of {@link Class}
	 * for which the {@link #supports(Class)} method typically returns {@code true}.
	 * <p>The supplied {@link Errors errors} instance can be used to report any
	 * resulting validation errors.
	 * <p><b>This variant of {@code validate()} supports validation hints, such as
	 * validation groups against a JSR-303 provider</b> (in which case, the provided hint
	 * objects need to be annotation arguments of type {@code Class}).
	 * <p>Note: Validation hints may get ignored by the actual target {@code Validator},
	 * in which case this method should behave just like its regular
	 * {@link #validate(Object, Errors)} sibling.
	 * @param target the object that is to be validated
	 * @param errors contextual state about the validation process
	 * @param validationHints one or more hint objects to be passed to the validation engine
	 * @see javax.validation.Validator#validate(Object, Class[])
	 */
	void validate(Object target, Errors errors, Object... validationHints);

	/**
	 * Validate the supplied value for the specified field on the target type,
	 * reporting the same validation errors as if the value would be bound to
	 * the field on an instance of the target class.
	 * @param targetType the target type
	 * @param fieldName the name of the field
	 * @param value the candidate value
	 * @param errors contextual state about the validation process
	 * @param validationHints one or more hint objects to be passed to the validation engine
	 * @since 5.1
	 * @see javax.validation.Validator#validateValue(Class, String, Object, Class[])
	 */
	default void validateValue(
			Class<?> targetType, String fieldName, @Nullable Object value, Errors errors, Object... validationHints) {

		throw new IllegalArgumentException("Cannot validate individual value for " + targetType);
	}

}
