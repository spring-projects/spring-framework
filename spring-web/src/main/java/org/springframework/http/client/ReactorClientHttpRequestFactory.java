/*
 * Copyright 2002-present the original author or authors.
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
import java.util.concurrent.Executor;
import java.util.function.Function;

import io.netty.channel.ChannelOption;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Reactor-Netty implementation of {@link ClientHttpRequestFactory}.
 *
 * <p>This class implements {@link SmartLifecycle} and can be optionally declared
 * as a Spring-managed bean in order to support JVM Checkpoint Restore.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 6.2
 */
public class ReactorClientHttpRequestFactory implements ClientHttpRequestFactory, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(ReactorClientHttpRequestFactory.class);

	private static final Duration ONE_MILLISECOND = Duration.ofMillis(1);

	private static final Duration MAX_CONNECT_TIMEOUT = Duration.ofMillis(Integer.MAX_VALUE);

	private static final Function<HttpClient, HttpClient> defaultInitializer =
			client -> client.compress(true)
					.responseTimeout(Duration.ofSeconds(10))
					.proxyWithSystemProperties();


	private final @Nullable ReactorResourceFactory resourceFactory;

	private final @Nullable Function<HttpClient, HttpClient> mapper;

	private @Nullable Executor executor;

	private @Nullable Integer connectTimeout;

	private @Nullable Duration readTimeout;

	private volatile @Nullable HttpClient httpClient;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Constructor with default client, created via {@link HttpClient#create()},
	 * and with {@link HttpClient#compress compression} and
	 * {@link HttpClient#proxyWithSystemProperties() proxyWithSystemProperties} enabled.
	 */
	public ReactorClientHttpRequestFactory() {
		this(defaultInitializer.apply(HttpClient.create()));
	}

	/**
	 * Constructor with a given {@link HttpClient} instance.
	 * @param client the client to use
	 */
	public ReactorClientHttpRequestFactory(HttpClient client) {
		Assert.notNull(client, "HttpClient must not be null");
		this.resourceFactory = null;
		this.mapper = null;
		this.httpClient = client;
	}

	/**
	 * Constructor with externally managed Reactor Netty resources, including
	 * {@link LoopResources} for event loop threads, and {@link ConnectionProvider}
	 * for connection pooling.
	 * <p>Generally, it is recommended to share resources for event loop
	 * concurrency. This can be achieved either by participating in the JVM-wide,
	 * global resources held in {@link reactor.netty.http.HttpResources}, or by
	 * using a specific, shared set of resources through a
	 * {@link ReactorResourceFactory} bean. The latter can ensure that resources
	 * are shut down when the Spring ApplicationContext is stopped/closed and
	 * restarted again (e.g. JVM checkpoint restore).
	 * @param resourceFactory the resource factory to get resources from
	 * @param mapper for further initialization of the client
	 */
	public ReactorClientHttpRequestFactory(
			ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {

		this.resourceFactory = resourceFactory;
		this.mapper = mapper;
		if (resourceFactory.isRunning()) {
			this.httpClient = createHttpClient(resourceFactory, mapper);
		}
	}

	private HttpClient createHttpClient(ReactorResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
		HttpClient client = HttpClient.create(factory.getConnectionProvider());
		client = defaultInitializer.andThen(mapper).apply(client);
		client = client.runOn(factory.getLoopResources());
		if (this.connectTimeout != null) {
			client = client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
		}
		if (this.readTimeout != null) {
			client = client.responseTimeout(responseTimeout(this.readTimeout));
		}
		return client;
	}

	/**
	 * Set the {@code Executor} to use for performing blocking I/O operations.
	 * <p>If no executor is provided, the request will use an {@link Schedulers#boundedElastic() elastic scheduler}.
	 * @param executor the executor to use.
	 * @since 6.2.13
	 */
	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "Executor must not be null");
		this.executor = executor;
	}

	/**
	 * Set the connect timeout on the underlying client, in milliseconds.
	 * <p>Effectively, a shortcut for
	 * {@code httpClient.option(CONNECT_TIMEOUT_MILLIS, connectTimeout)}.
	 * <p>A timeout value of 0 specifies an infinite timeout.
	 * <p>Negative values are not permitted.
	 * <p>Default is 30 seconds, as defined by Reactor Netty.
	 * @param connectTimeout the connect timeout in milliseconds
	 * @see #setConnectTimeout(Duration)
	 * @see HttpClient#option(ChannelOption, Object)
	 * @see ChannelOption#CONNECT_TIMEOUT_MILLIS
	 * @see <a href="https://projectreactor.io/docs/netty/release/reference/index.html#connection-timeout">Connection Timeout</a>
	 */
	public void setConnectTimeout(int connectTimeout) {
		Assert.isTrue(connectTimeout >= 0, "Timeout must be a non-negative value");
		this.connectTimeout = connectTimeout;
		HttpClient httpClient = this.httpClient;
		if (httpClient != null) {
			this.httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
		}
	}

	/**
	 * Set the connect timeout on the underlying client, as a {@code Duration}.
	 * <p>Effectively, a shortcut for
	 * {@code httpClient.option(CONNECT_TIMEOUT_MILLIS, connectTimeout.toMillis())}.
	 * <p>A timeout value of 0 specifies an infinite timeout.
	 * <p>Negative values and values less than one millisecond (other than 0)
	 * are not permitted, and values exceeding {@link Integer#MAX_VALUE}
	 * milliseconds are limited to {@code Integer.MAX_VALUE} milliseconds.
	 * <p>Default is 30 seconds, as defined by Reactor Netty.
	 * @param connectTimeout the connect timeout as a {@code Duration}
	 * @see #setConnectTimeout(int)
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		Assert.notNull(connectTimeout, "ConnectTimeout must not be null");
		Assert.isTrue(!connectTimeout.isNegative(), "Timeout must be a non-negative value");
		Assert.isTrue(connectTimeout.isZero() || connectTimeout.compareTo(ONE_MILLISECOND) >= 0,
				"Timeout must be zero or at least one millisecond");
		int millis = (connectTimeout.compareTo(MAX_CONNECT_TIMEOUT) > 0 ?
				Integer.MAX_VALUE : (int) connectTimeout.toMillis());
		setConnectTimeout(millis);
	}

	/**
	 * Set the read timeout on the underlying client, as a {@code Duration}.
	 * <p>Effectively, a shortcut for {@link HttpClient#responseTimeout(Duration)}.
	 * <p>A timeout value of 0 specifies an infinite timeout.
	 * <p>Negative values and values less than one millisecond (other than 0)
	 * are not permitted.
	 * <p>Default is 10 seconds, unless a pre-configured {@link HttpClient} is
	 * supplied.
	 * @param readTimeout the read timeout as a {@code Duration}
	 * @see #setReadTimeout(long)
	 */
	public void setReadTimeout(Duration readTimeout) {
		Assert.notNull(readTimeout, "ReadTimeout must not be null");
		Assert.isTrue(!readTimeout.isNegative(), "Timeout must be a non-negative value");
		Assert.isTrue(readTimeout.isZero() || readTimeout.compareTo(ONE_MILLISECOND) >= 0,
				"Timeout must be zero or at least one millisecond");
		this.readTimeout = readTimeout;
		HttpClient httpClient = this.httpClient;
		if (httpClient != null) {
			this.httpClient = httpClient.responseTimeout(responseTimeout(readTimeout));
		}
	}

	/**
	 * Set the read timeout on the underlying client, in milliseconds.
	 * <p>Effectively, a shortcut for
	 * {@code httpClient.responseTimeout(Duration.ofMillis(readTimeout))}.
	 * <p>A timeout value of 0 specifies an infinite timeout.
	 * <p>Negative values are not permitted.
	 * <p>Default is 10 seconds, unless a pre-configured {@link HttpClient} is
	 * supplied.
	 * @param readTimeout the read timeout in milliseconds
	 * @see #setReadTimeout(Duration)
	 */
	public void setReadTimeout(long readTimeout) {
		setReadTimeout(Duration.ofMillis(readTimeout));
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpClient client = this.httpClient;
		if (client == null) {
			Assert.state(this.resourceFactory != null && this.mapper != null,
					"Expected HttpClient or ResourceFactory and mapper");
			client = createHttpClient(this.resourceFactory, this.mapper);
		}
		return new ReactorClientHttpRequest(client, httpMethod, uri, this.executor);
	}


	@Override
	public void start() {
		if (this.resourceFactory != null && this.mapper != null) {
			synchronized (this.lifecycleMonitor) {
				if (this.httpClient == null) {
					this.httpClient = createHttpClient(this.resourceFactory, this.mapper);
				}
			}
		}
		else {
			logger.warn("Restarting a ReactorClientHttpRequestFactory bean is only supported " +
					"with externally managed Reactor Netty resources");
		}
	}

	@Override
	public void stop() {
		if (this.resourceFactory != null && this.mapper != null) {
			synchronized (this.lifecycleMonitor) {
				this.httpClient = null;
			}
		}
	}

	@Override
	public boolean isRunning() {
		return (this.httpClient != null);
	}

	@Override
	public int getPhase() {
		return 1; // start after ReactorResourceFactory (0)
	}


	/**
	 * Map a read timeout of 0 to {@code null}, which disables the response
	 * timeout in Reactor Netty. Passing {@code Duration.ZERO} through would
	 * instead result in a response timeout of one millisecond.
	 */
	private static @Nullable Duration responseTimeout(Duration readTimeout) {
		return (readTimeout.isZero() ? null : readTimeout);
	}

}
