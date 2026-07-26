/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.bind;

import java.util.ArrayList;
import java.util.List;

import guru.mocker.annotation.mixin.Mixin;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
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
public class EscapedErrors extends EscapedErrorsForwarder implements Errors {

	/**
	 * Create a new EscapedErrors instance for the given source instance.
	 */
	@Mixin
	public EscapedErrors(Errors source) {
		super(source);
		Assert.notNull(source, "Errors source must not be null");
	}

	public Errors getSource() {
		return source;
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return escapeObjectErrors(source.getAllErrors());
	}


	@Override
	public List<ObjectError> getGlobalErrors() {
		return escapeObjectErrors(source.getGlobalErrors());
	}

	@Override
	public @Nullable ObjectError getGlobalError() {
		return escapeObjectError(source.getGlobalError());
	}


	@Override
	public List<FieldError> getFieldErrors(String field) {
		return escapeObjectErrors(source.getFieldErrors(field));
	}

	@Override
	public @Nullable FieldError getFieldError(String field) {
		return escapeObjectError(source.getFieldError(field));
	}

	@Override
	public @Nullable Object getFieldValue(String field) {
		Object value = source.getFieldValue(field);
		return (value instanceof String text ? HtmlUtils.htmlEscape(text) : value);
	}


	@SuppressWarnings("unchecked")
	private <T extends ObjectError> @Nullable T escapeObjectError(@Nullable T source) {
		if (source == null) {
			return null;
		}
		String defaultMessage = source.getDefaultMessage();
		if (defaultMessage != null) {
			defaultMessage = HtmlUtils.htmlEscape(defaultMessage);
		}
		if (source instanceof FieldError fieldError) {
			Object value = fieldError.getRejectedValue();
			if (value instanceof String text) {
				value = HtmlUtils.htmlEscape(text);
			}
			return (T) new FieldError(
					fieldError.getObjectName(), fieldError.getField(), value, fieldError.isBindingFailure(),
					fieldError.getCodes(), fieldError.getArguments(), defaultMessage);
		}
		else {
			return (T) new ObjectError(
					source.getObjectName(), source.getCodes(), source.getArguments(), defaultMessage);
		}
	}

	private <T extends ObjectError> List<T> escapeObjectErrors(List<T> source) {
		List<T> escaped = new ArrayList<>(source.size());
		for (T objectError : source) {
			escaped.add(escapeObjectError(objectError));
		}
		return escaped;
	}

}
