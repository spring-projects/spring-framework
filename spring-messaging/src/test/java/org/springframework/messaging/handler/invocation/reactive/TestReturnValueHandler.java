/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation.reactive;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * Return value handler that simply stores the last return value.
 * @author Rossen Stoyanchev
 */
public class TestReturnValueHandler implements HandlerMethodReturnValueHandler {

	private @Nullable Object lastReturnValue;


	public @Nullable Object getLastReturnValue() {
		return this.lastReturnValue;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Mono<Void> handleReturnValue(@Nullable Object value, MethodParameter returnType, Message<?> message) {
		return (value instanceof Publisher publisher ?
				new ChannelSendOperator(publisher, this::saveValue) :
				saveValue(value));
	}

	private Mono<Void> saveValue(@Nullable Object value) {
		this.lastReturnValue = value;
		return Mono.empty();
	}

}
