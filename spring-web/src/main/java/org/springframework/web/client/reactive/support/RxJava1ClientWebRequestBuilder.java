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

package org.springframework.web.client.reactive.support;

import java.net.URI;

import reactor.adapter.RxJava1Adapter;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.reactive.ClientWebRequest;
import org.springframework.web.client.reactive.ClientWebRequestBuilder;
import org.springframework.web.client.reactive.ClientWebRequestPostProcessor;
import org.springframework.web.client.reactive.DefaultClientWebRequestBuilder;

/**
 * Builds a {@link ClientHttpRequest} using a {@code Observable}
 * or {@code Single} as request body.
 *
 * <p>See static factory methods in {@link RxJava1ClientWebRequestBuilders}
 *
 * @author Brian Clozel
 * @see RxJava1ClientWebRequestBuilders
 */
public class RxJava1ClientWebRequestBuilder implements ClientWebRequestBuilder {

	private final DefaultClientWebRequestBuilder delegate;

	public RxJava1ClientWebRequestBuilder(HttpMethod httpMethod, String urlTemplate,
			Object... urlVariables) throws RestClientException {
		this.delegate = new DefaultClientWebRequestBuilder(httpMethod, urlTemplate, urlVariables);
	}

	public RxJava1ClientWebRequestBuilder(HttpMethod httpMethod, URI url) {
		this.delegate = new DefaultClientWebRequestBuilder(httpMethod, url);
	}

	/**
	 * Add an HTTP request header
	 */
	public RxJava1ClientWebRequestBuilder header(String name, String... values) {
		this.delegate.header(name, values);
		return this;
	}

	/**
	 * Add all provided HTTP request headers
	 */
	public RxJava1ClientWebRequestBuilder headers(HttpHeaders httpHeaders) {
		this.delegate.headers(httpHeaders);
		return this;
	}

	/**
	 * Set the Content-Type request header to the given {@link MediaType}
	 */
	public RxJava1ClientWebRequestBuilder contentType(MediaType contentType) {
		this.delegate.contentType(contentType);
		return this;
	}

	/**
	 * Set the Content-Type request header to the given media type
	 */
	public RxJava1ClientWebRequestBuilder contentType(String contentType) {
		this.delegate.contentType(contentType);
		return this;
	}

	/**
	 * Set the Accept request header to the given {@link MediaType}s
	 */
	public RxJava1ClientWebRequestBuilder accept(MediaType... mediaTypes) {
		this.delegate.accept(mediaTypes);
		return this;
	}

	/**
	 * Set the Accept request header to the given media types
	 */
	public RxJava1ClientWebRequestBuilder accept(String... mediaTypes) {
		this.delegate.accept(mediaTypes);
		return this;
	}

	/**
	 * Add a Cookie to the HTTP request
	 */
	public RxJava1ClientWebRequestBuilder cookie(String name, String value) {
		this.delegate.cookie(name, value);
		return this;
	}

	/**
	 * Add a Cookie to the HTTP request
	 */
	public RxJava1ClientWebRequestBuilder cookie(HttpCookie cookie) {
		this.delegate.cookie(cookie);
		return this;
	}

	/**
	 * Allows performing more complex operations with a strategy. For example, a
	 * {@link ClientWebRequestPostProcessor} implementation might accept the arguments of username
	 * and password and set an HTTP Basic authentication header.
	 *
	 * @param postProcessor the {@link ClientWebRequestPostProcessor} to use. Cannot be null.
	 *
	 * @return this instance for further modifications.
	 */
	public RxJava1ClientWebRequestBuilder apply(ClientWebRequestPostProcessor postProcessor) {
		this.delegate.apply(postProcessor);
		return this;
	}

	/**
	 * Use the given object as the request body
	 */
	public RxJava1ClientWebRequestBuilder body(Object content) {
		this.delegate.body(Mono.just(content), ResolvableType.forInstance(content));
		return this;
	}

	/**
	 * Use the given {@link Single} as the request body and use its {@link ResolvableType}
	 * as type information for the element published by this reactive stream
	 */
	public RxJava1ClientWebRequestBuilder body(Single<?> content, ResolvableType elementType) {
		this.delegate.body(RxJava1Adapter.singleToMono(content), elementType);
		return this;
	}

	/**
	 * Use the given {@link Observable} as the request body and use its {@link ResolvableType}
	 * as type information for the elements published by this reactive stream
	 */
	public RxJava1ClientWebRequestBuilder body(Observable<?> content, ResolvableType elementType) {
		this.delegate.body(RxJava1Adapter.observableToFlux(content), elementType);
		return this;
	}

	@Override
	public ClientWebRequest build() {
		return this.delegate.build();
	}

}
