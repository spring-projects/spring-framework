/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.server.reactive.observation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import io.micrometer.observation.transport.RequestReplyReceiverContext;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * Context that holds information for metadata collection regarding
 * {@link ServerHttpObservationDocumentation#HTTP_REACTIVE_SERVER_REQUESTS reactive HTTP requests}
 * observations.
 *
 * <p>This context also extends {@link RequestReplyReceiverContext} for propagating
 * tracing information during HTTP request processing.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class ServerRequestObservationContext extends RequestReplyReceiverContext<ServerHttpRequest, ServerHttpResponse> {

	/**
	 * Name of the request attribute holding the {@link ServerRequestObservationContext context}
	 * for the current observation.
	 * @since 6.1
	 */
	public static final String CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE = ServerRequestObservationContext.class.getName();


	private final Map<String, Object> attributes;

	@Nullable
	private String pathPattern;

	private boolean connectionAborted;


	/**
	 * Create a new {@code ServerRequestObservationContext} instance.
	 * @param request the current request
	 * @param response the current response
	 * @param attributes the current attributes
	 */
	public ServerRequestObservationContext(
			ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> attributes) {

		super((req, key) -> req.getHeaders().getFirst(key));
		setCarrier(request);
		setResponse(response);
		this.attributes = Collections.unmodifiableMap(attributes);
	}


	/**
	 * Return an immutable map of the current request attributes.
	 */
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * Return the path pattern for the handler that matches the current request.
	 * For example, {@code "/projects/{name}"}.
	 * <p>Path patterns must have a low cardinality for the entire application.
	 * @return the path pattern, or {@code null} if none found
	 */
	@Nullable
	public String getPathPattern() {
		return this.pathPattern;
	}

	/**
	 * Set the path pattern for the handler that matches the current request.
	 * <p>Path patterns must have a low cardinality for the entire application.
	 * @param pathPattern the path pattern, for example {@code "/projects/{name}"}.
	 */
	public void setPathPattern(@Nullable String pathPattern) {
		this.pathPattern = pathPattern;
	}

	/**
	 * Whether the current connection was aborted by the client, resulting in a
	 * {@link reactor.core.publisher.SignalType#CANCEL cancel signal} on the reactive chain,
	 * or an {@code AbortedException} when reading the request.
	 * @return if the connection has been aborted
	 */
	public boolean isConnectionAborted() {
		return this.connectionAborted;
	}

	/**
	 * Set whether the current connection was aborted by the client, resulting in a
	 * {@link reactor.core.publisher.SignalType#CANCEL cancel signal} on the reactive chain,
	 * or an {@code AbortedException} when reading the request.
	 * @param connectionAborted if the connection has been aborted
	 */
	public void setConnectionAborted(boolean connectionAborted) {
		this.connectionAborted = connectionAborted;
	}


	/**
	 * Get the current {@link ServerRequestObservationContext observation context}
	 * from the given attributes, if available.
	 * @param attributes the current exchange attributes
	 * @return the current observation context
	 * @since 6.1
	 */
	public static Optional<ServerRequestObservationContext> findCurrent(Map<String, Object> attributes) {
		return Optional.ofNullable(
				(ServerRequestObservationContext) attributes.get(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE));
	}

}
