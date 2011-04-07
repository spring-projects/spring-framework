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

package org.springframework.web.method.annotation.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Adapts a {@link WebArgumentResolver} into the {@link HandlerMethodArgumentResolver} contract.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class WebArgumentResolverAdapter implements HandlerMethodArgumentResolver {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final WebArgumentResolver adaptee;

	public WebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		Assert.notNull(adaptee, "'adaptee' must not be null");
		this.adaptee = adaptee;
	}

	public boolean supportsParameter(MethodParameter parameter) {
		try {
			NativeWebRequest webRequest = getWebRequest();
			Object result = adaptee.resolveArgument(parameter, webRequest);
			if (result == WebArgumentResolver.UNRESOLVED) {
				return false;
			}
			else {
				return ClassUtils.isAssignableValue(parameter.getParameterType(), result);
			}
		}
		catch (Exception ex) {
			// ignore
			logger.trace("Error in checking support for parameter [" + parameter + "], message: " + ex.getMessage());
			return false;
		}
	}
	
	protected NativeWebRequest getWebRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		return (requestAttributes instanceof NativeWebRequest) ? (NativeWebRequest) requestAttributes : null; 
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public Object resolveArgument(MethodParameter parameter,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) throws Exception {
		Class<?> paramType = parameter.getParameterType();
		Object result = adaptee.resolveArgument(parameter, webRequest);
		if (result == WebArgumentResolver.UNRESOLVED || !ClassUtils.isAssignableValue(paramType, result)) {
			throw new IllegalStateException(
					"Standard argument type [" + paramType.getName() + "] resolved to incompatible value of type [" +
							(result != null ? result.getClass() : null) +
							"]. Consider declaring the argument type in a less specific fashion.");
		}
		return result;
	}
}
