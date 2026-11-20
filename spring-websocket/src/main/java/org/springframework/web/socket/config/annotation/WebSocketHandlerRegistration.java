/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
