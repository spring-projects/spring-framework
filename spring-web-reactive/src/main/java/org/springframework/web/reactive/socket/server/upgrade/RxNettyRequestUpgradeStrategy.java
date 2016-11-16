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
package org.springframework.web.reactive.socket.server.upgrade;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.http.server.reactive.RxNettyServerHttpRequest;
import org.springframework.http.server.reactive.RxNettyServerHttpResponse;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.RxNettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestUpgradeStrategy} for use with RxNetty.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler webSocketHandler) {

		RxNettyServerHttpRequest request = (RxNettyServerHttpRequest) exchange.getRequest();
		RxNettyServerHttpResponse response = (RxNettyServerHttpResponse) exchange.getResponse();

		RxNettyWebSocketHandlerAdapter rxNettyHandler =
				new RxNettyWebSocketHandlerAdapter(request, response, webSocketHandler);

		Observable<Void> completion = response.getRxNettyResponse()
				.acceptWebSocketUpgrade(rxNettyHandler)
				.subprotocol(getSubProtocols(webSocketHandler));

		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

	private static String[] getSubProtocols(WebSocketHandler webSocketHandler) {
		List<String> subProtocols = webSocketHandler.getSubProtocols();
		return subProtocols.toArray(new String[subProtocols.size()]);
	}

}
