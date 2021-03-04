/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.socket.server.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

/**
 * An interceptor to check request {@code Origin} header value against a
 * collection of allowed origins.
 *
 * @author Sebastien Deleuze
 * @since 4.1.2
 */
public class OriginHandshakeInterceptor implements HandshakeInterceptor {

	protected final Log logger = LogFactory.getLog(getClass());

	private final CorsConfiguration corsConfiguration = new CorsConfiguration();


	/**
	 * Default constructor with only same origin requests allowed.
	 */
	public OriginHandshakeInterceptor() {
	}

	/**
	 * Constructor using the specified allowed origin values.
	 * @see #setAllowedOrigins(Collection)
	 */
	public OriginHandshakeInterceptor(Collection<String> allowedOrigins) {
		setAllowedOrigins(allowedOrigins);
	}


	/**
	 * Configure allowed {@code Origin} header values. This check is mostly
	 * designed for browsers. There is nothing preventing other types of client
	 * to modify the {@code Origin} header value.
	 * <p>Each provided allowed origin must have a scheme, and optionally a port
	 * (e.g. "https://example.org", "https://example.org:9090"). An allowed origin
	 * string may also be "*" in which case all origins are allowed.
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.corsConfiguration.setAllowedOrigins(new ArrayList<>(allowedOrigins));
	}

	/**
	 * Return the allowed {@code Origin} header values.
	 * @since 4.1.5
	 */
	public Collection<String> getAllowedOrigins() {
		List<String> allowedOrigins = this.corsConfiguration.getAllowedOrigins();
		return (CollectionUtils.isEmpty(allowedOrigins) ? Collections.emptySet() :
				Collections.unmodifiableSet(new LinkedHashSet<>(allowedOrigins)));
	}

	/**
	 * A variant of {@link #setAllowedOrigins(Collection)} that accepts flexible
	 * domain patterns, e.g. {@code "https://*.domain1.com"}. Furthermore it
	 * always sets the {@code Access-Control-Allow-Origin} response header to
	 * the matched origin and never to {@code "*"}, nor to any other pattern.
	 * @since 5.3.2
	 * @see CorsConfiguration#setAllowedOriginPatterns(List)
	 */
	public void setAllowedOriginPatterns(Collection<String> allowedOriginPatterns) {
		Assert.notNull(allowedOriginPatterns, "Allowed origin patterns Collection must not be null");
		this.corsConfiguration.setAllowedOriginPatterns(new ArrayList<>(allowedOriginPatterns));
	}

	/**
	 * Return the allowed {@code Origin} pattern header values.
	 * @since 5.3.2
	 * @see CorsConfiguration#getAllowedOriginPatterns()
	 */
	public Collection<String> getAllowedOriginPatterns() {
		List<String> allowedOriginPatterns = this.corsConfiguration.getAllowedOriginPatterns();
		return (CollectionUtils.isEmpty(allowedOriginPatterns) ? Collections.emptySet() :
				Collections.unmodifiableSet(new LinkedHashSet<>(allowedOriginPatterns)));
	}


	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		if (!WebUtils.isSameOrigin(request) &&
				this.corsConfiguration.checkOrigin(request.getHeaders().getOrigin()) == null) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request rejected, Origin header value " +
						request.getHeaders().getOrigin() + " not allowed");
			}
			return false;
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, @Nullable Exception exception) {
	}

}
