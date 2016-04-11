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
import java.util.Map;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.http.client.ClientHttpResponse} implementation
 * that uses Netty 4 to parse responses.
 *
 * @author Arjen Poutsma
 * @since 4.1.2
 */
class Netty4ClientHttpResponse extends AbstractClientHttpResponse {

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
