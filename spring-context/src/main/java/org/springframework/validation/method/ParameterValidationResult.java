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

import java.util.Collection;
import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Store and expose the results of method validation for a method parameter.
 * <ul>
 * <li>Validation errors directly on method parameter values are exposed as a
 * list of {@link MessageSourceResolvable}s.
 * <li>Nested validation errors on an Object method parameter are exposed as
 * {@link org.springframework.validation.Errors} by the subclass
 * {@link ParameterErrors}.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class ParameterValidationResult {

	private final MethodParameter methodParameter;

	@Nullable
	private final Object argument;

	private final List<MessageSourceResolvable> resolvableErrors;


	/**
	 * Create a {@code ParameterValidationResult}.
	 */
	public ParameterValidationResult(
			MethodParameter param, @Nullable Object arg, Collection<? extends MessageSourceResolvable> errors) {

		Assert.notNull(param, "MethodParameter is required");
		Assert.notEmpty(errors, "`resolvableErrors` must not be empty");
		this.methodParameter = param;
		this.argument = arg;
		this.resolvableErrors = List.copyOf(errors);
	}


	/**
	 * The method parameter the validation results are for.
	 */
	public MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	/**
	 * The method argument value that was validated.
	 */
	@Nullable
	public Object getArgument() {
		return this.argument;
	}

	/**
	 * List of {@link MessageSourceResolvable} representations adapted from the
	 * validation errors of the validation library.
	 * <ul>
	 * <li>For a constraints directly on a method parameter, error codes are
	 * based on the names of the constraint annotation, the object, the method,
	 * the parameter, and parameter type, e.g.
	 * {@code ["Max.myObject#myMethod.myParameter", "Max.myParameter", "Max.int", "Max"]}.
	 * Arguments include the parameter itself as a {@link MessageSourceResolvable}, e.g.
	 * {@code ["myObject#myMethod.myParameter", "myParameter"]}, followed by actual
	 * constraint annotation attributes (i.e. excluding "message", "groups" and
	 * "payload") in alphabetical order of attribute names.
	 * <li>For cascaded constraints via {@link jakarta.validation.Validator @Valid}
	 * on a bean method parameter, this method returns
	 * {@link org.springframework.validation.FieldError field errors} that you
	 * can also access more conveniently through methods of the
	 * {@link ParameterErrors} sub-class.
	 * </ul>
	 */
	public List<MessageSourceResolvable> getResolvableErrors() {
		return this.resolvableErrors;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		ParameterValidationResult otherResult = (ParameterValidationResult) other;
		return (getMethodParameter().equals(otherResult.getMethodParameter()) &&
				ObjectUtils.nullSafeEquals(getArgument(), otherResult.getArgument()));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + getMethodParameter().hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getArgument());
		return hashCode;
	}

	@Override
	public String toString() {
		return "Validation results for method parameter '" + this.methodParameter +
				"': argument [" + ObjectUtils.nullSafeConciseToString(this.argument) + "]; " +
				getResolvableErrors();
	}

}
