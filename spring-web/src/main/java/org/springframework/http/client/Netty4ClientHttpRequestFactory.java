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

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory} implementation that
 * uses <a href="http://netty.io/">Netty 4</a> to create requests.
 *
 * <p>Allows to use a pre-configured {@link EventLoopGroup} instance - useful for sharing
 * across multiple clients.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.1.2
 */
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

	private SslContext sslContext;

	private int connectTimeout = -1;

	private int readTimeout = -1;

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
	 * <p>By default this is not set.
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

	private Bootstrap getBootstrap() {
		if (this.bootstrap == null) {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel channel) throws Exception {
							configureChannel(channel.config());
							ChannelPipeline pipeline = channel.pipeline();
							if (sslContext != null) {
								pipeline.addLast(sslContext.newHandler(channel.alloc()));
							}
							pipeline.addLast(new HttpClientCodec());
							pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
							if (readTimeout > 0) {
								pipeline.addLast(new ReadTimeoutHandler(readTimeout,
										TimeUnit.MILLISECONDS));
							}
						}
					});
			this.bootstrap = bootstrap;
		}
		return this.bootstrap;
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
	public void afterPropertiesSet() {
		getBootstrap();
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
		return new Netty4ClientHttpRequest(getBootstrap(), uri, httpMethod);
	}


	@Override
	public void destroy() throws InterruptedException {
		if (this.defaultEventLoopGroup) {
			// Clean up the EventLoopGroup if we created it in the constructor
			this.eventLoopGroup.shutdownGracefully().sync();
		}
	}

	private static class Netty4ClientHttpRequest extends AbstractAsyncClientHttpRequest implements ClientHttpRequest {

		private final Bootstrap bootstrap;

		private final URI uri;

		private final HttpMethod method;

		private final ByteBufOutputStream body;


		public Netty4ClientHttpRequest(Bootstrap bootstrap, URI uri, HttpMethod method) {
			this.bootstrap = bootstrap;
			this.uri = uri;
			this.method = method;
			this.body = new ByteBufOutputStream(Unpooled.buffer(1024));
		}


		@Override
		public HttpMethod getMethod() {
			return this.method;
		}

		@Override
		public URI getURI() {
			return this.uri;
		}

		@Override
		protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
			return this.body;
		}

		@Override
		protected ListenableFuture<ClientHttpResponse> executeInternal(final HttpHeaders headers) throws IOException {
			final SettableListenableFuture<ClientHttpResponse> responseFuture =
					new SettableListenableFuture<ClientHttpResponse>();

			ChannelFutureListener connectionListener = new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						Channel channel = future.channel();
						channel.pipeline().addLast(new RequestExecuteHandler(responseFuture));
						FullHttpRequest nettyRequest = createFullHttpRequest(headers);
						channel.writeAndFlush(nettyRequest);
					}
					else {
						responseFuture.setException(future.cause());
					}
				}
			};

			this.bootstrap.connect(this.uri.getHost(), getPort(this.uri)).addListener(connectionListener);

			return responseFuture;
		}

		@Override
		public ClientHttpResponse execute() throws IOException {
			try {
				return executeAsync().get();
			}
			catch (InterruptedException ex) {
				throw new IOException(ex.getMessage(), ex);
			}
			catch (ExecutionException ex) {
				if (ex.getCause() instanceof IOException) {
					throw (IOException) ex.getCause();
				}
				else {
					throw new IOException(ex.getMessage(), ex);
				}
			}
		}

		private static int getPort(URI uri) {
			int port = uri.getPort();
			if (port == -1) {
				if ("http".equalsIgnoreCase(uri.getScheme())) {
					port = 80;
				}
				else if ("https".equalsIgnoreCase(uri.getScheme())) {
					port = 443;
				}
			}
			return port;
		}

		private FullHttpRequest createFullHttpRequest(HttpHeaders headers) {
			io.netty.handler.codec.http.HttpMethod nettyMethod =
					io.netty.handler.codec.http.HttpMethod.valueOf(this.method.name());

			FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
					nettyMethod, this.uri.toString(), this.body.buffer());

			nettyRequest.headers().set(HttpHeaders.HOST, uri.getHost());
			nettyRequest.headers().set(HttpHeaders.CONNECTION, io.netty.handler.codec.http.HttpHeaders.Values.CLOSE);

			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				nettyRequest.headers().add(entry.getKey(), entry.getValue());
			}

			return nettyRequest;
		}


		/**
		 * A SimpleChannelInboundHandler to update the given SettableListenableFuture.
		 */
		private static class RequestExecuteHandler extends
				SimpleChannelInboundHandler<FullHttpResponse> {

			private final SettableListenableFuture<ClientHttpResponse> responseFuture;

			public RequestExecuteHandler(SettableListenableFuture<ClientHttpResponse> responseFuture) {
				this.responseFuture = responseFuture;
			}

			@Override
			protected void channelRead0(ChannelHandlerContext context, FullHttpResponse response) throws Exception {
				this.responseFuture.set(new Netty4ClientHttpResponse(context, response));
			}

			@Override
			public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
				this.responseFuture.setException(cause);
			}
		}

	}

	private static class Netty4ClientHttpResponse extends AbstractClientHttpResponse {

		private final ChannelHandlerContext context;

		private final FullHttpResponse nettyResponse;

		private final ByteBufInputStream body;

		private volatile HttpHeaders headers;


		public Netty4ClientHttpResponse(ChannelHandlerContext context, FullHttpResponse nettyResponse) {
			Assert.notNull(context, "ChannelHandlerContext must not be null");
			Assert.notNull(nettyResponse, "FullHttpResponse must not be null");
			this.context = context;
			this.nettyResponse = nettyResponse;
			this.body = new ByteBufInputStream(this.nettyResponse.content());
			this.nettyResponse.retain();
		}


		@Override
		public int getRawStatusCode() throws IOException {
			return this.nettyResponse.getStatus().code();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.nettyResponse.getStatus().reasonPhrase();
		}

		@Override
		public HttpHeaders getHeaders() {
			if (this.headers == null) {
				HttpHeaders headers = new HttpHeaders();
				for (Map.Entry<String, String> entry : this.nettyResponse.headers()) {
					headers.add(entry.getKey(), entry.getValue());
				}
				this.headers = headers;
			}
			return this.headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			return this.body;
		}

		@Override
		public void close() {
			this.nettyResponse.release();
			this.context.close();
		}

	}
}
