/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.reactive.socket;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

/**
 * Simple container of information related to the handshake request that started
 * the {@link WebSocketSession} session.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebSocketSession#getHandshakeInfo()
 */
public class HandshakeInfo {

	private static final MultiValueMap<String, HttpCookie> EMPTY_COOKIES =
			CollectionUtils.toMultiValueMap(Collections.emptyMap());


	private final URI uri;

	private final Mono<Principal> principalMono;

	private final HttpHeaders headers;

	private final MultiValueMap<String, HttpCookie> cookies;

	@Nullable
	private final String protocol;

	@Nullable
	private final InetSocketAddress remoteAddress;

	private final Map<String, Object> attributes;

	@Nullable
	private final String logPrefix;


	/**
	 * Constructor with basic information about the handshake.
	 * @param uri the endpoint URL
	 * @param headers request headers for server or response headers or client
	 * @param principal the principal for the session
	 * @param protocol the negotiated sub-protocol (may be {@code null})
	 */
	public HandshakeInfo(URI uri, HttpHeaders headers, Mono<Principal> principal, @Nullable String protocol) {
		this(uri, headers, EMPTY_COOKIES, principal, protocol, null, Collections.emptyMap(), null);
	}

	/**
	 * Constructor targeting server-side use with extra information such as
	 * the remote address, attributes, and a log prefix.
	 * @param uri the endpoint URL
	 * @param headers server request headers
	 * @param principal the principal for the session
	 * @param protocol the negotiated sub-protocol (may be {@code null})
	 * @param remoteAddress the remote address of the client
	 * @param attributes initial attributes for the WebSocket session
	 * @param logPrefix the log prefix for the handshake request.
	 * @since 5.1
	 * @deprecated as of 5.3.5 in favor of
	 * {@link #HandshakeInfo(URI, HttpHeaders, MultiValueMap, Mono, String, InetSocketAddress, Map, String)}
	 */
	@Deprecated
	public HandshakeInfo(URI uri, HttpHeaders headers, Mono<Principal> principal,
				@Nullable String protocol, @Nullable InetSocketAddress remoteAddress,
				Map<String, Object> attributes, @Nullable String logPrefix) {

		this(uri, headers, EMPTY_COOKIES, principal, protocol, remoteAddress, attributes, logPrefix);
	}

	/**
	 * Constructor targeting server-side use with extra information such as the
	 * cookies, remote address, attributes, and a log prefix.
	 * @param uri the endpoint URL
	 * @param headers server request headers
	 * @param cookies server request cookies
	 * @param principal the principal for the session
	 * @param protocol the negotiated sub-protocol (may be {@code null})
	 * @param remoteAddress the remote address of the client
	 * @param attributes initial attributes for the WebSocket session
	 * @param logPrefix the log prefix for the handshake request.
	 * @since 5.3.5
	 */
	public HandshakeInfo(URI uri, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies,
				Mono<Principal> principal, @Nullable String protocol, @Nullable InetSocketAddress remoteAddress,
				Map<String, Object> attributes, @Nullable String logPrefix) {

		Assert.notNull(uri, "URI is required");
		Assert.notNull(headers, "HttpHeaders are required");
		Assert.notNull(cookies, "`cookies` are required");
		Assert.notNull(principal, "Principal is required");
		Assert.notNull(attributes, "'attributes' is required");

		this.uri = uri;
		this.headers = headers;
		this.cookies = cookies;
		this.principalMono = principal;
		this.protocol = protocol;
		this.remoteAddress = remoteAddress;
		this.attributes = attributes;
		this.logPrefix = logPrefix;
	}


	/**
	 * Return the URL for the WebSocket endpoint.
	 */
	public URI getUri() {
		return this.uri;
	}

	/**
	 * Return the HTTP headers from the handshake request, either server request
	 * headers for a server session or the client response headers for a client
	 * session.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * For a server session this returns the server request cookies from the
	 * handshake request. For a client session, it is an empty map.
	 * @since 5.3.5
	 */
	public MultiValueMap<String, HttpCookie> getCookies() {
		return this.cookies;
	}

	/**
	 * Return the principal associated with the handshake request, if any.
	 */
	public Mono<Principal> getPrincipal() {
		return this.principalMono;
	}

	/**
	 * The sub-protocol negotiated at handshake time, or {@code null} if none.
	 * @see <a href="https://tools.ietf.org/html/rfc6455#section-1.9">
	 * https://tools.ietf.org/html/rfc6455#section-1.9</a>
	 */
	@Nullable
	public String getSubProtocol() {
		return this.protocol;
	}

	/**
	 * For a server session this is the remote address where the handshake
	 * request came from. For a client session, it is {@code null}.
	 * @since 5.1
	 */
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * Attributes extracted from the handshake request to add to the session.
	 * @since 5.1
	 */
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	/**
	 * A log prefix used in the handshake to correlate log messages, if any.
	 * @return a log prefix, or {@code null} if not specified
	 * @since 5.1
	 */
	@Nullable
	public String getLogPrefix() {
		return this.logPrefix;
	}


	@Override
	public String toString() {
		return "HandshakeInfo[uri=" + this.uri + "]";
	}

}
