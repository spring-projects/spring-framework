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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

import io.netty.channel.ChannelOption;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Reactor-Netty implementation of {@link ClientHttpRequestFactory}.
 *
 * <p>This class implements {@link SmartLifecycle} and can be optionally declared
 * as a Spring-managed bean in order to support JVM Checkpoint Restore.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 6.1
 */
public class ReactorNettyClientRequestFactory implements ClientHttpRequestFactory, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(ReactorNettyClientRequestFactory.class);

	private static final Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);


	private HttpClient httpClient;

	@Nullable
	private final ReactorResourceFactory resourceFactory;

	@Nullable
	private final Function<HttpClient, HttpClient> mapper;

	private Duration exchangeTimeout = Duration.ofSeconds(5);

	private Duration readTimeout = Duration.ofSeconds(10);

	private volatile boolean running = true;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Create a new instance of the {@code ReactorNettyClientRequestFactory}
	 * with a default {@link HttpClient} that has compression enabled.
	 */
	public ReactorNettyClientRequestFactory() {
		this.httpClient = defaultInitializer.apply(HttpClient.create());
		this.resourceFactory = null;
		this.mapper = null;
	}

	/**
	 * Create a new instance of the {@code ReactorNettyClientRequestFactory}
	 * based on the given {@link HttpClient}.
	 * @param httpClient the client to base on
	 */
	public ReactorNettyClientRequestFactory(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient must not be null");
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
	 */
	public ReactorNettyClientRequestFactory(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
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
	 * Set the underlying connect timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * <p>Default is 30 seconds.
	 * @see HttpClient#option(ChannelOption, Object)
	 * @see ChannelOption#CONNECT_TIMEOUT_MILLIS
	 */
	public void setConnectTimeout(int connectTimeout) {
		Assert.isTrue(connectTimeout >= 0, "Timeout must be a non-negative value");
		this.httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
	}

	/**
	 * Set the underlying connect timeout in milliseconds.
	 * A value of 0 specifies an infinite timeout.
	 * <p>Default is 30 seconds.
	 * @see HttpClient#option(ChannelOption, Object)
	 * @see ChannelOption#CONNECT_TIMEOUT_MILLIS
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		Assert.notNull(connectTimeout, "ConnectTimeout must not be null");
		Assert.isTrue(!connectTimeout.isNegative(), "Timeout must be a non-negative value");
		this.httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)connectTimeout.toMillis());
	}

	/**
	 * Set the underlying read timeout in milliseconds.
	 * <p>Default is 10 seconds.
	 */
	public void setReadTimeout(long readTimeout) {
		Assert.isTrue(readTimeout > 0, "Timeout must be a positive value");
		this.readTimeout = Duration.ofMillis(readTimeout);
	}

	/**
	 * Set the underlying read timeout as {@code Duration}.
	 * <p>Default is 10 seconds.
	 */
	public void setReadTimeout(Duration readTimeout) {
		Assert.notNull(readTimeout, "ReadTimeout must not be null");
		Assert.isTrue(!readTimeout.isNegative(), "Timeout must be a non-negative value");
		this.readTimeout = readTimeout;
	}

	/**
	 * Set the timeout for the HTTP exchange in milliseconds.
	 * <p>Default is 30 seconds.
	 */
	public void setExchangeTimeout(long exchangeTimeout) {
		Assert.isTrue(exchangeTimeout > 0, "Timeout must be a positive value");
		this.exchangeTimeout = Duration.ofMillis(exchangeTimeout);
	}

	/**
	 * Set the timeout for the HTTP exchange.
	 * <p>Default is 30 seconds.
	 */
	public void setExchangeTimeout(Duration exchangeTimeout) {
		Assert.notNull(exchangeTimeout, "ExchangeTimeout must not be null");
		Assert.isTrue(!exchangeTimeout.isNegative(), "Timeout must be a non-negative value");
		this.exchangeTimeout = exchangeTimeout;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return new ReactorNettyClientRequest(this.httpClient, uri, httpMethod, this.exchangeTimeout, this.readTimeout);
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				if (this.resourceFactory != null && this.mapper != null) {
					this.httpClient = createHttpClient(this.resourceFactory, this.mapper);
				}
				else {
					logger.warn("Restarting a ReactorNettyClientRequestFactory bean is only supported with externally managed Reactor Netty resources");
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
