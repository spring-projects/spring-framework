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
package org.springframework.mock.http.server.reactive.test;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Mock implementation of {@link ServerHttpRequest}.
 * @author Rossen Stoyanchev
 */
public class MockServerHttpRequest implements ServerHttpRequest {

	private HttpMethod httpMethod;

	private URI url;

	private String contextPath = "";

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

	private Flux<DataBuffer> body = Flux.empty();


	/**
	 * Create a new instance where the HTTP method and/or URL can be set later
	 * via {@link #setHttpMethod(HttpMethod)} and {@link #setUri(URI)}.
	 */
	public MockServerHttpRequest() {
	}

	/**
	 * Convenience alternative to {@link #MockServerHttpRequest(HttpMethod, URI)}
	 * that accepts a String URL.
	 */
	public MockServerHttpRequest(HttpMethod httpMethod, String url) {
		this(httpMethod, (url != null ? URI.create(url) : null));
	}

	/**
	 * Create a new instance with the given HTTP method and URL.
	 */
	public MockServerHttpRequest(HttpMethod httpMethod, URI url) {
		this.httpMethod = httpMethod;
		this.url = url;
	}


	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	public MockServerHttpRequest setUri(String url) {
		this.url = URI.create(url);
		return this;
	}

	public MockServerHttpRequest setUri(URI uri) {
		this.url = uri;
		return this;
	}

	@Override
	public URI getURI() {
		return this.url;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	public MockServerHttpRequest addHeader(String name, String value) {
		getHeaders().add(name, value);
		return this;
	}

	public MockServerHttpRequest setHeader(String name, String value) {
		getHeaders().set(name, value);
		return this;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return this.queryParams;
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		return this.cookies;
	}

	public MockServerHttpRequest setBody(Publisher<DataBuffer> body) {
		this.body = Flux.from(body);
		return this;
	}

	public MockServerHttpRequest setBody(String body) {
		DataBuffer buffer = toDataBuffer(body, StandardCharsets.UTF_8);
		this.body = Flux.just(buffer);
		return this;
	}

	public MockServerHttpRequest setBody(String body, Charset charset) {
		DataBuffer buffer = toDataBuffer(body, charset);
		this.body = Flux.just(buffer);
		return this;
	}

	private DataBuffer toDataBuffer(String body, Charset charset) {
		byte[] bytes = body.getBytes(charset);
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		return new DefaultDataBufferFactory().wrap(byteBuffer);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

}
