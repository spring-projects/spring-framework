/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.socket.adapter;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Base class for {@link WebSocketHandler} adapters to WebSocket handler APIs
 * of underlying runtimes.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class WebSocketHandlerAdapterSupport {

	private final HandshakeInfo handshakeInfo;

	private final WebSocketHandler delegate;

	private final DataBufferFactory bufferFactory;


	protected WebSocketHandlerAdapterSupport(HandshakeInfo handshakeInfo, DataBufferFactory bufferFactory,
			WebSocketHandler handler) {

		Assert.notNull(handshakeInfo, "HandshakeInfo is required.");
		Assert.notNull(bufferFactory, "DataBufferFactory is required");
		Assert.notNull(handler, "WebSocketHandler handler is required");

		this.handshakeInfo = handshakeInfo;
		this.bufferFactory = bufferFactory;
		this.delegate = handler;
	}


	protected HandshakeInfo getHandshakeInfo() {
		return this.handshakeInfo;
	}

	protected WebSocketHandler getDelegate() {
		return this.delegate;
	}

	@SuppressWarnings("unchecked")
	protected <T extends DataBufferFactory> T getBufferFactory() {
		return (T) this.bufferFactory;
	}

}
