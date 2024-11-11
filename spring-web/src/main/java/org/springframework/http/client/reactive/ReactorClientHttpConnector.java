/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.netty.util.AttributeKey;
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
 * @author Juergen Hoeller
 * @since 5.0
 * @see reactor.netty.http.client.HttpClient
 */
public class ReactorClientHttpConnector implements ClientHttpConnector, SmartLifecycle {

	/**
	 * Channel attribute key under which {@code WebClient} request attributes are stored as a Map.
	 * @since 6.2
	 */
	public static final AttributeKey<Map<String, Object>> ATTRIBUTES_KEY =
			AttributeKey.valueOf(ReactorClientHttpRequest.class.getName() + ".ATTRIBUTES");

	private static final Log logger = LogFactory.getLog(ReactorClientHttpConnector.class);

	private static final Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);


	@Nullable
	private final ReactorResourceFactory resourceFactory;

	@Nullable
	private final Function<HttpClient, HttpClient> mapper;

	@Nullable
	private volatile HttpClient httpClient;

	private boolean lazyStart = false;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Default constructor. Initializes {@link HttpClient} via:
	 * <pre class="code">HttpClient.create().compress(true)</pre>
	 */
	public ReactorClientHttpConnector() {
		this.httpClient = defaultInitializer.apply(HttpClient.create());
		this.resourceFactory = null;
		this.mapper = null;
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
		this.resourceFactory = resourceFactory;
		this.mapper = mapper;
		if (resourceFactory.isRunning()) {
			this.httpClient = createHttpClient(resourceFactory, mapper);
		}
		else {
			this.lazyStart = true;
		}
	}

	private static HttpClient createHttpClient(ReactorResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
		return defaultInitializer.andThen(mapper).andThen(httpClient -> httpClient.runOn(factory.getLoopResources()))
				.apply(HttpClient.create(factory.getConnectionProvider()));
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		HttpClient httpClient = this.httpClient;
		if (httpClient == null) {
			Assert.state(this.resourceFactory != null && this.mapper != null, "Illegal configuration");
			if (this.resourceFactory.isRunning()) {
				// Retain HttpClient instance if resource factory has been started in the meantime,
				// considering this connector instance as lazily started as well.
				synchronized (this.lifecycleMonitor) {
					httpClient = this.httpClient;
					if (httpClient == null && this.lazyStart) {
						httpClient = createHttpClient(this.resourceFactory, this.mapper);
						this.httpClient = httpClient;
						this.lazyStart = false;
					}
				}
			}
			if (httpClient == null) {
				httpClient = createHttpClient(this.resourceFactory, this.mapper);
			}
		}

		HttpClient.RequestSender requestSender = httpClient
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

		requestSender = setUri(requestSender, uri);
		AtomicReference<ReactorClientHttpResponse> responseRef = new AtomicReference<>();

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
		if (this.resourceFactory != null && this.mapper != null) {
			synchronized (this.lifecycleMonitor) {
				if (this.httpClient == null) {
					this.httpClient = createHttpClient(this.resourceFactory, this.mapper);
					this.lazyStart = false;
				}
			}
		}
		else {
			logger.warn("Restarting a ReactorClientHttpConnector bean is only supported " +
					"with externally managed Reactor Netty resources");
		}
	}

	@Override
	public void stop() {
		if (this.resourceFactory != null && this.mapper != null) {
			synchronized (this.lifecycleMonitor) {
				this.httpClient = null;
				this.lazyStart = false;
			}
		}
	}

	@Override
	public boolean isRunning() {
		return (this.httpClient != null);
	}

	@Override
	public int getPhase() {
		// Start after ReactorResourceFactory
		return 1;
	}

}
