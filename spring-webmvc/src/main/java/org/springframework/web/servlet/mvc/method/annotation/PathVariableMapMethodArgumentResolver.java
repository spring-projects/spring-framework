/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves {@link Map} method arguments annotated with an @{@link PathVariable}
 * where the annotation does not specify a path variable name. The created
 * {@link Map} contains all URI template name/value pairs.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @see PathVariableMethodArgumentResolver
 */
public class PathVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		PathVariable annot = parameter.getParameterAnnotation(PathVariable.class);
		return ((annot != null) && (Map.class.isAssignableFrom(parameter.getParameterType()))
				&& (!StringUtils.hasText(annot.value())));
	}

	/**
	 * Return a Map with all URI template variables.
	 * @throws ServletRequestBindingException if no URI vars are found in the
	 * 	request attribute {@link HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE}
	 */
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, String> uriTemplateVars =
				(Map<String, String>) webRequest.getAttribute(
						HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (CollectionUtils.isEmpty(uriTemplateVars)) {
			throw new ServletRequestBindingException(
					"No URI template variables for method parameter type [" + parameter.getParameterType() + "]");
		}

		return new LinkedHashMap<String, String>(uriTemplateVars);
	}

}