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

package org.springframework.validation;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A simple implementation of the {@link Errors} interface, managing global
 * errors and field errors for a top-level target object. Flexibly retrieves
 * field values through bean property getter methods, and automatically
 * falls back to raw field access if necessary.
 *
 * <p>Note that this {@link Errors} implementation comes without support for
 * nested paths. It is exclusively designed for the validation of individual
 * top-level objects, not aggregating errors from multiple sources.
 * If this is insufficient for your purposes, use a binding-capable
 * {@link Errors} implementation such as {@link BeanPropertyBindingResult}.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see Validator#validateObject(Object)
 * @see BeanPropertyBindingResult
 * @see DirectFieldBindingResult
 */
@SuppressWarnings("serial")
public class SimpleErrors implements Errors, Serializable {

	private final Object target;

	private final String objectName;

	private final List<ObjectError> globalErrors = new ArrayList<>();

	private final List<FieldError> fieldErrors = new ArrayList<>();


	/**
	 * Create a new {@link SimpleErrors} holder for the given target,
	 * using the simple name of the target class as the object name.
	 * @param target the target to wrap
	 */
	public SimpleErrors(Object target) {
		Assert.notNull(target, "Target must not be null");
		this.target = target;
		this.objectName = this.target.getClass().getSimpleName();
	}

	/**
	 * Create a new {@link SimpleErrors} holder for the given target.
	 * @param target the target to wrap
	 * @param objectName the name of the target object for error reporting
	 */
	public SimpleErrors(Object target, String objectName) {
		Assert.notNull(target, "Target must not be null");
		this.target = target;
		this.objectName = objectName;
	}


	@Override
	public String getObjectName() {
		return this.objectName;
	}

	@Override
	public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
		this.globalErrors.add(new ObjectError(getObjectName(), new String[] {errorCode}, errorArgs, defaultMessage));
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode,
			@Nullable Object[] errorArgs, @Nullable String defaultMessage) {

		if (!StringUtils.hasLength(field)) {
			reject(errorCode, errorArgs, defaultMessage);
			return;
		}

		Object newVal = getFieldValue(field);
		this.fieldErrors.add(new FieldError(getObjectName(), field, newVal, false,
				new String[] {errorCode}, errorArgs, defaultMessage));
	}

	@Override
	public void addAllErrors(Errors errors) {
		this.globalErrors.addAll(errors.getGlobalErrors());
		this.fieldErrors.addAll(errors.getFieldErrors());
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		return this.globalErrors;
	}

	@Override
	public List<FieldError> getFieldErrors() {
		return this.fieldErrors;
	}

	@Override
	@Nullable
	public Object getFieldValue(String field) {
		FieldError fieldError = getFieldError(field);
		if (fieldError != null) {
			return fieldError.getRejectedValue();
		}

		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.target.getClass(), field);
		if (pd != null && pd.getReadMethod() != null) {
			ReflectionUtils.makeAccessible(pd.getReadMethod());
			return ReflectionUtils.invokeMethod(pd.getReadMethod(), this.target);
		}

		Field rawField = ReflectionUtils.findField(this.target.getClass(), field);
		if (rawField != null) {
			ReflectionUtils.makeAccessible(rawField);
			return ReflectionUtils.getField(rawField, this.target);
		}

		throw new IllegalArgumentException("Cannot retrieve value for field '" + field +
				"' - neither a getter method nor a raw field found");
	}

	@Override
	public Class<?> getFieldType(String field) {
		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.target.getClass(), field);
		if (pd != null) {
			return pd.getPropertyType();
		}
		Field rawField = ReflectionUtils.findField(this.target.getClass(), field);
		if (rawField != null) {
			return rawField.getType();
		}
		return null;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SimpleErrors that &&
				ObjectUtils.nullSafeEquals(this.target, that.target) &&
				this.globalErrors.equals(that.globalErrors) &&
				this.fieldErrors.equals(that.fieldErrors)));
	}

	@Override
	public int hashCode() {
		return this.target.hashCode();
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (ObjectError error : this.globalErrors) {
			sb.append('\n').append(error);
		}
		for (ObjectError error : this.fieldErrors) {
			sb.append('\n').append(error);
		}
		return sb.toString();
	}

}
