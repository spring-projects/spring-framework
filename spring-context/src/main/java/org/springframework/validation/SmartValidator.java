/*
 * Copyright 2002-2012 the original author or authors.
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
	 * @param target the object that is to be validated (can be {@code null})
	 * @param errors contextual state about the validation process (never {@code null})
	 * @param validationHints one or more hint objects to be passed to the validation engine
	 * @see ValidationUtils
	 */
	void validate(@Nullable Object target, Errors errors, Object... validationHints);

}
