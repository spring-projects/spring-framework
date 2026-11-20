/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * A HandlerMethodReturnValueHandler that wraps and delegates to others.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class HandlerMethodReturnValueHandlerComposite implements HandlerMethodReturnValueHandler {

	protected final Log logger = LogFactory.getLog(getClass());

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
	 * Add the given {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.
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

	@Override
	public Mono<Void> handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message) {
		HandlerMethodReturnValueHandler handler = getReturnValueHandler(returnType);
		if (handler == null) {
			throw new IllegalStateException("No handler for return value type: " + returnType.getParameterType());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Processing return value with " + handler);
		}
		return handler.handleReturnValue(returnValue, returnType, message);
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private @Nullable HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
		for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
			if (handler.supportsReturnType(returnType)) {
				return handler;
			}
		}
		return null;
	}

}
