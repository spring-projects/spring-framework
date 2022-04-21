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
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
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

	private static final Log LOG = LogFactory.getLog(PathVariableArgumentResolver.class);
	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
	@Nullable
	private final ConversionService conversionService;

	public PathVariableArgumentResolver(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void resolve(@Nullable Object argument, MethodParameter parameter,
			HttpRequestDefinition requestDefinition) {
		PathVariable annotation = parameter.getParameterAnnotation(PathVariable.class);
		if (annotation == null) {
			return;
		}
		String resolvedAnnotationName = StringUtils.hasText(annotation.value())
				? annotation.value() : annotation.name();
		boolean required = annotation.required();
		Object resolvedArgument = resolveFromOptional(argument);
		if (resolvedArgument instanceof Map<?, ?> valueMap) {
			if (StringUtils.hasText(resolvedAnnotationName)) {
				Object value = valueMap.get(resolvedAnnotationName);
				Object resolvedValue = resolveFromOptional(value);
				addUriParameter(requestDefinition, resolvedAnnotationName, resolvedValue, required);
				return;
			}
			valueMap.entrySet()
					.forEach(entry -> addUriParameter(requestDefinition, entry, required));
			return;
		}
		String name = StringUtils.hasText(resolvedAnnotationName)
				? resolvedAnnotationName : parameter.getParameterName();
		addUriParameter(requestDefinition, name, resolvedArgument, required);
	}

	private void addUriParameter(HttpRequestDefinition requestDefinition, @Nullable String name,
			@Nullable Object value, boolean required) {
		if (name == null) {
			throw new IllegalStateException("Path variable name cannot be null");
		}
		String stringValue = getStringValue(value, required);
		if (LOG.isTraceEnabled()) {
			LOG.trace("Path variable " + name + " resolved to " + stringValue);
		}
		requestDefinition.getUriVariables().put(name, stringValue);
	}

	@Nullable
	private String getStringValue(@Nullable Object value, boolean required) {
		validateForNull(value, required);
		validateForReactiveWrapper(value);
		return value != null
				? convertToString(TypeDescriptor.valueOf(value.getClass()), value) : null;
	}

	private void addUriParameter(HttpRequestDefinition requestDefinition,
			Map.Entry<?, ?> entry, boolean required) {
		Object resolvedName = resolveFromOptional(entry.getKey());
		String stringName = getStringValue(resolvedName, true);
		Object resolvedValue = resolveFromOptional(entry.getValue());
		addUriParameter(requestDefinition, stringName, resolvedValue, required);
	}

	private void validateForNull(@Nullable Object argument, boolean required) {
		if (argument == null) {
			if (required) {
				throw new IllegalStateException("Required variable cannot be null");
			}
		}
	}

	private void validateForReactiveWrapper(@Nullable Object object) {
		if (object != null) {
			Class<?> type = object.getClass();
			ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
			ReactiveAdapter adapter = adapterRegistry.getAdapter(type);
			if (adapter != null) {
				throw new IllegalStateException(getClass().getSimpleName() +
						" does not support reactive type wrapper: " + type);
			}
		}

	}

	@Nullable
	private Object resolveFromOptional(@Nullable Object argument) {
		if (argument instanceof Optional) {
			return ((Optional<?>) argument).orElse(null);
		}
		return argument;
	}

	@Nullable
	private String convertToString(TypeDescriptor typeDescriptor, @Nullable Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String) {
			return (String) value;
		}
		if (this.conversionService != null) {
			return (String) this.conversionService.convert(value, typeDescriptor, STRING_TYPE_DESCRIPTOR);
		}
		return String.valueOf(value);
	}

}
