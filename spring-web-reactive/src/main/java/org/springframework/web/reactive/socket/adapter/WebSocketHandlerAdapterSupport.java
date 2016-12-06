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

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Base class for {@link WebSocketHandler} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class WebSocketHandlerAdapterSupport {

	private final URI uri;

	private final WebSocketHandler delegate;


	protected WebSocketHandlerAdapterSupport(ServerHttpRequest request, WebSocketHandler handler) {
		Assert.notNull("'request' is required");
		Assert.notNull("'handler' handler is required");
		this.uri = request.getURI();
		this.delegate = handler;
	}


	public URI getUri() {
		return this.uri;
	}

	public WebSocketHandler getDelegate() {
		return this.delegate;
	}

}
