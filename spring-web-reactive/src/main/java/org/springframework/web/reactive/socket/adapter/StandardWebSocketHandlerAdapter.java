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

package org.springframework.web.reactive.socket.adapter;

import javax.websocket.Endpoint;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Adapter for Java WebSocket API (JSR-356).
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StandardWebSocketHandlerAdapter extends WebSocketHandlerAdapterSupport {


	public StandardWebSocketHandlerAdapter(WebSocketHandler delegate, HandshakeInfo info,
			DataBufferFactory bufferFactory) {

		super(delegate, info, bufferFactory);
	}


	public Endpoint getEndpoint() {
		return new StandardEndpoint(getDelegate(), getHandshakeInfo(), bufferFactory());
	}

}
