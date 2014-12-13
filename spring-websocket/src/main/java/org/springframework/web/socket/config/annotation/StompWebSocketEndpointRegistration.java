/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * A contract for configuring a STOMP over WebSocket endpoint.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface StompWebSocketEndpointRegistration {

	/**
	 * Enable SockJS fallback options.
	 */
	SockJsServiceRegistration withSockJS();

	/**
	 * Configure the HandshakeHandler to use.
	 */
	StompWebSocketEndpointRegistration setHandshakeHandler(HandshakeHandler handshakeHandler);

	/**
	 * Configure the HandshakeInterceptor's to use.
	 */
	StompWebSocketEndpointRegistration addInterceptors(HandshakeInterceptor... interceptors);

	/**
	 * Configure allowed {@code Origin} header values. This check is mostly designed for browser
	 * clients. There is noting preventing other types of client to modify the Origin header value.
	 *
	 * <p>When SockJS is enabled and allowed origins are restricted, transport types that do not
	 * use {@code Origin} headers for cross origin requests (jsonp-polling, iframe-xhr-polling,
	 * iframe-eventsource and iframe-htmlfile) are disabled. As a consequence, IE6/IE7 won't be
	 * supported anymore and IE8/IE9 will only be supported without cookies.
	 *
	 * <p>By default, all origins are allowed.
	 * @since 4.1.2
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	StompWebSocketEndpointRegistration setAllowedOrigins(String... origins);

}