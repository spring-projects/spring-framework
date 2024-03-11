/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Reactor-Netty implementation of {@link ClientHttpConnector}.
 *
 * <p>This class implements {@link SmartLifecycle} and can be optionally declared
 * as a Spring-managed bean.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 * @see reactor.netty.http.client.HttpClient
 */
public class ReactorClientHttpConnector implements ClientHttpConnector, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(ReactorClientHttpConnector.class);

	private static final Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);


	private HttpClient httpClient;

	@Nullable
	private final ReactorResourceFactory resourceFactory;

	@Nullable
	private final Function<HttpClient, HttpClient> mapper;

	private volatile boolean running = true;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Default constructor. Initializes {@link HttpClient} via:
	 * <pre class="code">
	 * HttpClient.create().compress()
	 * </pre>
	 */
	public ReactorClientHttpConnector() {
		this.httpClient = defaultInitializer.apply(HttpClient.create());
		this.resourceFactory = null;
		this.mapper = null;
	}

	/**
	 * Constructor with externally managed Reactor Netty resources, including
	 * {@link LoopResources} for event loop threads, and {@link ConnectionProvider}
	 * for the connection pool.
	 * <p>This constructor should be used only when you don't want the client
	 * to participate in the Reactor Netty global resources. By default the
	 * client participates in the Reactor Netty global resources held in
	 * {@link reactor.netty.http.HttpResources}, which is recommended since
	 * fixed, shared resources are favored for event loop concurrency. However,
	 * consider declaring a {@link ReactorResourceFactory} bean with
	 * {@code globalResources=true} in order to ensure the Reactor Netty global
	 * resources are shut down when the Spring ApplicationContext is stopped or closed
	 * and restarted properly when the Spring ApplicationContext is
	 * (with JVM Checkpoint Restore for example).
	 * @param resourceFactory the resource factory to obtain the resources from
	 * @param mapper a mapper for further initialization of the created client
	 * @since 5.1
	 */
	public ReactorClientHttpConnector(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
		this.httpClient = createHttpClient(resourceFactory, mapper);
		this.resourceFactory = resourceFactory;
		this.mapper = mapper;
	}

	private static HttpClient createHttpClient(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
		ConnectionProvider provider = resourceFactory.getConnectionProvider();
		Assert.notNull(provider, "No ConnectionProvider: is ReactorResourceFactory not initialized yet?");
		return defaultInitializer.andThen(mapper).andThen(applyLoopResources(resourceFactory))
				.apply(HttpClient.create(provider));
	}

	private static Function<HttpClient, HttpClient> applyLoopResources(ReactorResourceFactory factory) {
		return httpClient -> {
			LoopResources resources = factory.getLoopResources();
			Assert.notNull(resources, "No LoopResources: is ReactorResourceFactory not initialized yet?");
			return httpClient.runOn(resources);
		};
	}


	/**
	 * Constructor with a pre-configured {@code HttpClient} instance.
	 * @param httpClient the client to use
	 * @since 5.1
	 */
	public ReactorClientHttpConnector(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient is required");
		this.httpClient = httpClient;
		this.resourceFactory = null;
		this.mapper = null;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		AtomicReference<ReactorClientHttpResponse> responseRef = new AtomicReference<>();

		HttpClient.RequestSender requestSender = this.httpClient
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

		requestSender = setUri(requestSender, uri);

		return requestSender
				.send((request, outbound) -> requestCallback.apply(adaptRequest(method, uri, request, outbound)))
				.responseConnection((response, connection) -> {
					responseRef.set(new ReactorClientHttpResponse(response, connection));
					return Mono.just((ClientHttpResponse) responseRef.get());
				})
				.next()
				.doOnCancel(() -> {
					ReactorClientHttpResponse response = responseRef.get();
					if (response != null) {
						response.releaseAfterCancel(method);
					}
				});
	}

	private static HttpClient.RequestSender setUri(HttpClient.RequestSender requestSender, URI uri) {
		if (uri.isAbsolute()) {
			try {
				return requestSender.uri(uri);
			}
			catch (Exception ex) {
				// Fall back on passing it in as a String
			}
		}
		return requestSender.uri(uri.toString());
	}

	private ReactorClientHttpRequest adaptRequest(HttpMethod method, URI uri, HttpClientRequest request,
			NettyOutbound nettyOutbound) {

		return new ReactorClientHttpRequest(method, uri, request, nettyOutbound);
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				if (this.resourceFactory != null && this.mapper != null) {
					this.httpClient = createHttpClient(this.resourceFactory, this.mapper);
				}
				else {
					logger.warn("Restarting a ReactorClientHttpConnector bean is only supported with externally managed Reactor Netty resources");
				}
				this.running = true;
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				this.running = false;
			}
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isAutoStartup() {
		return false;
	}

	@Override
	public int getPhase() {
		// Start after ReactorResourceFactory
		return 1;
	}

}
