/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.function.BiFunction;

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
 * <p>When the method parameter is a container such as a {@link List}, array,
 * or {@link java.util.Map}, then a separate {@link ParameterValidationResult}
 * is created for each element with errors. In that case, the properties
 * {@link #getContainer() container}, {@link #getContainerIndex() containerIndex},
 * and {@link #getContainerKey() containerKey} provide additional context.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class ParameterValidationResult {

	private final MethodParameter methodParameter;

	@Nullable
	private final Object argument;

	private final List<MessageSourceResolvable> resolvableErrors;

	@Nullable
	private final Object container;

	@Nullable
	private final Integer containerIndex;

	@Nullable
	private final Object containerKey;

	private final BiFunction<MessageSourceResolvable, Class<?>, Object> sourceLookup;


	/**
	 * Create a {@code ParameterValidationResult}.
	 */
	public ParameterValidationResult(
			MethodParameter param, @Nullable Object arg, Collection<? extends MessageSourceResolvable> errors,
			@Nullable Object container, @Nullable Integer index, @Nullable Object key,
			BiFunction<MessageSourceResolvable, Class<?>, Object> sourceLookup) {

		Assert.notNull(param, "MethodParameter is required");
		Assert.notEmpty(errors, "`resolvableErrors` must not be empty");
		this.methodParameter = param;
		this.argument = arg;
		this.resolvableErrors = List.copyOf(errors);
		this.container = container;
		this.containerIndex = index;
		this.containerKey = key;
		this.sourceLookup = sourceLookup;
	}

	/**
	 * Create a {@code ParameterValidationResult}.
	 * @deprecated in favor of
	 * {@link ParameterValidationResult#ParameterValidationResult(MethodParameter, Object, Collection, Object, Integer, Object, BiFunction)}
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public ParameterValidationResult(
			MethodParameter param, @Nullable Object arg, Collection<? extends MessageSourceResolvable> errors,
			@Nullable Object container, @Nullable Integer index, @Nullable Object key) {

		this(param, arg, errors, container, index, key, (error, sourceType) -> {
			throw new IllegalArgumentException("No source object of the given type");
		});
	}

	/**
	 * Create a {@code ParameterValidationResult}.
	 * @deprecated in favor of
	 * {@link ParameterValidationResult#ParameterValidationResult(MethodParameter, Object, Collection, Object, Integer, Object, BiFunction)}
	 */
	@Deprecated(since = "6.1.3", forRemoval = true)
	public ParameterValidationResult(
			MethodParameter param, @Nullable Object arg, Collection<? extends MessageSourceResolvable> errors) {

		this(param, arg, errors, null, null, null, (error, sourceType) -> {
			throw new IllegalArgumentException("No source object of the given type");
		});
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

	/**
	 * When {@code @Valid} is declared on a container of elements such as
	 * {@link java.util.Collection}, {@link java.util.Map},
	 * {@link java.util.Optional}, and others, this method returns the container
	 * of the validated {@link #getArgument() argument}, while
	 * {@link #getContainerIndex()} and {@link #getContainerKey()} provide
	 * information about the index or key if applicable.
	 */
	@Nullable
	public Object getContainer() {
		return this.container;
	}

	/**
	 * When {@code @Valid} is declared on an indexed container of elements such as
	 * {@link List} or array, this method returns the index of the validated
	 * {@link #getArgument() argument}.
	 */
	@Nullable
	public Integer getContainerIndex() {
		return this.containerIndex;
	}

	/**
	 * When {@code @Valid} is declared on a container of elements referenced by
	 * key such as {@link java.util.Map}, this method returns the key of the
	 * validated {@link #getArgument() argument}.
	 */
	@Nullable
	public Object getContainerKey() {
		return this.containerKey;
	}

	/**
	 * Unwrap the source behind the given error. For Jakarta Bean validation the
	 * source is a {@link jakarta.validation.ConstraintViolation}.
	 * @param sourceType the expected source type
	 * @return the source object of the given type
	 * @since 6.2
	 */
	@SuppressWarnings("unchecked")
	public <T> T unwrap(MessageSourceResolvable error, Class<T> sourceType) {
		return (T) this.sourceLookup.apply(error, sourceType);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		return (other instanceof ParameterValidationResult otherResult &&
				getMethodParameter().equals(otherResult.getMethodParameter()) &&
				ObjectUtils.nullSafeEquals(getArgument(), otherResult.getArgument()) &&
				ObjectUtils.nullSafeEquals(getContainerIndex(), otherResult.getContainerIndex()) &&
				ObjectUtils.nullSafeEquals(getContainerKey(), otherResult.getContainerKey()));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + getMethodParameter().hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getArgument());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getContainerIndex());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getContainerKey());
		return hashCode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + this.methodParameter +
				", argument value '" + ObjectUtils.nullSafeConciseToString(this.argument) + "'," +
				(this.containerIndex != null ? "containerIndex[" + this.containerIndex + "]," : "") +
				(this.containerKey != null ? "containerKey['" + this.containerKey + "']," : "") +
				" errors: " + getResolvableErrors();
	}

}
