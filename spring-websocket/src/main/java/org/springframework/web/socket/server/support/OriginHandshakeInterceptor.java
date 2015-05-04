/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.server.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

/**
 * An interceptor to check request {@code Origin} header value against a collection of
 * allowed origins.
 *
 * @author Sebastien Deleuze
 * @since 4.1.2
 */
public class OriginHandshakeInterceptor implements HandshakeInterceptor {

	protected Log logger = LogFactory.getLog(getClass());

	private final List<String> allowedOrigins;


	/**
	 * Default constructor with only same origin requests allowed.
	 */
	public OriginHandshakeInterceptor() {
		this.allowedOrigins = new ArrayList<String>();
	}

	/**
	 * Constructor using the specified allowed origin values.
	 *
	 * @see #setAllowedOrigins(Collection)
	 */
	public OriginHandshakeInterceptor(Collection<String> allowedOrigins) {
		this();
		setAllowedOrigins(allowedOrigins);
	}

	/**
	 * Configure allowed {@code Origin} header values. This check is mostly designed for
	 * browser clients. There is nothing preventing other types of client to modify the
	 * {@code Origin} header value.
	 *
	 * <p>Each provided allowed origin must start by "http://", "https://" or be "*"
	 * (means that all origins are allowed).
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origin Collection must not be null");
		this.allowedOrigins.clear();
		this.allowedOrigins.addAll(allowedOrigins);
	}

	/**
	 * @see #setAllowedOrigins(Collection)
	 * @since 4.1.5
	 */
	public Collection<String> getAllowedOrigins() {
		return Collections.unmodifiableList(this.allowedOrigins);
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		if (!WebUtils.isValidOrigin(request, this.allowedOrigins)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request rejected, Origin header value "
						+ request.getHeaders().getOrigin() + " not allowed");
			}
			return false;
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}

}
