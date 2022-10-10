/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.observation.reactive;

import io.micrometer.observation.transport.RequestReplyReceiverContext;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Context that holds information for metadata collection during observations for reactive web applications.
 * <p>This context also extends {@link RequestReplyReceiverContext} for propagating
 * tracing information with the HTTP server exchange.
 * @author Brian Clozel
 * @since 6.0
 */
public class HttpRequestsObservationContext extends RequestReplyReceiverContext<ServerHttpRequest, ServerHttpResponse> {

	@Nullable
	private PathPattern pathPattern;

	private boolean connectionAborted;

	public HttpRequestsObservationContext(ServerWebExchange exchange) {
		super((request, key) -> request.getHeaders().getFirst(key));
		setCarrier(exchange.getRequest());
		setResponse(exchange.getResponse());
	}

	/**
	 * Return the path pattern for the handler that matches the current request.
	 * For example, {@code "/projects/{name}"}.
	 * <p>Path patterns must have a low cardinality for the entire application.
	 * @return the path pattern, or {@code null} if none found
	 */
	@Nullable
	public PathPattern getPathPattern() {
		return this.pathPattern;
	}

	/**
	 * Set the path pattern for the handler that matches the current request.
	 * <p>Path patterns must have a low cardinality for the entire application.
	 * @param pathPattern the path pattern, for example {@code "/projects/{name}"}.
	 */
	public void setPathPattern(@Nullable PathPattern pathPattern) {
		this.pathPattern = pathPattern;
	}

	/**
	 * Whether the current connection was aborted by the client, resulting
	 * in a {@link reactor.core.publisher.SignalType#CANCEL cancel signal} on the reactive chain,
	 * or an {@code AbortedException} when reading the request.
	 * @return if the connection has been aborted
	 */
	public boolean isConnectionAborted() {
		return this.connectionAborted;
	}

	void setConnectionAborted(boolean connectionAborted) {
		this.connectionAborted = connectionAborted;
	}

}
