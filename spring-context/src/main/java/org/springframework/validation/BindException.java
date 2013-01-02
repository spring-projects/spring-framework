/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation;

import java.beans.PropertyEditor;
import java.util.List;
import java.util.Map;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.util.Assert;

/**
 * Thrown when binding errors are considered fatal. Implements the
 * {@link BindingResult} interface (and its super-interface {@link Errors})
 * to allow for the direct analysis of binding errors.
 *
 * <p>As of Spring 2.0, this is a special-purpose class. Normally,
 * application code will work with the {@link BindingResult} interface,
 * or with a {@link DataBinder} that in turn exposes a BindingResult via
 * {@link org.springframework.validation.DataBinder#getBindingResult()}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see BindingResult
 * @see DataBinder#getBindingResult()
 * @see DataBinder#close()
 */
@SuppressWarnings("serial")
public class BindException extends Exception implements BindingResult {

	private final BindingResult bindingResult;


	/**
	 * Create a new BindException instance for a BindingResult.
	 * @param bindingResult the BindingResult instance to wrap
	 */
	public BindException(BindingResult bindingResult) {
		Assert.notNull(bindingResult, "BindingResult must not be null");
		this.bindingResult = bindingResult;
	}

	/**
	 * Create a new BindException instance for a target bean.
	 * @param target target bean to bind onto
	 * @param objectName the name of the target object
	 * @see BeanPropertyBindingResult
	 */
	public BindException(Object target, String objectName) {
		Assert.notNull(target, "Target object must not be null");
		this.bindingResult = new BeanPropertyBindingResult(target, objectName);
	}


	/**
	 * Return the BindingResult that this BindException wraps.
	 * Will typically be a BeanPropertyBindingResult.
	 * @see BeanPropertyBindingResult
	 */
	public final BindingResult getBindingResult() {
		return this.bindingResult;
	}


	public String getObjectName() {
		return this.bindingResult.getObjectName();
	}

	public void setNestedPath(String nestedPath) {
		this.bindingResult.setNestedPath(nestedPath);
	}

	public String getNestedPath() {
		return this.bindingResult.getNestedPath();
	}

	public void pushNestedPath(String subPath) {
		this.bindingResult.pushNestedPath(subPath);
	}

	public void popNestedPath() throws IllegalStateException {
		this.bindingResult.popNestedPath();
	}


	public void reject(String errorCode) {
		this.bindingResult.reject(errorCode);
	}

	public void reject(String errorCode, String defaultMessage) {
		this.bindingResult.reject(errorCode, defaultMessage);
	}

	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		this.bindingResult.reject(errorCode, errorArgs, defaultMessage);
	}

	public void rejectValue(String field, String errorCode) {
		this.bindingResult.rejectValue(field, errorCode);
	}

	public void rejectValue(String field, String errorCode, String defaultMessage) {
		this.bindingResult.rejectValue(field, errorCode, defaultMessage);
	}

	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		this.bindingResult.rejectValue(field, errorCode, errorArgs, defaultMessage);
	}

	public void addAllErrors(Errors errors) {
		this.bindingResult.addAllErrors(errors);
	}


	public boolean hasErrors() {
		return this.bindingResult.hasErrors();
	}

	public int getErrorCount() {
		return this.bindingResult.getErrorCount();
	}

	public List<ObjectError> getAllErrors() {
		return this.bindingResult.getAllErrors();
	}

	public boolean hasGlobalErrors() {
		return this.bindingResult.hasGlobalErrors();
	}

	public int getGlobalErrorCount() {
		return this.bindingResult.getGlobalErrorCount();
	}

	public List<ObjectError> getGlobalErrors() {
		return this.bindingResult.getGlobalErrors();
	}

	public ObjectError getGlobalError() {
		return this.bindingResult.getGlobalError();
	}

	public boolean hasFieldErrors() {
		return this.bindingResult.hasFieldErrors();
	}

	public int getFieldErrorCount() {
		return this.bindingResult.getFieldErrorCount();
	}

	public List<FieldError> getFieldErrors() {
		return this.bindingResult.getFieldErrors();
	}

	public FieldError getFieldError() {
		return this.bindingResult.getFieldError();
	}

	public boolean hasFieldErrors(String field) {
		return this.bindingResult.hasFieldErrors(field);
	}

	public int getFieldErrorCount(String field) {
		return this.bindingResult.getFieldErrorCount(field);
	}

	public List<FieldError> getFieldErrors(String field) {
		return this.bindingResult.getFieldErrors(field);
	}

	public FieldError getFieldError(String field) {
		return this.bindingResult.getFieldError(field);
	}

	public Object getFieldValue(String field) {
		return this.bindingResult.getFieldValue(field);
	}

	public Class<?> getFieldType(String field) {
		return this.bindingResult.getFieldType(field);
	}

	public Object getTarget() {
		return this.bindingResult.getTarget();
	}

	public Map<String, Object> getModel() {
		return this.bindingResult.getModel();
	}

	public Object getRawFieldValue(String field) {
		return this.bindingResult.getRawFieldValue(field);
	}

	@SuppressWarnings("rawtypes")
	public PropertyEditor findEditor(String field, Class valueType) {
		return this.bindingResult.findEditor(field, valueType);
	}

	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return this.bindingResult.getPropertyEditorRegistry();
	}

	public void addError(ObjectError error) {
		this.bindingResult.addError(error);
	}

	public String[] resolveMessageCodes(String errorCode) {
		return this.bindingResult.resolveMessageCodes(errorCode);
	}

	public String[] resolveMessageCodes(String errorCode, String field) {
		return this.bindingResult.resolveMessageCodes(errorCode, field);
	}

	public void recordSuppressedField(String field) {
		this.bindingResult.recordSuppressedField(field);
	}

	public String[] getSuppressedFields() {
		return this.bindingResult.getSuppressedFields();
	}


	/**
	 * Returns diagnostic information about the errors held in this object.
	 */
	@Override
	public String getMessage() {
		return this.bindingResult.toString();
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || this.bindingResult.equals(other));
	}

	@Override
	public int hashCode() {
		return this.bindingResult.hashCode();
	}

}
