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

package org.springframework.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import reactor.core.publisher.Flux;
import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Adapt {@link ServerHttpRequest} to the RxNetty {@link HttpServerRequest}.
 * For internal use within the framework.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.0
 */
public class RxNettyServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServerRequest<ByteBuf> request;

	private final NettyDataBufferFactory dataBufferFactory;

	private final InetSocketAddress remoteAddress;


	public RxNettyServerHttpRequest(HttpServerRequest<ByteBuf> request,
			NettyDataBufferFactory dataBufferFactory, InetSocketAddress remoteAddress)
			throws URISyntaxException {

		super(initUri(request, remoteAddress), "", initHeaders(request));
		this.request = request;

		Assert.notNull(dataBufferFactory, "NettyDataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;

		this.remoteAddress = remoteAddress;
	}

	private static URI initUri(HttpServerRequest<ByteBuf> request, InetSocketAddress remoteAddress)
			throws URISyntaxException {

		Assert.notNull(request, "HttpServerRequest must not be null");
		String requestUri = request.getUri();
		return (remoteAddress != null ? createUrl(remoteAddress, requestUri) : URI.create(requestUri));
	}

	private static URI createUrl(InetSocketAddress address, String requestUri) throws URISyntaxException {
		URI baseUrl = new URI(null, null, address.getHostString(), address.getPort(), null, null, null);
		return new URI(baseUrl.toString() + requestUri);
	}

	private static HttpHeaders initHeaders(HttpServerRequest<ByteBuf> request) {
		HttpHeaders headers = new HttpHeaders();
		for (String name : request.getHeaderNames()) {
			headers.put(name, request.getAllHeaderValues(name));
		}
		return headers;
	}


	public HttpServerRequest<ByteBuf> getRxNettyRequest() {
		return this.request;
	}

	@Override
	public String getMethodValue() {
		return this.request.getHttpMethod().name();
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		for (String name : this.request.getCookies().keySet()) {
			for (Cookie cookie : this.request.getCookies().get(name)) {
				HttpCookie httpCookie = new HttpCookie(name, cookie.value());
				cookies.add(name, httpCookie);
			}
		}
		return cookies;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		Observable<DataBuffer> content = this.request.getContent().map(dataBufferFactory::wrap);
		return Flux.from(RxReactiveStreams.toPublisher(content));
	}

}
