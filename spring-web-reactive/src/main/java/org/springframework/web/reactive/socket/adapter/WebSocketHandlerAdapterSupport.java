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

import java.net.URI;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Base class for {@link WebSocketHandler} adapters to underlying WebSocket
 * handler APIs.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class WebSocketHandlerAdapterSupport {

	private final URI uri;

	private final WebSocketHandler delegate;

	private final DataBufferFactory bufferFactory;


	protected WebSocketHandlerAdapterSupport(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler) {

		Assert.notNull("ServerHttpRequest is required");
		Assert.notNull("ServerHttpResponse is required");
		Assert.notNull("WebSocketHandler handler is required");
		this.uri = request.getURI();
		this.bufferFactory = response.bufferFactory();
		this.delegate = handler;
	}


	protected URI getUri() {
		return this.uri;
	}

	protected WebSocketHandler getDelegate() {
		return this.delegate;
	}

	@SuppressWarnings("unchecked")
	protected <T extends DataBufferFactory> T getBufferFactory() {
		return (T) this.bufferFactory;
	}

}
