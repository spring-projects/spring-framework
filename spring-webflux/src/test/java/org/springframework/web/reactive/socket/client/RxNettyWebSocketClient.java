/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import io.reactivex.netty.protocol.http.ws.client.WebSocketRequest;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import io.reactivex.netty.threads.RxEventLoopProvider;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.RxNettyWebSocketSession;

import static io.reactivex.netty.protocol.http.HttpHandlerNames.WsClientDecoder;

/**
 * {@link WebSocketClient} implementation for use with RxNetty.
 * For internal use within the framework.
 * 
 * <p><strong>Note: </strong> RxNetty {@link HttpClient} instances require a host
 * and port in order to be created. Hence it is not possible to configure a
 * single {@code HttpClient} instance to use upfront. Instead the constructors
 * accept a function for obtaining client instances when establishing a
 * connection to a specific URI. By default new instances are created per
 * connection with a shared Netty {@code EventLoopGroup}. See constructors for
 * more details.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyWebSocketClient extends WebSocketClientSupport implements WebSocketClient {

	private final Function<URI, HttpClient<ByteBuf, ByteBuf>> httpClientProvider;


	/**
	 * Default constructor that creates {@code HttpClient} instances via
	 * {@link HttpClient#newClient(String, int)} using port 80 or 443 depending
	 * on the target URL scheme.
	 *
	 * <p><strong>Note: </strong> By default a new {@link HttpClient} instance
	 * is created per WebSocket connection. Those instances will share a global
	 * {@code EventLoopGroup} that RxNetty obtains via
	 * {@link RxEventLoopProvider#globalClientEventLoop(boolean)}.
	 */
	public RxNettyWebSocketClient() {
		this(RxNettyWebSocketClient::getDefaultHttpClientProvider);
	}

	/**
	 * Constructor with a function to use to obtain {@link HttpClient} instances.
	 */
	public RxNettyWebSocketClient(Function<URI, HttpClient<ByteBuf, ByteBuf>> httpClientProvider) {
		this.httpClientProvider = httpClientProvider;
	}

	private static HttpClient<ByteBuf, ByteBuf> getDefaultHttpClientProvider(URI url) {
		boolean secure = "wss".equals(url.getScheme());
		int port = (url.getPort() > 0 ? url.getPort() : secure ? 443 : 80);
		return HttpClient.newClient(url.getHost(), port);
	}


	/**
	 * Return the configured {@link HttpClient} provider depending on which
	 * constructor was used.
	 */
	public Function<URI, HttpClient<ByteBuf, ByteBuf>> getHttpClientProvider() {
		return this.httpClientProvider;
	}

	/**
	 * Return an {@link HttpClient} instance to use to connect to the given URI.
	 * The default implementation invokes the {@link #getHttpClientProvider()}
	 * provider} function created or supplied at construction time.
	 * @param url the full URL of the WebSocket endpoint.
	 */
	public HttpClient<ByteBuf, ByteBuf> getHttpClient(URI url) {
		return this.httpClientProvider.apply(url);
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		Observable<Void> completion = executeInternal(url, headers, handler);
		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

	@SuppressWarnings("cast")
	private Observable<Void> executeInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		String[] protocols = beforeHandshake(url, headers, handler);
		return createRequest(url, headers, protocols)
				.flatMap(response -> {
					Observable<WebSocketConnection> conn = response.getWebSocketConnection();
					// following cast is necessary to enable compilation on Eclipse 4.6
					return (Observable<Tuple2<WebSocketResponse<ByteBuf>, WebSocketConnection>>)
							Observable.zip(Observable.just(response), conn, Tuples::of);
				})
				.flatMap(tuple -> {
					WebSocketResponse<ByteBuf> response = tuple.getT1();
					WebSocketConnection conn = tuple.getT2();

					HandshakeInfo info = afterHandshake(url, toHttpHeaders(response));
					ByteBufAllocator allocator = response.unsafeNettyChannel().alloc();
					NettyDataBufferFactory factory = new NettyDataBufferFactory(allocator);
					RxNettyWebSocketSession session = new RxNettyWebSocketSession(conn, info, factory);
					session.aggregateFrames(response.unsafeNettyChannel(), WsClientDecoder.getName());

					return RxReactiveStreams.toObservable(handler.handle(session));
				});
	}

	private WebSocketRequest<ByteBuf> createRequest(URI url, HttpHeaders headers, String[] protocols) {
		String query = url.getRawQuery();
		String requestUrl = url.getRawPath() + (query != null ? "?" + query : "");
		HttpClientRequest<ByteBuf, ByteBuf> request = getHttpClient(url).createGet(requestUrl);

		if (!headers.isEmpty()) {
			Map<String, List<Object>> map = new HashMap<>(headers.size());
			headers.forEach((key, values) -> map.put(key, new ArrayList<>(headers.get(key))));
			request = request.setHeaders(map);
		}

		return (ObjectUtils.isEmpty(protocols) ?
				request.requestWebSocketUpgrade() :
				request.requestWebSocketUpgrade().requestSubProtocols(protocols));
	}

	private HttpHeaders toHttpHeaders(WebSocketResponse<ByteBuf> response) {
		HttpHeaders headers = new HttpHeaders();
		response.headerIterator().forEachRemaining(entry -> {
			String name = entry.getKey().toString();
			headers.put(name, response.getAllHeaderValues(name));
		});
		return headers;
	}

}
