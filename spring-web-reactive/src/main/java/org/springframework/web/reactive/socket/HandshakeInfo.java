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
package org.springframework.web.reactive.socket;

import java.net.URI;
import java.security.Principal;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Simple container of information related to the handshake request that started
 * the {@link WebSocketSession} session.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebSocketSession#getHandshakeInfo()
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class HandshakeInfo {

	private final URI uri;

	private final Mono<Principal> principalMono;

	private HttpHeaders headers;

	private Optional<String> protocol;


	public HandshakeInfo(URI uri, Mono<Principal> principal) {
		this(uri, new HttpHeaders(), principal, Optional.empty());
	}

	public HandshakeInfo(URI uri, HttpHeaders headers, Mono<Principal> principal,
			Optional<String> subProtocol) {

		Assert.notNull(uri, "URI is required.");
		Assert.notNull(headers, "HttpHeaders are required.");
		Assert.notNull(principal, "Principal is required.");
		Assert.notNull(subProtocol, "Sub-protocol is required.");
		this.uri = uri;
		this.headers = headers;
		this.principalMono = principal;
		this.protocol = subProtocol;
	}


	/**
	 * Return the URL for the WebSocket endpoint.
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return the handshake HTTP headers. Those are the request headers for a
	 * server session and the response headers for a client session.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Sets the handshake HTTP headers. Those are the request headers for a
	 * server session and the response headers for a client session.
	 * @param headers the handshake HTTP headers.
	 */
	public void setHeaders(HttpHeaders headers) {
		this.headers = headers;
	}

	/**
	 * Return the principal associated with the handshake HTTP request.
	 */
	public Mono<Principal> getPrincipal() {
		return this.principalMono;
	}

	/**
	 * The sub-protocol negotiated at handshake time.
	 * @see <a href="https://tools.ietf.org/html/rfc6455#section-1.9">
	 *     https://tools.ietf.org/html/rfc6455#section-1.9</a>
	 */
	public Optional<String> getSubProtocol() {
		return this.protocol;
	}

	/**
	 * Sets the sub-protocol negotiated at handshake time.
	 * @param protocol the sub-protocol negotiated at handshake time.
	 */
	public void setSubProtocol(Optional<String> protocol) {
		this.protocol = protocol;
	}


	@Override
	public String toString() {
		return "HandshakeInfo[uri=" + this.uri + ", headers=" + this.headers + "]";
	}

}
