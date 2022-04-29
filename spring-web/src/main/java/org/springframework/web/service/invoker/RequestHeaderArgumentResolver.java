/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ValueConstants;


/**
 * {@link HttpServiceArgumentResolver} for {@link RequestHeader @RequestHeader}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>{@code Map} or {@link org.springframework.util.MultiValueMap} with
 * multiple headers and value(s).
 * <li>{@code Collection} or an array of header values.
 * <li>An individual header value.
 * </ul>
 *
 * <p>Individual header values may be Strings or Objects to be converted to
 * String values through the configured {@link ConversionService}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 */
public class RequestHeaderArgumentResolver implements HttpServiceArgumentResolver {

	private static final Log logger = LogFactory.getLog(RequestHeaderArgumentResolver.class);

	private final ConversionService conversionService;


	public RequestHeaderArgumentResolver(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService is required");
		this.conversionService = conversionService;
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		RequestHeader annot = parameter.getParameterAnnotation(RequestHeader.class);
		if (annot == null) {
			return false;
		}

		Class<?> parameterType = parameter.getParameterType();
		boolean required = (annot.required() && !Optional.class.isAssignableFrom(parameterType));
		Object defaultValue = (ValueConstants.DEFAULT_NONE.equals(annot.defaultValue()) ? null : annot.defaultValue());

		if (Map.class.isAssignableFrom(parameterType)) {
			if (argument != null) {
				Assert.isInstanceOf(Map.class, argument);
				((Map<String, ?>) argument).forEach((key, value) ->
						addHeader(key, value, false, defaultValue, requestValues));
			}
		}
		else {
			String name = StringUtils.hasText(annot.value()) ? annot.value() : annot.name();
			name = StringUtils.hasText(name) ? name : parameter.getParameterName();
			Assert.notNull(name, "Failed to determine request header name for parameter: " + parameter);
			addHeader(name, argument, required, defaultValue, requestValues);
		}

		return true;
	}

	private void addHeader(
			String name, @Nullable Object value, boolean required, @Nullable Object defaultValue,
			HttpRequestValues.Builder requestValues) {

		value = (ObjectUtils.isArray(value) ? Arrays.asList((Object[]) value) : value);
		if (value instanceof Collection<?> elements) {
			boolean hasValue = false;
			for (Object element : elements) {
				if (element != null) {
					hasValue = true;
					addHeaderValue(name, element, false, requestValues);
				}
			}
			if (hasValue) {
				return;
			}
			value = null;
		}

		if (value instanceof Optional<?> optionalValue) {
			value = optionalValue.orElse(null);
		}

		if (value == null && defaultValue != null) {
			value = defaultValue;
		}

		addHeaderValue(name, value, required, requestValues);
	}

	private void addHeaderValue(
			String name, @Nullable Object value, boolean required, HttpRequestValues.Builder requestValues) {

		if (!(value instanceof String)) {
			value = this.conversionService.convert(value, String.class);
		}

		if (value == null) {
			Assert.isTrue(!required, "Missing required header '" + name + "'");
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Resolved header '" + name + ":" + value + "'");
			}

		requestValues.addHeader(name, (String) value);
	}

}
