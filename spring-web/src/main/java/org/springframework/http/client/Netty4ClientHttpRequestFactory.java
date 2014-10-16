/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that
 * uses <a href="http://netty.io/">Netty 4</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link EventLoopGroup} instance - useful for sharing
 * across multiple clients.
 *
 * @author Arjen Poutsma
 * @since 4.2
 */
public class Netty4ClientHttpRequestFactory
		implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory,
		InitializingBean, DisposableBean {

	/**
	 * The default maximum request size.
	 * @see #setMaxRequestSize(int)
	 */
	public static final int DEFAULT_MAX_REQUEST_SIZE = 1024 * 1024 * 10;

	private final EventLoopGroup eventLoopGroup;

	private final boolean defaultEventLoopGroup;

	private SslContext sslContext;

	private int maxRequestSize = DEFAULT_MAX_REQUEST_SIZE;

	private Bootstrap bootstrap;

	/**
	 * Creates a new {@code Netty4ClientHttpRequestFactory} with a default
	 * {@link NioEventLoopGroup}.
	 */
	public Netty4ClientHttpRequestFactory() {
		int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
		eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
		defaultEventLoopGroup = true;
	}

	/**
	 * Creates a new {@code Netty4ClientHttpRequestFactory} with the given
	 * {@link EventLoopGroup}.
	 *
	 * <p><b>NOTE:</b> the given group will <strong>not</strong> be
	 * {@linkplain EventLoopGroup#shutdownGracefully() shutdown} by this factory; doing
	 * so becomes the responsibility of the caller.
	 */
	public Netty4ClientHttpRequestFactory(EventLoopGroup eventLoopGroup) {
		Assert.notNull(eventLoopGroup, "'eventLoopGroup' must not be null");
		this.eventLoopGroup = eventLoopGroup;
		this.defaultEventLoopGroup = false;
	}

	/**
	 * Sets the default maximum request size. The default is
	 * {@link #DEFAULT_MAX_REQUEST_SIZE}.
	 * @see HttpObjectAggregator#HttpObjectAggregator(int)
	 */
	public void setMaxRequestSize(int maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	/**
	 * Sets the SSL context.
	 */
	public void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	private Bootstrap getBootstrap() {
		if (this.bootstrap == null) {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();

							if (sslContext != null) {
								pipeline.addLast(sslContext.newHandler(ch.alloc()));
							}
							pipeline.addLast(new HttpClientCodec());
							pipeline.addLast(new HttpObjectAggregator(maxRequestSize));
						}
					});
			this.bootstrap = bootstrap;
		}
		return this.bootstrap;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		getBootstrap();
	}

	private Netty4ClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
		return new Netty4ClientHttpRequest(getBootstrap(), uri, httpMethod, maxRequestSize);
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
			throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod)
			throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public void destroy() throws InterruptedException {
		if (defaultEventLoopGroup) {
			// clean up the EventLoopGroup if we created it in the constructor
			eventLoopGroup.shutdownGracefully().sync();
		}
	}


}
