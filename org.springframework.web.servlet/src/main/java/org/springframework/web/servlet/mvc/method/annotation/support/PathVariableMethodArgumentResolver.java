/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation.support;

import java.util.Map;

import javax.servlet.ServletException;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.support.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves method arguments annotated with an @{@link PathVariable}.
 *
 * <p>An @{@link PathVariable} is a named value that gets resolved from a URI template variable. It is always 
 * required and does not have a default value to fall back on. See the base class 
 * {@link AbstractNamedValueMethodArgumentResolver} for more information on how named values are processed.
 * 
 * <p>A {@link WebDataBinder} is invoked to apply type conversion to resolved path variable values that 
 * don't yet match the method parameter type.
 * 
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public PathVariableMethodArgumentResolver() {
		super(null);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(PathVariable.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable annotation = parameter.getParameterAnnotation(PathVariable.class);
		return new PathVariableNamedValueInfo(annotation);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		String key = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		int scope = RequestAttributes.SCOPE_REQUEST;
		Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(key, scope);
		return (uriTemplateVars != null) ? uriTemplateVars.get(name) : null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new IllegalStateException("Could not find the URL template variable [" + name + "]");
	}

	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		private PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.value(), true, ValueConstants.DEFAULT_NONE);
		}
	}
}