/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation
 * that uses <a href="https://netty.io/">Netty 4</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link EventLoopGroup} instance: useful for
 * sharing across multiple clients.
 *
 * <p>Note that this implementation consistently closes the HTTP connection on each
 * request.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Mark Paluch
 * @since 4.1.2
 * @deprecated as of Spring 5.0, in favor of
 * {@link org.springframework.http.client.reactive.ReactorClientHttpConnector}
 */
@Deprecated
public class Netty4ClientHttpRequestFactory implements ClientHttpRequestFactory,
		AsyncClientHttpRequestFactory, InitializingBean, DisposableBean {

	/**
	 * The default maximum response size.
	 * @see #setMaxResponseSize(int)
	 */
	public static final int DEFAULT_MAX_RESPONSE_SIZE = 1024 * 1024 * 10;


	private final EventLoopGroup eventLoopGroup;

	private final boolean defaultEventLoopGroup;

	private int maxResponseSize = DEFAULT_MAX_RESPONSE_SIZE;

	@Nullable
	private SslContext sslContext;

	private int connectTimeout = -1;

	private int readTimeout = -1;

	@Nullable
	private volatile Bootstrap bootstrap;


	/**
	 * Create a new {@code Netty4ClientHttpRequestFactory} with a default
	 * {@link NioEventLoopGroup}.
	 */
	public Netty4ClientHttpRequestFactory() {
		int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
		this.eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
		this.defaultEventLoopGroup = true;
	}

	/**
	 * Create a new {@code Netty4ClientHttpRequestFactory} with the given
	 * {@link EventLoopGroup}.
	 * <p><b>NOTE:</b> the given group will <strong>not</strong> be
	 * {@linkplain EventLoopGroup#shutdownGracefully() shutdown} by this factory;
	 * doing so becomes the responsibility of the caller.
	 */
	public Netty4ClientHttpRequestFactory(EventLoopGroup eventLoopGroup) {
		Assert.notNull(eventLoopGroup, "EventLoopGroup must not be null");
		this.eventLoopGroup = eventLoopGroup;
		this.defaultEventLoopGroup = false;
	}


	/**
	 * Set the default maximum response size.
	 * <p>By default this is set to {@link #DEFAULT_MAX_RESPONSE_SIZE}.
	 * @see HttpObjectAggregator#HttpObjectAggregator(int)
	 * @since 4.1.5
	 */
	public void setMaxResponseSize(int maxResponseSize) {
		this.maxResponseSize = maxResponseSize;
	}

	/**
	 * Set the SSL context. When configured it is used to create and insert an
	 * {@link io.netty.handler.ssl.SslHandler} in the channel pipeline.
	 * <p>A default client SslContext is configured if none has been provided.
	 */
	public void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * Set the underlying connect timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * @see ChannelConfig#setConnectTimeoutMillis(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Set the underlying URLConnection's read timeout (in milliseconds).
	 * A timeout value of 0 specifies an infinite timeout.
	 * @see ReadTimeoutHandler
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.sslContext == null) {
			this.sslContext = getDefaultClientSslContext();
		}
	}

	private SslContext getDefaultClientSslContext() {
		try {
			return SslContextBuilder.forClient().build();
		}
		catch (SSLException ex) {
			throw new IllegalStateException("Could not create default client SslContext", ex);
		}
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	private Netty4ClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
		return new Netty4ClientHttpRequest(getBootstrap(uri), uri, httpMethod);
	}

	private Bootstrap getBootstrap(URI uri) {
		boolean isSecure = (uri.getPort() == 443 || "https".equalsIgnoreCase(uri.getScheme()));
		if (isSecure) {
			return buildBootstrap(uri, true);
		}
		else {
			Bootstrap bootstrap = this.bootstrap;
			if (bootstrap == null) {
				bootstrap = buildBootstrap(uri, false);
				this.bootstrap = bootstrap;
			}
			return bootstrap;
		}
	}

	private Bootstrap buildBootstrap(URI uri, boolean isSecure) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel channel) throws Exception {
						configureChannel(channel.config());
						ChannelPipeline pipeline = channel.pipeline();
						if (isSecure) {
							Assert.notNull(sslContext, "sslContext should not be null");
							pipeline.addLast(sslContext.newHandler(channel.alloc(), uri.getHost(), uri.getPort()));
						}
						pipeline.addLast(new HttpClientCodec());
						pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
						if (readTimeout > 0) {
							pipeline.addLast(new ReadTimeoutHandler(readTimeout,
									TimeUnit.MILLISECONDS));
						}
					}
				});
		return bootstrap;
	}

	/**
	 * Template method for changing properties on the given {@link SocketChannelConfig}.
	 * <p>The default implementation sets the connect timeout based on the set property.
	 * @param config the channel configuration
	 */
	protected void configureChannel(SocketChannelConfig config) {
		if (this.connectTimeout >= 0) {
			config.setConnectTimeoutMillis(this.connectTimeout);
		}
	}


	@Override
	public void destroy() throws InterruptedException {
		if (this.defaultEventLoopGroup) {
			// Clean up the EventLoopGroup if we created it in the constructor
			this.eventLoopGroup.shutdownGracefully().sync();
		}
	}

}
