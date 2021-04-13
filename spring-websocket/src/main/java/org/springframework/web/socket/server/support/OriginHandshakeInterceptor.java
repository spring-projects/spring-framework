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
	 * Set the origins for which cross-origin requests are allowed from a browser.
	 * Please, refer to {@link CorsConfiguration#setAllowedOrigins(List)} for
	 * format details and considerations, and keep in mind that the CORS spec
	 * does not allow use of {@code "*"} with {@code allowCredentials=true}.
	 * For more flexible origin patterns use {@link #setAllowedOriginPatterns}
	 * instead.
	 *
	 * <p>By default, no origins are allowed. When
	 * {@link #setAllowedOriginPatterns(Collection) allowedOriginPatterns} is also
	 * set, then that takes precedence over this property.
	 *
	 * <p>Note when SockJS is enabled and origins are restricted, transport types
	 * that do not allow to check request origin (Iframe based transports) are
	 * disabled. As a consequence, IE 6 to 9 are not supported when origins are
	 * restricted.
	 *
	 * @see #setAllowedOriginPatterns(Collection)
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.corsConfiguration.setAllowedOrigins(new ArrayList<>(allowedOrigins));
	}

	/**
	 * Return the {@link #setAllowedOriginPatterns(Collection) configured} allowed origins.
	 * @since 4.1.5
	 */
	public Collection<String> getAllowedOrigins() {
		List<String> allowedOrigins = this.corsConfiguration.getAllowedOrigins();
		return (CollectionUtils.isEmpty(allowedOrigins) ? Collections.emptySet() :
				Collections.unmodifiableSet(new LinkedHashSet<>(allowedOrigins)));
	}

	/**
	 * Alternative to {@link #setAllowedOrigins(Collection)} that supports more
	 * flexible patterns for specifying the origins for which cross-origin
	 * requests are allowed from a browser. Please, refer to
	 * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
	 * details and other considerations.
	 * <p>By default this is not set.
	 * @since 5.3.2
	 */
	public void setAllowedOriginPatterns(Collection<String> allowedOriginPatterns) {
		Assert.notNull(allowedOriginPatterns, "Allowed origin patterns Collection must not be null");
		this.corsConfiguration.setAllowedOriginPatterns(new ArrayList<>(allowedOriginPatterns));
	}

	/**
	 * Return the {@link #setAllowedOriginPatterns(Collection) configured} allowed origin patterns.
	 * @since 5.3.2
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
