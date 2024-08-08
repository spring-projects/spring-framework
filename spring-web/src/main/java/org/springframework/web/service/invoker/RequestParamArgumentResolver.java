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

package org.springframework.web.service.invoker;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestParam @RequestParam}
 * annotated arguments.
 *
 * <p>When {@code "content-type"} is set to
 * {@code "application/x-www-form-urlencoded"}, request parameters are encoded
 * in the request body. Otherwise, they are added as URL query parameters.
 *
 * <p>The argument may be:
 * <ul>
 * <li>{@code Map<String, ?>} or
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, ?&gt;} with
 * multiple request parameter and value(s).
 * <li>{@code Collection} or an array of request parameters.
 * <li>An individual request parameter.
 * </ul>
 *
 * <p>Individual request parameters may be Strings or Objects to be converted to
 * String values through the configured {@link ConversionService}.
 *
 * <p>If the value is required but {@code null}, {@link IllegalArgumentException}
 * is raised. The value is not required if:
 * <ul>
 * <li>{@link RequestParam#required()} is set to {@code false}
 * <li>{@link RequestParam#defaultValue()} provides a fallback value
 * <li>The argument is declared as {@link java.util.Optional}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class RequestParamArgumentResolver extends AbstractNamedValueArgumentResolver {

	private boolean favorSingleValue;


	public RequestParamArgumentResolver(ConversionService conversionService) {
		super(conversionService);
	}


	/**
	 * Whether to format multiple values (e.g. collection, array) as a single
	 * String value through the configured {@link ConversionService} unless the
	 * content type is form data, or it is a multipart request.
	 * <p>By default, this is {@code false} in which case formatting is not applied,
	 * and a separate parameter with the same name is created for each value.
	 * @since 6.2
	 */
	public void setFavorSingleValue(boolean favorSingleValue) {
		this.favorSingleValue = favorSingleValue;
	}

	/**
	 * Return the setting for {@link #setFavorSingleValue favorSingleValue}.
	 * @since 6.2
	 */
	public boolean isFavorSingleValue() {
		return this.favorSingleValue;
	}


	@Override
	@Nullable
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter, HttpRequestValues.Metadata metadata) {
		RequestParam annot = parameter.getParameterAnnotation(RequestParam.class);
		if (annot == null) {
			return null;
		}
		return new NamedValueInfo(
				annot.name(), annot.required(), annot.defaultValue(), "request parameter",
				supportsMultipleValues(parameter, metadata));
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		// Shouldn't be called since we override createNamedValueInfo with HttpRequestValues.Metadata
		throw new UnsupportedOperationException();
	}

	/**
	 * Determine whether the resolver should send multi-value request parameters
	 * as individual values. If not, they are formatted to a single String value.
	 * The default implementation uses {@link #isFavorSingleValue()} to decide
	 * unless the content type is form data, or it is a multipart request.
	 * @since 6.2
	 */
	protected boolean supportsMultipleValues(MethodParameter parameter, HttpRequestValues.Metadata metadata) {
		return (!isFavorSingleValue() || isFormOrMultipartContent(metadata));
	}

	/**
	 * Whether the content type is form data, or it is a multipart request.
	 * @since 6.2
	 */
	protected boolean isFormOrMultipartContent(HttpRequestValues.Metadata metadata) {
		MediaType mediaType = metadata.getContentType();
		return (mediaType != null && (mediaType.getType().equals("multipart") ||
				mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)));
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		requestValues.addRequestParameter(name, (String) value);
	}

}
