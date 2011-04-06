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

package org.springframework.web.method.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} that resolves handler method arguments by delegating 
 * to a list of registered {@link HandlerMethodArgumentResolver}s.
 * 
 * <p>Previously resolved method argument types are cached internally for faster lookups. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HandlerMethodArgumentResolverContainer implements HandlerMethodArgumentResolver {

	protected final Log logger = LogFactory.getLog(HandlerMethodArgumentResolverContainer.class);

	private List<HandlerMethodArgumentResolver> argumentResolvers = 
		new ArrayList<HandlerMethodArgumentResolver>();

	private Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
		new ConcurrentHashMap<MethodParameter, HandlerMethodArgumentResolver>();

	/**
	 * Indicates whether the given {@linkplain MethodParameter method parameter} is supported by any of the 
	 * registered {@link HandlerMethodArgumentResolver}s.
	 */
	public boolean supportsParameter(MethodParameter parameter) {
		return getArgumentResolver(parameter) != null;
	}

	/**
	 * Resolve a method parameter into an argument value for the given request by iterating over registered 
	 * {@link HandlerMethodArgumentResolver}s to find one that supports the given method parameter.
	 */
	public Object resolveArgument(MethodParameter parameter, 
								  ModelMap model,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) throws Exception {
		HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
		if (resolver != null) {
			return resolver.resolveArgument(parameter, model, webRequest, binderFactory);
		}
		
		throw new IllegalStateException(
				"No suitable HandlerMethodArgumentResolver found. " + 
				"supportsParameter(MethodParameter) should have been called previously.");
	}

	/**
	 * Find a registered {@link HandlerMethodArgumentResolver} that supports the given method parameter.
	 * @return a {@link HandlerMethodArgumentResolver} instance, or {@code null} if none 
	 */
	private HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
		HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
		if (result == null) {
			for (HandlerMethodArgumentResolver methodArgumentResolver : argumentResolvers) {
				if (logger.isTraceEnabled()) {
					logger.trace("Testing if argument resolver [" + methodArgumentResolver + "] supports [" +
							parameter.getGenericParameterType() + "]");
				}
				if (methodArgumentResolver.supportsParameter(parameter)) {
					result = methodArgumentResolver;
					this.argumentResolverCache.put(parameter, result);
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Indicates whether the argument resolver that supports the given method parameter uses the response argument.
	 * @see HandlerMethodProcessor#usesResponseArgument(MethodParameter)
	 */
	public boolean usesResponseArgument(MethodParameter parameter) {
		HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
		return (resolver != null && resolver.usesResponseArgument(parameter));
	}
	
	/**
	 * Register the given {@link HandlerMethodArgumentResolver}.
	 */
	public void registerArgumentResolver(HandlerMethodArgumentResolver argumentResolver) {
		this.argumentResolvers.add(argumentResolver);
	}

}