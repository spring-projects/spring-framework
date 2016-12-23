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

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.WebSocketExtension;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.WebSocketChannel;

import org.xnio.IoFuture;
import org.xnio.IoFuture.Notifier;
import org.xnio.IoFuture.Status;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketHandlerAdapter;

/**
 * An Undertow based implementation of {@link WebSocketClient}.
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
public class UndertowWebSocketClient extends WebSocketClientSupport implements WebSocketClient {

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static XnioWorker worker;

	static {
		try {
			worker = Xnio.getInstance().createWorker(OptionMap.builder()
					.set(Options.WORKER_IO_THREADS, 2)
					.set(Options.CONNECTION_HIGH_WATER, 1000000)
					.set(Options.CONNECTION_LOW_WATER, 1000000)
					.set(Options.WORKER_TASK_CORE_THREADS, 30)
					.set(Options.WORKER_TASK_MAX_THREADS, 30)
					.set(Options.TCP_NODELAY, true)
					.set(Options.CORK, true)
					.getMap());
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private final Function<URI, io.undertow.websockets.client.WebSocketClient.ConnectionBuilder> builder;


	/**
	 * Default constructor that uses
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder(XnioWorker, ByteBufferPool, URI)}
	 * to create a web socket connection.
	 */
	public UndertowWebSocketClient() {
		this(UndertowWebSocketClient::createDefaultConnectionBuilder);
	}

	/**
	 * Constructor that accepts an existing
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder(XnioWorker, ByteBufferPool, URI)}
	 * instance.
	 * @param builder a connection builder that can be used to create a web socket connection.
	 */
	public UndertowWebSocketClient(Function<URI,
			io.undertow.websockets.client.WebSocketClient.ConnectionBuilder> builder) {
		this.builder = builder;
	}

	private static io.undertow.websockets.client.WebSocketClient.ConnectionBuilder createDefaultConnectionBuilder(
			URI url) {

		io.undertow.websockets.client.WebSocketClient.ConnectionBuilder builder =
				io.undertow.websockets.client.WebSocketClient.connectionBuilder(
				worker, new DefaultByteBufferPool(false, DEFAULT_BUFFER_SIZE), url);

		boolean secure = "wss".equals(url.getScheme());
		if (secure) {
			try {
				UndertowXnioSsl ssl = new UndertowXnioSsl(Xnio.getInstance(),
						OptionMap.EMPTY, SSLContext.getDefault());
				builder.setSsl(ssl);
			}
			catch (NoSuchAlgorithmException ex) {
				throw new RuntimeException("Failed to create Undertow ConnectionBuilder for " + url, ex);
			}
		}

		return builder;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return connectInternal(url, headers, handler);
	}

	private Mono<Void> connectInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		MonoProcessor<Void> processor = MonoProcessor.create();
		return Mono.fromCallable(
				() -> {
					WSClientNegotiation clientNegotiation =
							new WSClientNegotiation(beforeHandshake(url, headers, handler),
									Collections.emptyList(), headers);

					io.undertow.websockets.client.WebSocketClient.ConnectionBuilder builder =
							this.builder.apply(url).setClientNegotiation(clientNegotiation);

					IoFuture<WebSocketChannel> future = builder.connect();
					future.addNotifier(new ResultNotifier(url, handler, clientNegotiation, processor), new Object());
					return future;
				})
				.then(processor);
	}


	private static final class ResultNotifier implements Notifier<WebSocketChannel, Object> {

		private final URI url;

		private final WebSocketHandler handler;

		private final WSClientNegotiation clientNegotiation;

		private final MonoProcessor<Void> processor;

		public ResultNotifier(URI url, WebSocketHandler handler,
				WSClientNegotiation clientNegotiation, MonoProcessor<Void> processor) {
			this.url = url;
			this.handler = handler;
			this.clientNegotiation = clientNegotiation;
			this.processor = processor;
		}

		@Override
		public void notify(IoFuture<? extends WebSocketChannel> ioFuture,
				Object attachment) {
			if (Status.CANCELLED.equals(ioFuture.getStatus())) {
				processor.onError(null);
			}
			else if (Status.FAILED.equals(ioFuture.getStatus())) {
				processor.onError(ioFuture.getException());
			}
			else if (Status.DONE.equals(ioFuture.getStatus())) {
				try {
					WebSocketChannel channel = ioFuture.get();
					DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
					HandshakeInfo info = new HandshakeInfo(url, clientNegotiation.getResponseHeaders(),
							Mono.empty(), Optional.ofNullable(channel.getSubProtocol()));

					UndertowWebSocketHandlerAdapter adapter =
							new UndertowWebSocketHandlerAdapter(handler,
									info, bufferFactory, processor);
					adapter.onConnect(null, channel);
				}
				catch (CancellationException | IOException ex) {
					processor.onError(ex);
				}
			}
		}
	}


	private static final class WSClientNegotiation extends WebSocketClientNegotiation {

		private final HttpHeaders requestHeaders;

		private HttpHeaders responseHeaders = new HttpHeaders();

		public WSClientNegotiation(String[] subProtocols,
				List<WebSocketExtension> extensions, HttpHeaders requestHeaders) {
			super(Arrays.asList(subProtocols), extensions);
			this.requestHeaders = requestHeaders;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			requestHeaders.forEach((k, v) -> headers.put(k, v));
		}

		@Override
		public void afterRequest(Map<String, List<String>> headers) {
			headers.forEach((k, v) -> responseHeaders.put(k, v));
		}

		public HttpHeaders getResponseHeaders() {
			return responseHeaders;
		}
	}

}
