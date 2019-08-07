/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Base class for {@link WebSocketClient} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebSocketClientSupport {

	private static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";


	protected final Log logger = LogFactory.getLog(getClass());


	protected List<String> beforeHandshake(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing handshake to " + url);
		}
		return handler.getSubProtocols();
	}

	protected HandshakeInfo afterHandshake(URI url, HttpHeaders responseHeaders) {
		if (logger.isDebugEnabled()) {
			logger.debug("Handshake response: " + url + ", " + responseHeaders);
		}
		String protocol = responseHeaders.getFirst(SEC_WEBSOCKET_PROTOCOL);
		return new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
	}

}
