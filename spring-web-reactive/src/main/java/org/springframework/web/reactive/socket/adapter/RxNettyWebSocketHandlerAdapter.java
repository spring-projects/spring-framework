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

import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * RxNetty {@code WebSocketHandler} implementation adapting and delegating to a
 * Spring {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyWebSocketHandlerAdapter extends WebSocketHandlerAdapterSupport
		implements io.reactivex.netty.protocol.http.ws.server.WebSocketHandler {

	private final NettyDataBufferFactory bufferFactory;


	public RxNettyWebSocketHandlerAdapter(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler) {

		super(request, handler);
		Assert.notNull("'response' is required");
		this.bufferFactory = (NettyDataBufferFactory) response.bufferFactory();
	}


	public NettyDataBufferFactory getBufferFactory() {
		return this.bufferFactory;
	}

	@Override
	public Observable<Void> handle(WebSocketConnection conn) {
		RxNettyWebSocketSession session = new RxNettyWebSocketSession(conn, getUri(), getBufferFactory());
		Mono<Void> result = getDelegate().handle(session);
		return RxReactiveStreams.toObservable(result);
	}

}
