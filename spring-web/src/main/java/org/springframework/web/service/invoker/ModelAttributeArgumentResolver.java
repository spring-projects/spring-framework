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

package org.springframework.web.service.invoker;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves {@link ModelAttribute}-annotated method parameters by expanding a bean
 * into request parameters for an HTTP client.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Each readable bean property yields a request parameter named after the property.</li>
 *   <li>{@link BindParam} can override the parameter name. It is supported on both fields and
 *       getter methods; if both are present, the getter annotation wins.</li>
 *   <li>Null property values are skipped.</li>
 *   <li>Values are converted to strings via the configured {@link ConversionService} when
 *       possible; otherwise, {@code toString()} is used as a fallback.</li>
 * </ul>
 *
 * @author Hermann Pencole
 * @since 7.0
 */
public class ModelAttributeArgumentResolver extends AbstractNamedValueArgumentResolver {
	private final ConversionService conversionService;

	/**
	 * Constructor for a resolver to a String value.
	 * @param conversionService the {@link ConversionService} to use to format
	 * Object to String values
	 */
	public ModelAttributeArgumentResolver(ConversionService conversionService) {
		super();
		this.conversionService = conversionService;
	}


	@Override
	protected @Nullable NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		ModelAttribute annot = parameter.getParameterAnnotation(ModelAttribute.class);
		if (annot == null) {
			return null;
		}
		return new NamedValueInfo(
				annot.name(), false, null, "model attribute",
				true);
	}

	@Override
	protected void addRequestValue(String name, Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
		// Create a map to store custom parameter names
		Map<String, String> customParamNames = new HashMap<>();

		// Retrieve all @BindParam annotations
		Class<?> clazz = argument.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			BindParam bindParam = field.getAnnotation(BindParam.class);
			if (bindParam != null) {
				customParamNames.put(field.getName(), bindParam.value());
			}
		}

		// Convert object to query parameters
		BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(argument);
		for (PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
			String propertyName = descriptor.getName();
			if (!"class".equals(propertyName)) {
				Object value = wrapper.getPropertyValue(propertyName);
				if (value != null) {
					// Use a custom name if it exists, otherwise use the property name
					String paramName = customParamNames.getOrDefault(propertyName, propertyName);
					requestValues.addRequestParameter(paramName, convertSingleToString(value));
				}
			}
		}
	}

	/**
	 * Convert an arbitrary value to a string using the configured {@link ConversionService}
	 * when possible, otherwise falls back to {@code toString()}.
	 */
	private String convertSingleToString(Object value) {
		try {
			if (this.conversionService.canConvert(value.getClass(), String.class)) {
				String converted = this.conversionService.convert(value, String.class);
				return converted != null ? converted : "";
			}
		} catch (Exception ignore) {
			// Fallback to toString below
		}
		return String.valueOf(value);
	}



}
