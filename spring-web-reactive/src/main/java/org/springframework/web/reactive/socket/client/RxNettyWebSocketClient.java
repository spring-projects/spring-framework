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
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import io.reactivex.netty.protocol.http.ws.client.WebSocketRequest;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.RxNettyWebSocketSession;

/**
 * A {@link WebSocketClient} based on RxNetty.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RxNettyWebSocketClient implements WebSocketClient {

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
		HandshakeInfo info = new HandshakeInfo(url, headers, Mono.empty());
		Observable<Void> completion = connectInternal(handler, info);
		return Mono.from(RxReactiveStreams.toPublisher(completion));
	}

	private Observable<Void> connectInternal(WebSocketHandler handler, HandshakeInfo info) {
		return createWebSocketRequest(info.getUri())
				.flatMap(response -> {
					ByteBufAllocator allocator = response.unsafeNettyChannel().alloc();
					NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(allocator);
					Observable<WebSocketConnection> conn = response.getWebSocketConnection();
					return Observable.zip(conn, Observable.just(bufferFactory), Tuples::of);
				})
				.flatMap(tuple -> {
					WebSocketConnection conn = tuple.getT1();
					NettyDataBufferFactory bufferFactory = tuple.getT2();
					WebSocketSession session = new RxNettyWebSocketSession(conn, info, bufferFactory);
					return RxReactiveStreams.toObservable(handler.handle(session));
				});
	}

	private WebSocketRequest<ByteBuf> createWebSocketRequest(URI url) {
		String query = url.getRawQuery();
		return this.httpClientFactory.apply(url)
				.createGet(url.getRawPath() + (query != null ? "?" + query : ""))
				.requestWebSocketUpgrade();
	}

}
