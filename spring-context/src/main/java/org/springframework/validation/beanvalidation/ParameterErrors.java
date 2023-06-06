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

import java.util.Collection;
import java.util.List;

import jakarta.validation.ConstraintViolation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * Extension of {@link ParameterValidationResult} that's created for Object
 * method arguments or return values with cascaded violations on their properties.
 * Such method parameters are annotated with {@link jakarta.validation.Valid @Valid},
 * or in the case of return values, the annotation is on the method.
 *
 * <p>In addition to the (generic) {@link #getResolvableErrors()
 * MessageSourceResolvable errors} from the base class, this subclass implements
 * {@link Errors} to expose convenient access to the same as {@link FieldError}s.
 *
 * <p>When {@code @Valid} is declared on a {@link List} or {@link java.util.Map}
 * parameter, a separate {@link ParameterErrors} is created for each list or map
 * value for which there are constraint violations. In such cases, the
 * {@link #getContainer()} is the list or map, while {@link #getContainerIndex()}
 * and {@link #getContainerKey()} reflect the index or key of the value.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class ParameterErrors extends ParameterValidationResult implements Errors {

	private final Errors errors;

	@Nullable
	private final Object container;

	@Nullable
	private final Integer containerIndex;

	@Nullable
	private final Object containerKey;


	/**
	 * Create a {@code ParameterErrors}.
	 */
	public ParameterErrors(
			MethodParameter parameter, @Nullable Object argument, Errors errors,
			Collection<ConstraintViolation<Object>> violations,
			@Nullable Object container, @Nullable Integer index, @Nullable Object key) {

		super(parameter, argument, errors.getAllErrors(), violations);

		this.errors = errors;
		this.container = container;
		this.containerIndex = index;
		this.containerKey = key;
	}


	/**
	 * When {@code @Valid} is declared on a {@link List} or {@link java.util.Map}
	 * method parameter, this method returns the list or map that contained the
	 * validated object {@link #getArgument() argument}, while
	 * {@link #getContainerIndex()} and {@link #getContainerKey()} returns the
	 * respective index or key.
	 */
	@Nullable
	public Object getContainer() {
		return this.container;
	}

	/**
	 * When {@code @Valid} is declared on a {@link List}, this method returns
	 * the index under which the validated object {@link #getArgument() argument}
	 * is stored in the list {@link #getContainer() container}.
	 */
	@Nullable
	public Integer getContainerIndex() {
		return this.containerIndex;
	}

	/**
	 * When {@code @Valid} is declared on a {@link java.util.Map}, this method
	 * returns the key under which the validated object {@link #getArgument()
	 * argument} is stored in the map {@link #getContainer()}.
	 */
	@Nullable
	public Object getContainerKey() {
		return this.containerKey;
	}


	// Errors implementation

	@Override
	public String getObjectName() {
		return this.errors.getObjectName();
	}

	@Override
	public void setNestedPath(String nestedPath) {
		this.errors.setNestedPath(nestedPath);
	}

	@Override
	public String getNestedPath() {
		return this.errors.getNestedPath();
	}

	@Override
	public void pushNestedPath(String subPath) {
		this.errors.pushNestedPath(subPath);
	}

	@Override
	public void popNestedPath() throws IllegalStateException {
		this.errors.popNestedPath();
	}

	@Override
	public void reject(String errorCode) {
		this.errors.reject(errorCode);
	}

	@Override
	public void reject(String errorCode, String defaultMessage) {
		this.errors.reject(errorCode, defaultMessage);
	}

	@Override
	public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
		this.errors.reject(errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode) {
		this.errors.rejectValue(field, errorCode);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, String defaultMessage) {
		this.errors.rejectValue(field, errorCode, defaultMessage);
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode,
			@Nullable Object[] errorArgs, @Nullable String defaultMessage) {

		this.errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void addAllErrors(Errors errors) {
		this.errors.addAllErrors(errors);
	}

	@Override
	public boolean hasErrors() {
		return this.errors.hasErrors();
	}

	@Override
	public int getErrorCount() {
		return this.errors.getErrorCount();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return this.errors.getAllErrors();
	}

	@Override
	public boolean hasGlobalErrors() {
		return this.errors.hasGlobalErrors();
	}

	@Override
	public int getGlobalErrorCount() {
		return this.errors.getGlobalErrorCount();
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		return this.errors.getGlobalErrors();
	}

	@Override
	public ObjectError getGlobalError() {
		return this.errors.getGlobalError();
	}

	@Override
	public boolean hasFieldErrors() {
		return this.errors.hasFieldErrors();
	}

	@Override
	public int getFieldErrorCount() {
		return this.errors.getFieldErrorCount();
	}

	@Override
	public List<FieldError> getFieldErrors() {
		return this.errors.getFieldErrors();
	}

	@Override
	public FieldError getFieldError() {
		return this.errors.getFieldError();
	}

	@Override
	public boolean hasFieldErrors(String field) {
		return this.errors.hasFieldErrors(field);
	}

	@Override
	public int getFieldErrorCount(String field) {
		return this.errors.getFieldErrorCount(field);
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		return this.errors.getFieldErrors(field);
	}

	@Override
	public FieldError getFieldError(String field) {
		return this.errors.getFieldError(field);
	}

	@Override
	public Object getFieldValue(String field) {
		return this.errors.getFieldError(field);
	}

	@Override
	public Class<?> getFieldType(String field) {
		return this.errors.getFieldType(field);
	}

}
