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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.http.client.HttpClientResponse;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation for the Reactor-Netty HTTP client.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see reactor.netty.http.client.HttpClient
 */
class ReactorClientHttpResponse implements ClientHttpResponse {

	private static final Log logger = LogFactory.getLog(ReactorClientHttpResponse.class);

	private final HttpClientResponse response;

	private final NettyInbound inbound;

	private final NettyDataBufferFactory bufferFactory;

	// 0 - not subscribed, 1 - subscribed, 2 - cancelled via connector (before subscribe)
	private final AtomicInteger state = new AtomicInteger(0);

	private final String logPrefix;


	/**
	 * Constructor that matches the inputs from
	 * {@link reactor.netty.http.client.HttpClient.ResponseReceiver#responseConnection(BiFunction)}.
	 * @since 5.2.8
	 */
	public ReactorClientHttpResponse(HttpClientResponse response, Connection connection) {
		this.response = response;
		this.inbound = connection.inbound();
		this.bufferFactory = new NettyDataBufferFactory(connection.outbound().alloc());
		this.logPrefix = (logger.isDebugEnabled() ? "[" + connection.channel().id().asShortText() + "] " : "");
	}

	/**
	 * Constructor with inputs extracted from a {@link Connection}.
	 * @deprecated as of 5.2.8, in favor of {@link #ReactorClientHttpResponse(HttpClientResponse, Connection)}
	 */
	@Deprecated
	public ReactorClientHttpResponse(HttpClientResponse response, NettyInbound inbound, ByteBufAllocator alloc) {
		this.response = response;
		this.inbound = inbound;
		this.bufferFactory = new NettyDataBufferFactory(alloc);
		this.logPrefix = "";
	}


	@Override
	public Flux<DataBuffer> getBody() {
		return this.inbound.receive()
				.doOnSubscribe(s -> {
					if (this.state.compareAndSet(0, 1)) {
						return;
					}
					if (this.state.get() == 2) {
						throw new IllegalStateException(
								"The client response body has been released already due to cancellation.");
					}
				})
				.map(byteBuf -> {
					byteBuf.retain();
					return this.bufferFactory.wrap(byteBuf);
				});
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		this.response.responseHeaders().entries().forEach(e -> headers.add(e.getKey(), e.getValue()));
		return headers;
	}

	@Override
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(getRawStatusCode());
	}

	@Override
	public int getRawStatusCode() {
		return this.response.status().code();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		MultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
		this.response.cookies().values().stream().flatMap(Collection::stream)
				.forEach(c ->

					result.add(c.name(), ResponseCookie.fromClientResponse(c.name(), c.value())
							.domain(c.domain())
							.path(c.path())
							.maxAge(c.maxAge())
							.secure(c.isSecure())
							.httpOnly(c.isHttpOnly())
							.build()));
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	/**
	 * Called by {@link ReactorClientHttpConnector} when a cancellation is detected
	 * but the content has not been subscribed to. If the subscription never
	 * materializes then the content will remain not drained. Or it could still
	 * materialize if the cancellation happened very early, or the response
	 * reading was delayed for some reason.
	 */
	void releaseAfterCancel(HttpMethod method) {
		if (mayHaveBody(method) && this.state.compareAndSet(0, 2)) {
			if (logger.isDebugEnabled()) {
				logger.debug(this.logPrefix + "Releasing body, not yet subscribed.");
			}
			this.inbound.receive().doOnNext(byteBuf -> {}).subscribe(byteBuf -> {}, ex -> {});
		}
	}

	private boolean mayHaveBody(HttpMethod method) {
		int code = this.getRawStatusCode();
		return !((code >= 100 && code < 200) || code == 204 || code == 205 ||
				method.equals(HttpMethod.HEAD) || getHeaders().getContentLength() == 0);
	}

	@Override
	public String toString() {
		return "ReactorClientHttpResponse{" +
				"request=[" + this.response.method().name() + " " + this.response.uri() + "]," +
				"status=" + getRawStatusCode() + '}';
	}

}
