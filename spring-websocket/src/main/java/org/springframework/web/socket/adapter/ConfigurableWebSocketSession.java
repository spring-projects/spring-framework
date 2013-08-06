/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.adapter;

import java.net.URI;
import java.security.Principal;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.DefaultHandshakeHandler;

/**
 * A WebSocketSession with configurable properties.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ConfigurableWebSocketSession extends WebSocketSession {

	void setUri(URI uri);

	void setRemoteHostName(String name);

	void setRemoteAddress(String address);

	void setPrincipal(Principal principal);

	/**
	 * Set the protocol accepted as part of the WebSocket handshake. This property can be
	 * used when the WebSocket handshake is performed through
	 * {@link DefaultHandshakeHandler} rather than the underlying WebSocket runtime, or
	 * when there is no WebSocket handshake (e.g. SockJS HTTP fallback options)
	 */
	void setAcceptedProtocol(String protocol);

}
