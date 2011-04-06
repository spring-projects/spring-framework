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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.support.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that supports arguments annotated with
 * {@link PathVariable @PathVariable}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	public PathVariableMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
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
	protected Object resolveNamedValueArgument(NativeWebRequest webRequest, MethodParameter parameter, String name)
			throws Exception {
		Map<String, String> uriTemplateVariables = (Map<String, String>) webRequest.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return (uriTemplateVariables != null) ? uriTemplateVariables.get(name) : null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new IllegalStateException("Could not find @PathVariable [" + name + "] in @RequestMapping");
	}

	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		private PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.value(), true, ValueConstants.DEFAULT_NONE);
		}
	}


}
