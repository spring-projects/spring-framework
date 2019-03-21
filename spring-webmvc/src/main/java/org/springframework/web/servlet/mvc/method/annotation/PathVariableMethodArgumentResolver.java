/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Resolves method arguments annotated with an @{@link PathVariable}.
 *
 * <p>An @{@link PathVariable} is a named value that gets resolved from a URI template variable.
 * It is always required and does not have a default value to fall back on. See the base class
 * {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}
 * for more information on how named values are processed.
 *
 * <p>If the method parameter type is {@link Map}, the name specified in the annotation is used
 * to resolve the URI variable String value. The value is then converted to a {@link Map} via
 * type conversion, assuming a suitable {@link Converter} or {@link PropertyEditor} has been
 * registered.
 *
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved path variable
 * values that don't yet match the method parameter type.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.1
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
		implements UriComponentsContributor {

	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (!parameter.hasParameterAnnotation(PathVariable.class)) {
			return false;
		}
		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			String paramName = parameter.getParameterAnnotation(PathVariable.class).value();
			return StringUtils.hasText(paramName);
		}
		return true;
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable annotation = parameter.getParameterAnnotation(PathVariable.class);
		return new PathVariableNamedValueInfo(annotation);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingPathVariableException(name, parameter);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleResolvedValue(Object arg, String name, MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest request) {

		String key = View.PATH_VARIABLES;
		int scope = RequestAttributes.SCOPE_REQUEST;
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(key, scope);
		if (pathVars == null) {
			pathVars = new HashMap<String, Object>();
			request.setAttribute(key, pathVars, scope);
		}
		pathVars.put(name, arg);
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			return;
		}

		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		String name = (ann != null && !StringUtils.isEmpty(ann.value()) ? ann.value() : parameter.getParameterName());
		value = formatUriValue(conversionService, new TypeDescriptor(parameter.nestedIfOptional()), value);
		uriVariables.put(name, value);
	}

	protected String formatUriValue(ConversionService cs, TypeDescriptor sourceType, Object value) {
		if (value == null) {
			return null;
		}
		else if (value instanceof String) {
			return (String) value;
		}
		else if (cs != null) {
			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
		}
		else {
			return value.toString();
		}
	}


	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		public PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
		}
	}

}
