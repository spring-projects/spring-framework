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
package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.reactivex.netty.protocol.http.HttpHandlerNames;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import io.reactivex.netty.protocol.http.ws.client.WebSocketRequest;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.RxNettyWebSocketSession;

/**
 * An RxNetty based implementation of {@link WebSocketClient}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyWebSocketClient extends WebSocketClientSupport implements WebSocketClient {

	private final Function<URI, HttpClient<ByteBuf, ByteBuf>> httpClientFactory;


	/**
	 * Default constructor that uses {@link HttpClient#newClient(String, int)}
	 * to create HTTP client instances when connecting.
	 */
	public RxNettyWebSocketClient() {
		this(RxNettyWebSocketClient::createDefaultHttpClient);
	}

	/**
	 * Constructor with a function to create {@link HttpClient} instances.
	 * @param httpClientFactory factory to create clients
	 */
	public RxNettyWebSocketClient(Function<URI, HttpClient<ByteBuf, ByteBuf>> httpClientFactory) {
		this.httpClientFactory = httpClientFactory;
	}

	private static HttpClient<ByteBuf, ByteBuf> createDefaultHttpClient(URI url) {
		boolean secure = "wss".equals(url.getScheme());
		int port = url.getPort() > 0 ? url.getPort() : secure ? 443 : 80;
		HttpClient<ByteBuf, ByteBuf> httpClient = HttpClient.newClient(url.getHost(), port);
		if (secure) {
			try {
				SSLContext context = SSLContext.getDefault();
				SSLEngine engine = context.createSSLEngine(url.getHost(), port);
				engine.setUseClientMode(true);
				httpClient.secure(engine);
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException("Failed to create HttpClient for " + url, ex);
			}
		}
		return httpClient;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		Observable<Void> completion = connectInternal(url, headers, handler);
		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

	private Observable<Void> connectInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return createRequest(url, headers, handler)
				.flatMap(response -> {
					Observable<WebSocketConnection> conn = response.getWebSocketConnection();
					return Observable.zip(Observable.just(response), conn, Tuples::of);
				})
				.flatMap(tuple -> {
					WebSocketResponse<ByteBuf> response = tuple.getT1();
					HttpHeaders responseHeaders = getResponseHeaders(response);
					Optional<String> protocol = Optional.ofNullable(response.getAcceptedSubProtocol());
					HandshakeInfo info = new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);

					ByteBufAllocator allocator = response.unsafeNettyChannel().alloc();
					NettyDataBufferFactory factory = new NettyDataBufferFactory(allocator);

					WebSocketConnection conn = tuple.getT2();
					RxNettyWebSocketSession session = new RxNettyWebSocketSession(conn, info, factory);
					String name = HttpHandlerNames.WsClientDecoder.getName();
					session.aggregateFrames(response.unsafeNettyChannel(), name);

					return RxReactiveStreams.toObservable(handler.handle(session));
				});
	}

	private WebSocketRequest<ByteBuf> createRequest(URI url, HttpHeaders headers, WebSocketHandler handler) {

		String query = url.getRawQuery();
		String requestUrl = url.getRawPath() + (query != null ? "?" + query : "");

		WebSocketRequest<ByteBuf> request = this.httpClientFactory.apply(url)
				.createGet(requestUrl)
				.setHeaders(toObjectValueMap(headers))
				.requestWebSocketUpgrade();

		String[] protocols = getSubProtocols(headers, handler);
		if (!ObjectUtils.isEmpty(protocols)) {
			request = request.requestSubProtocols(protocols);
		}

		return request;
	}

	private Map<String, List<Object>> toObjectValueMap(HttpHeaders headers) {
		if (headers.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, List<Object>> map = new HashMap<>(headers.size());
		headers.keySet().stream().forEach(key -> map.put(key, new ArrayList<>(headers.get(key))));
		return map;
	}

	private HttpHeaders getResponseHeaders(WebSocketResponse<ByteBuf> response) {
		HttpHeaders headers = new HttpHeaders();
		response.headerIterator().forEachRemaining(entry -> {
			String name = entry.getKey().toString();
			headers.put(name, response.getAllHeaderValues(name));
		});
		return headers;
	}

}
