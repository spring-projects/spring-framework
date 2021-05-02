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

package org.springframework.web.socket.config.annotation;

import java.util.List;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Provides methods for configuring a WebSocket handler.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface WebSocketHandlerRegistration {

	/**
	 * Add more handlers that will share the same configuration (interceptors, SockJS
	 * config, etc).
	 */
	WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths);

	/**
	 * Configure the HandshakeHandler to use.
	 */
	WebSocketHandlerRegistration setHandshakeHandler(HandshakeHandler handshakeHandler);

	/**
	 * Configure interceptors for the handshake request.
	 */
	WebSocketHandlerRegistration addInterceptors(HandshakeInterceptor... interceptors);

	/**
	 * Set the origins for which cross-origin requests are allowed from a browser.
	 * Please, refer to {@link CorsConfiguration#setAllowedOrigins(List)} for
	 * format details and considerations, and keep in mind that the CORS spec
	 * does not allow use of {@code "*"} with {@code allowCredentials=true}.
	 * For more flexible origin patterns use {@link #setAllowedOriginPatterns}
	 * instead.
	 *
	 * <p>By default, no origins are allowed. When
	 * {@link #setAllowedOriginPatterns(String...) allowedOriginPatterns} is also
	 * set, then that takes precedence over this property.
	 *
	 * <p>Note when SockJS is enabled and origins are restricted, transport types
	 * that do not allow to check request origin (Iframe based transports) are
	 * disabled. As a consequence, IE 6 to 9 are not supported when origins are
	 * restricted.
	 *
	 * @since 4.1.2
	 * @see #setAllowedOriginPatterns(String...)
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	WebSocketHandlerRegistration setAllowedOrigins(String... origins);

	/**
	 * Alternative to {@link #setAllowedOrigins(String...)} that supports more
	 * flexible patterns for specifying the origins for which cross-origin
	 * requests are allowed from a browser. Please, refer to
	 * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
	 * details and other considerations.
	 * <p>By default this is not set.
	 * @since 5.3.5
	 */
	WebSocketHandlerRegistration setAllowedOriginPatterns(String... originPatterns);

	/**
	 * Enable SockJS fallback options.
	 */
	SockJsServiceRegistration withSockJS();



}
