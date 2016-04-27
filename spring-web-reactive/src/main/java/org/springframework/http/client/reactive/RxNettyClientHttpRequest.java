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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import org.reactivestreams.Publisher;
import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.NettyDataBufferAllocator;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequest} implementation for the RxNetty HTTP client
 *
 * @author Brian Clozel
 */
public class RxNettyClientHttpRequest extends AbstractClientHttpRequest {

	private final NettyDataBufferAllocator allocator;

	private final HttpMethod httpMethod;

	private final URI uri;

	private Observable<ByteBuf> body;


	public RxNettyClientHttpRequest(HttpMethod httpMethod, URI uri, HttpHeaders headers, NettyDataBufferAllocator allocator) {
		super(headers);
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.allocator = allocator;
	}

	@Override
	public DataBufferAllocator allocator() {
		return this.allocator;
	}

	/**
	 * Set the body of the message to the given {@link Publisher}.
	 *
	 * <p>Since the HTTP channel is not yet created when this method
	 * is called, the {@code Mono<Void>} return value completes immediately.
	 * For an event that signals that we're done writing the request, check the
	 * {@link #execute()} method.
	 *
	 * @return a publisher that completes immediately.
	 * @see #execute()
	 */
	@Override
	public Mono<Void> setBody(Publisher<DataBuffer> body) {

		this.body = RxJava1ObservableConverter.from(Flux.from(body)
				.map(b -> allocator.wrap(b.asByteBuffer()).getNativeBuffer()));

		return Mono.empty();
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public Mono<ClientHttpResponse> execute() {
		try {
			HttpClientRequest<ByteBuf, ByteBuf> request = HttpClient
					.newClient(this.uri.getHost(), this.uri.getPort())
					.createRequest(io.netty.handler.codec.http.HttpMethod.valueOf(this.httpMethod.name()), uri.getRawPath());

			return applyBeforeCommit()
					.then(() -> Mono.just(request))
					.map(req -> {
						for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
							for (String value : entry.getValue()) {
								req = req.addHeader(entry.getKey(), value);
							}
						}
						for (Map.Entry<String, List<HttpCookie>> entry : getCookies().entrySet()) {
							for (HttpCookie cookie : entry.getValue()) {
								req.addCookie(new DefaultCookie(cookie.getName(), cookie.getValue()));
							}
						}
						return req;
					})
					.map(req -> {
						if (this.body != null) {
							return RxJava1ObservableConverter.from(req.writeContent(this.body));
						}
						else {
							return RxJava1ObservableConverter.from(req);
						}
					})
					.flatMap(resp -> resp)
					.next()
					.map(response -> new RxNettyClientHttpResponse(response, this.allocator));
		}
		catch (IllegalArgumentException exc) {
			return Mono.error(exc);
		}
	}

}
