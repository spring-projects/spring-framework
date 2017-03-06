/*
 * Copyright 2002-2017 the original author or authors.
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
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * {@link ClientHttpRequest} implementation based on Netty 4.
 *
 * <p>Created via the {@link Netty4ClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1.2
 */
class Netty4ClientHttpRequest extends AbstractAsyncClientHttpRequest implements ClientHttpRequest {

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
				throw new IOException(ex.getMessage(), ex.getCause());
			}
		}
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.body;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(final HttpHeaders headers) throws IOException {
		final SettableListenableFuture<ClientHttpResponse> responseFuture = new SettableListenableFuture<>();

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

	private FullHttpRequest createFullHttpRequest(HttpHeaders headers) {
		io.netty.handler.codec.http.HttpMethod nettyMethod =
				io.netty.handler.codec.http.HttpMethod.valueOf(this.method.name());

		String authority = this.uri.getRawAuthority();
		String path = this.uri.toString().substring(this.uri.toString().indexOf(authority) + authority.length());
		FullHttpRequest nettyRequest = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, nettyMethod, path, this.body.buffer());

		nettyRequest.headers().set(HttpHeaders.HOST, this.uri.getHost());
		nettyRequest.headers().set(HttpHeaders.CONNECTION, "close");
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			nettyRequest.headers().add(entry.getKey(), entry.getValue());
		}
		if (!nettyRequest.headers().contains(HttpHeaders.CONTENT_LENGTH) && this.body.buffer().readableBytes() > 0) {
			nettyRequest.headers().set(HttpHeaders.CONTENT_LENGTH, this.body.buffer().readableBytes());
		}

		return nettyRequest;
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


	/**
	 * A SimpleChannelInboundHandler to update the given SettableListenableFuture.
	 */
	private static class RequestExecuteHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

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
