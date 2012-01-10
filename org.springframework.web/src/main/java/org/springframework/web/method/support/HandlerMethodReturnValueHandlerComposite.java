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

package org.springframework.web.method.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Handles method return values by delegating to a list of registered {@link HandlerMethodReturnValueHandler}s.
 * Previously resolved return types are cached for faster lookups.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HandlerMethodReturnValueHandlerComposite implements HandlerMethodReturnValueHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<HandlerMethodReturnValueHandler> returnValueHandlers =
		new ArrayList<HandlerMethodReturnValueHandler>();

	private final Map<MethodParameter, HandlerMethodReturnValueHandler> returnValueHandlerCache =
		new ConcurrentHashMap<MethodParameter, HandlerMethodReturnValueHandler>();

	/**
	 * Return a read-only list with the registered handlers, or an empty list.
	 */
	public List<HandlerMethodReturnValueHandler> getHandlers() {
		return Collections.unmodifiableList(this.returnValueHandlers);
	}

	/**
	 * Whether the given {@linkplain MethodParameter method return type} is supported by any registered
	 * {@link HandlerMethodReturnValueHandler}.
	 */
	public boolean supportsReturnType(MethodParameter returnType) {
		return getReturnValueHandler(returnType) != null;
	}

	/**
	 * Iterate over registered {@link HandlerMethodReturnValueHandler}s and invoke the one that supports it.
	 * @exception IllegalStateException if no suitable {@link HandlerMethodReturnValueHandler} is found.
	 */
	public void handleReturnValue(
			Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws Exception {

		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		Assert.notNull(handler, "Unknown return value type [" + returnType.getParameterType().getName() + "]");
		handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
	}

	/**
	 * Find a registered {@link HandlerMethodReturnValueHandler} that supports the given return type.
	 */
	private HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
		HandlerMethodReturnValueHandler result = this.returnValueHandlerCache.get(returnType);
		if (result == null) {
			for (HandlerMethodReturnValueHandler returnValueHandler : returnValueHandlers) {
				if (logger.isTraceEnabled()) {
					logger.trace("Testing if return value handler [" + returnValueHandler + "] supports [" +
							returnType.getGenericParameterType() + "]");
				}
				if (returnValueHandler.supportsReturnType(returnType)) {
					result = returnValueHandler;
					this.returnValueHandlerCache.put(returnType, returnValueHandler);
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Add the given {@link HandlerMethodReturnValueHandler}.
	 */
	public HandlerMethodReturnValueHandlerComposite addHandler(HandlerMethodReturnValueHandler returnValuehandler) {
		returnValueHandlers.add(returnValuehandler);
		return this;
	}

	/**
	 * Add the given {@link HandlerMethodReturnValueHandler}s.
	 */
	public HandlerMethodReturnValueHandlerComposite addHandlers(
			List<? extends HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers != null) {
			for (HandlerMethodReturnValueHandler handler : returnValueHandlers) {
				this.returnValueHandlers.add(handler);
			}
		}
		return this;
	}

}
