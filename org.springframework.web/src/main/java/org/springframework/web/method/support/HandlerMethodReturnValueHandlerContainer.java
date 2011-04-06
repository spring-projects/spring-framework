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
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Implementation of {@link HandlerMethodReturnValueHandler} that handles method return values by 
 * delegating to a list of registered {@link HandlerMethodReturnValueHandler}s.
 * 
 * <p>Previously resolved return types are cached internally for faster lookups. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HandlerMethodReturnValueHandlerContainer implements HandlerMethodReturnValueHandler {
	
	protected final Log logger = LogFactory.getLog(HandlerMethodArgumentResolverContainer.class);

	private List<HandlerMethodReturnValueHandler> returnValueHandlers = 
		new ArrayList<HandlerMethodReturnValueHandler>();

	private Map<MethodParameter, HandlerMethodReturnValueHandler> returnValueHandlerCache =
		new ConcurrentHashMap<MethodParameter, HandlerMethodReturnValueHandler>();

	/**
	 * Indicates whether the given {@linkplain MethodParameter method return type} is supported by any of the
	 * registered {@link HandlerMethodReturnValueHandler}s.
	 */
	public boolean supportsReturnType(MethodParameter returnType) {
		return getReturnValueHandler(returnType) != null;
	}

	/**
	 * Handles the given method return value by iterating over registered {@link HandlerMethodReturnValueHandler}s 
	 * to find one that supports it.
	 */
	public void handleReturnValue(Object returnValue, 
								  MethodParameter returnType, 
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest) throws Exception {
		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		if (handler != null) {
			handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
			return;
		}

		throw new IllegalStateException(
				"No suitable HandlerMethodReturnValueHandler found. " +
				"supportsReturnType(MethodParameter) should have been called previously");
	}

	/**
	 * Find a registered {@link HandlerMethodReturnValueHandler} that supports the given method return type.
	 */
	private HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
		if (this.returnValueHandlers == null) {
			return null;
		}
		HandlerMethodReturnValueHandler result = this.returnValueHandlerCache.get(returnType);
		if (result == null) {
			for (HandlerMethodReturnValueHandler methodReturnValueHandler : returnValueHandlers) {
				if (logger.isTraceEnabled()) {
					logger.trace("Testing if return value handler [" + methodReturnValueHandler + "] supports [" +
							returnType.getGenericParameterType() + "]");
				}
				if (methodReturnValueHandler.supportsReturnType(returnType)) {
					result = methodReturnValueHandler;
					this.returnValueHandlerCache.put(returnType, methodReturnValueHandler);
					break;
				}
			}
		}
		return result;
	}	
	
	/**
	 * Indicates whether the return value handler that supports the given method parameter uses the response.
	 * @see HandlerMethodProcessor#usesResponseArgument(MethodParameter)
	 */
	public boolean usesResponseArgument(MethodParameter parameter) {
		HandlerMethodReturnValueHandler handler = getReturnValueHandler(parameter);
		return (handler != null && handler.usesResponseArgument(parameter));
	}
	
	/**
	 * Register the given {@link HandlerMethodReturnValueHandler}.
	 */
	public void registerReturnValueHandler(HandlerMethodReturnValueHandler returnValuehandler) {
		returnValueHandlers.add(returnValuehandler);
	}

}