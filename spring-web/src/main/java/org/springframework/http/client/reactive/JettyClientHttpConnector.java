/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpConnector} for the Jetty Reactive Streams HttpClient.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 */
public class JettyClientHttpConnector implements ClientHttpConnector {

	private final HttpClient httpClient;

	private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	/**
	 * Default constructor that creates a new instance of {@link HttpClient}.
	 */
	public JettyClientHttpConnector() {
		this(new HttpClient());
	}

	/**
	 * Constructor with an initialized {@link HttpClient}.
	 */
	public JettyClientHttpConnector(HttpClient httpClient) {
		this(httpClient, null);
	}

	/**
	 * Constructor with an initialized {@link HttpClient} and configures it
	 * with the given {@link JettyResourceFactory}.
	 * @param httpClient the {@link HttpClient} to use
	 * @param resourceFactory the {@link JettyResourceFactory} to use
	 * @since 5.2
	 */
	public JettyClientHttpConnector(HttpClient httpClient, @Nullable JettyResourceFactory resourceFactory) {
		Assert.notNull(httpClient, "HttpClient is required");
		if (resourceFactory != null) {
			httpClient.setExecutor(resourceFactory.getExecutor());
			httpClient.setByteBufferPool(resourceFactory.getByteBufferPool());
			httpClient.setScheduler(resourceFactory.getScheduler());
		}
		this.httpClient = httpClient;
	}

	/**
	 * Constructor with an {@link JettyResourceFactory} that will manage shared resources.
	 * @param resourceFactory the {@link JettyResourceFactory} to use
	 * @param customizer the lambda used to customize the {@link HttpClient}
	 * @deprecated as of 5.2, in favor of
	 * {@link JettyClientHttpConnector#JettyClientHttpConnector(HttpClient, JettyResourceFactory)}
	 */
	@Deprecated
	public JettyClientHttpConnector(JettyResourceFactory resourceFactory, @Nullable Consumer<HttpClient> customizer) {
		this(new HttpClient(), resourceFactory);
		if (customizer != null) {
			customizer.accept(this.httpClient);
		}
	}


	public void setBufferFactory(DataBufferFactory bufferFactory) {
		this.bufferFactory = bufferFactory;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		if (!uri.isAbsolute()) {
			return Mono.error(new IllegalArgumentException("URI is not absolute: " + uri));
		}

		if (!this.httpClient.isStarted()) {
			try {
				this.httpClient.start();
			}
			catch (Exception ex) {
				return Mono.error(ex);
			}
		}

		Request request = this.httpClient.newRequest(uri).method(method.toString());

		return requestCallback.apply(new JettyClientHttpRequest(request, this.bufferFactory))
				.then(Mono.fromDirect(ReactiveRequest.newBuilder(request).build()
						.response((reactiveResponse, chunkPublisher) -> {
							Flux<DataBuffer> content = Flux.from(chunkPublisher).map(this::toDataBuffer);
							return Mono.just(new JettyClientHttpResponse(reactiveResponse, content));
						})));
	}

	private DataBuffer toDataBuffer(ContentChunk chunk) {

		// We must copy until this is resolved:
		// https://github.com/eclipse/jetty.project/issues/2429

		// Use copy instead of buffer wrapping because Callback#succeeded() is
		// used not only to release the buffer but also to request more data
		// which is a problem for codecs that buffer data.

		DataBuffer buffer = this.bufferFactory.allocateBuffer(chunk.buffer.capacity());
		buffer.write(chunk.buffer);
		chunk.callback.succeeded();
		return buffer;
	}

}
