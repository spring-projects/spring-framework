/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.bind;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.util.HtmlUtils;

/**
 * Errors wrapper that adds automatic HTML escaping to the wrapped instance,
 * for convenient usage in HTML views. Can be retrieved easily via
 * RequestContext's {@code getErrors} method.
 *
 * <p>Note that BindTag does <i>not</i> use this class to avoid unnecessary
 * creation of ObjectError instances. It just escapes the messages and values
 * that get copied into the respective BindStatus instance.
 *
 * @author Juergen Hoeller
 * @since 01.03.2003
 * @see org.springframework.web.servlet.support.RequestContext#getErrors
 * @see org.springframework.web.servlet.tags.BindTag
 */
public class EscapedErrors implements Errors {

	private final Errors source;


	/**
	 * Create a new EscapedErrors instance for the given source instance.
	 */
	public EscapedErrors(Errors source) {
		if (source == null) {
			throw new IllegalArgumentException("Cannot wrap a null instance");
		}
		this.source = source;
	}

	public Errors getSource() {
		return this.source;
	}


	public String getObjectName() {
		return this.source.getObjectName();
	}

	public void setNestedPath(String nestedPath) {
		this.source.setNestedPath(nestedPath);
	}

	public String getNestedPath() {
		return this.source.getNestedPath();
	}

	public void pushNestedPath(String subPath) {
		this.source.pushNestedPath(subPath);
	}

	public void popNestedPath() throws IllegalStateException {
		this.source.popNestedPath();
	}


	public void reject(String errorCode) {
		this.source.reject(errorCode);
	}

	public void reject(String errorCode, String defaultMessage) {
		this.source.reject(errorCode, defaultMessage);
	}

	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		this.source.reject(errorCode, errorArgs, defaultMessage);
	}

	public void rejectValue(String field, String errorCode) {
		this.source.rejectValue(field, errorCode);
	}

	public void rejectValue(String field, String errorCode, String defaultMessage) {
		this.source.rejectValue(field, errorCode, defaultMessage);
	}

	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		this.source.rejectValue(field, errorCode, errorArgs, defaultMessage);
	}

	public void addAllErrors(Errors errors) {
		this.source.addAllErrors(errors);
	}


	public boolean hasErrors() {
		return this.source.hasErrors();
	}

	public int getErrorCount() {
		return this.source.getErrorCount();
	}

	public List<ObjectError> getAllErrors() {
		return escapeObjectErrors(this.source.getAllErrors());
	}

	public boolean hasGlobalErrors() {
		return this.source.hasGlobalErrors();
	}

	public int getGlobalErrorCount() {
		return this.source.getGlobalErrorCount();
	}

	public List<ObjectError> getGlobalErrors() {
		return escapeObjectErrors(this.source.getGlobalErrors());
	}

	public ObjectError getGlobalError() {
		return escapeObjectError(this.source.getGlobalError());
	}

	public boolean hasFieldErrors() {
		return this.source.hasFieldErrors();
	}

	public int getFieldErrorCount() {
		return this.source.getFieldErrorCount();
	}

	public List<FieldError> getFieldErrors() {
		return this.source.getFieldErrors();
	}

	public FieldError getFieldError() {
		return this.source.getFieldError();
	}

	public boolean hasFieldErrors(String field) {
		return this.source.hasFieldErrors(field);
	}

	public int getFieldErrorCount(String field) {
		return this.source.getFieldErrorCount(field);
	}

	public List<FieldError> getFieldErrors(String field) {
		return escapeObjectErrors(this.source.getFieldErrors(field));
	}

	public FieldError getFieldError(String field) {
		return escapeObjectError(this.source.getFieldError(field));
	}

	public Object getFieldValue(String field) {
		Object value = this.source.getFieldValue(field);
		return (value instanceof String ? HtmlUtils.htmlEscape((String) value) : value);
	}

	public Class getFieldType(String field) {
		return this.source.getFieldType(field);
	}

	@SuppressWarnings("unchecked")
	private <T extends ObjectError> T escapeObjectError(T source) {
		if (source == null) {
			return null;
		}
		if (source instanceof FieldError) {
			FieldError fieldError = (FieldError) source;
			Object value = fieldError.getRejectedValue();
			if (value instanceof String) {
				value = HtmlUtils.htmlEscape((String) value);
			}
			return (T) new FieldError(
					fieldError.getObjectName(), fieldError.getField(), value,
					fieldError.isBindingFailure(), fieldError.getCodes(),
					fieldError.getArguments(), HtmlUtils.htmlEscape(fieldError.getDefaultMessage()));
		}
		else {
			return (T) new ObjectError(
					source.getObjectName(), source.getCodes(), source.getArguments(),
					HtmlUtils.htmlEscape(source.getDefaultMessage()));
		}
	}

	private <T extends ObjectError> List<T> escapeObjectErrors(List<T> source) {
		List<T> escaped = new ArrayList<T>(source.size());
		for (T objectError : source) {
			escaped.add(escapeObjectError(objectError));
		}
		return escaped;
	}

}
