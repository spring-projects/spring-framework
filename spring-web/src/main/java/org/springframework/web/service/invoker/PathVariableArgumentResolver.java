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

import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;


/**
 * An implementation of {@link HttpServiceMethodArgumentResolver} that resolves
 * request path variables based on method arguments annotated
 * with {@link  PathVariable}. {@code null} values are allowed only
 * if {@link PathVariable#required()} is {@code true}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 */
public class PathVariableArgumentResolver implements HttpServiceMethodArgumentResolver {

	private static final Log logger = LogFactory.getLog(PathVariableArgumentResolver.class);


	private final ConversionService conversionService;


	public PathVariableArgumentResolver(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService is required");
		this.conversionService = conversionService;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestDefinition requestDefinition) {

		PathVariable annotation = parameter.getParameterAnnotation(PathVariable.class);
		if (annotation == null) {
			return;
		}

		if (Map.class.isAssignableFrom(parameter.getParameterType())) {
			if (argument != null) {
				Assert.isInstanceOf(Map.class, argument);
				((Map<String, ?>) argument).forEach((key, value) ->
						addUriParameter(key, value, annotation.required(), requestDefinition));
			}
		}
		else {
			String name = StringUtils.hasText(annotation.value()) ? annotation.value() : annotation.name();
			name = StringUtils.hasText(name) ? name : parameter.getParameterName();
			Assert.notNull(name, "Failed to determine path variable name for parameter: " + parameter);
			addUriParameter(name, argument, annotation.required(), requestDefinition);
		}
	}

	private void addUriParameter(
			String name, @Nullable Object value, boolean required, HttpRequestDefinition requestDefinition) {

		if (value instanceof Optional) {
			value = ((Optional<?>) value).orElse(null);
		}

		if (value == null) {
			Assert.isTrue(!required, "Missing required path variable '" + name + "'");
			return;
		}

		if (!(value instanceof String)) {
			value = this.conversionService.convert(value, String.class);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Resolved path variable '" + name + "' to " + value);
		}

		requestDefinition.getUriVariables().put(name, (String) value);
	}

}
