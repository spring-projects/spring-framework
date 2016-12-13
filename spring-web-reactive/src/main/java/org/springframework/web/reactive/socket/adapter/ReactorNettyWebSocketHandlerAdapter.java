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

import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import reactor.ipc.netty.http.websocket.WebsocketInbound;
import reactor.ipc.netty.http.websocket.WebsocketOutbound;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Reactor Netty {@code WebSocketHandler} implementation adapting and
 * delegating to a Spring {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketHandlerAdapter extends WebSocketHandlerAdapterSupport
		implements BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {


	public ReactorNettyWebSocketHandlerAdapter(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler) {

		super(request, response, handler);
	}


	@Override
	public Publisher<Void> apply(WebsocketInbound inbound, WebsocketOutbound outbound) {
		ReactorNettyWebSocketSession session =
				new ReactorNettyWebSocketSession(inbound, outbound, getUri(), getBufferFactory());
		return getDelegate().handle(session);
	}

}
