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

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
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


	public RequestParamArgumentResolver(ConversionService conversionService) {
		super(conversionService);
	}


	@Override
	@Nullable
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestParam annot = parameter.getParameterAnnotation(RequestParam.class);
		return (annot == null ? null :
				new NamedValueInfo(annot.name(), annot.required(), annot.defaultValue(), "request parameter", true));
	}

	@Override
	protected void addRequestValue(
			String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		requestValues.addRequestParameter(name, (String) value);
	}

}
