/*
/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.messaging.handler.invocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A HandlerMethodReturnValueHandler that wraps and delegates to others.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HandlerMethodReturnValueHandlerComposite implements AsyncHandlerMethodReturnValueHandler {

	private static final Log logger = LogFactory.getLog(HandlerMethodReturnValueHandlerComposite.class);

	private final List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();


	/**
	 * Return a read-only list with the configured handlers.
	 */
	public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		return Collections.unmodifiableList(this.returnValueHandlers);
	}

	/**
	 * Clear the list of configured handlers.
	 */
	public void clear() {
		this.returnValueHandlers.clear();
	}

	/**
	 * Add the given {@link HandlerMethodReturnValueHandler}.
	 */
	public HandlerMethodReturnValueHandlerComposite addHandler(HandlerMethodReturnValueHandler returnValueHandler) {
		this.returnValueHandlers.add(returnValueHandler);
		return this;
	}

	/**
	 * Add the given {@link HandlerMethodReturnValueHandler}s.
	 */
	public HandlerMethodReturnValueHandlerComposite addHandlers(
			@Nullable List<? extends HandlerMethodReturnValueHandler> handlers) {

		if (handlers != null) {
			this.returnValueHandlers.addAll(handlers);
		}
		return this;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return getReturnValueHandler(returnType) != null;
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Nullable
	private HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
		for (int i = 0; i < this.returnValueHandlers.size(); i++) {
			HandlerMethodReturnValueHandler handler = this.returnValueHandlers.get(i);
			if (handler.supportsReturnType(returnType)) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		if (handler == null) {
			throw new IllegalStateException("No handler for return value type: " + returnType.getParameterType());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Processing return value with " + handler);
		}
		handler.handleReturnValue(returnValue, returnType, message);
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		return (handler instanceof AsyncHandlerMethodReturnValueHandler &&
				((AsyncHandlerMethodReturnValueHandler) handler).isAsyncReturnValue(returnValue, returnType));
	}

	@Override
	@Nullable
	public ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType) {
		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		Assert.state(handler instanceof AsyncHandlerMethodReturnValueHandler,
				"AsyncHandlerMethodReturnValueHandler required");
		return ((AsyncHandlerMethodReturnValueHandler) handler).toListenableFuture(returnValue, returnType);
	}

}
