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

package org.springframework.web.bind.support;

import java.util.Locale;

import guru.mocker.annotation.mixin.Mixin;
import org.jspecify.annotations.Nullable;

import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.util.BindErrorUtils;

/**
 * {@link ServerWebInputException} subclass that indicates a data binding or
 * validation failure.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class WebExchangeBindException extends WebExchangeBindExceptionForwarder implements BindingResult {

	final BindingResult bindingResult;

	public WebExchangeBindException(MethodParameter parameter, BindingResult bindingResult) {
		this("Validation failure", parameter, null, null, null, bindingResult);
	}

	@Mixin(grandparent = ServerWebInputException.class)
	private WebExchangeBindException(String reason, @Nullable MethodParameter parameter, @Nullable Throwable cause,
									@Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments, BindingResult bindingResult) {
		super(reason, parameter, cause, messageDetailCode, messageDetailArguments, bindingResult);
		this.bindingResult = bindingResult;
		getBody().setDetail("Invalid request content.");
	}


	/**
	 * Return the BindingResult that this BindException wraps.
	 * <p>Will typically be a BeanPropertyBindingResult.
	 * @see BeanPropertyBindingResult
	 */
	public final BindingResult getBindingResult() {
		return this.bindingResult;
	}


	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] {
				BindErrorUtils.resolveAndJoin(getGlobalErrors()),
				BindErrorUtils.resolveAndJoin(getFieldErrors())};
	}

	@Override
	public Object[] getDetailMessageArguments(MessageSource source, Locale locale) {
		return new Object[] {
				BindErrorUtils.resolveAndJoin(getGlobalErrors(), source, locale),
				BindErrorUtils.resolveAndJoin(getFieldErrors(), source, locale)};
	}

	/**
	 * Returns diagnostic information about the errors held in this object.
	 */
	@Override
	public String getMessage() {
		MethodParameter parameter = getMethodParameter();
		Assert.state(parameter != null, "No MethodParameter");
		StringBuilder sb = new StringBuilder("Validation failed for argument at index ")
				.append(parameter.getParameterIndex()).append(" in method: ")
				.append(parameter.getExecutable().toGenericString())
				.append(", with ").append(getErrorCount()).append(" error(s): ");
		for (ObjectError error : getAllErrors()) {
			sb.append('[').append(error).append("] ");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || this.bindingResult.equals(other));
	}

	@Override
	public int hashCode() {
		return this.bindingResult.hashCode();
	}

}
