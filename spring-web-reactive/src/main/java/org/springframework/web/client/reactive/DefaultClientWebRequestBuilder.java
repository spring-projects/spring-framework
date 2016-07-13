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

package org.springframework.web.client.reactive;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Builds a {@link ClientHttpRequest} using a {@link Publisher}
 * as request body.
 *
 * <p>See static factory methods in {@link ClientWebRequestBuilders}
 *
 * @author Brian Clozel
 * @see ClientWebRequestBuilders
 */
public class DefaultClientWebRequestBuilder implements ClientWebRequestBuilder {


	private final UriTemplateHandler uriTemplateHandler = new DefaultUriTemplateHandler();

	private HttpMethod httpMethod;

	private HttpHeaders httpHeaders;

	private URI url;

	private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

	private Publisher<?> body;

	private ResolvableType elementType;

	private List<ClientWebRequestPostProcessor> postProcessors = new ArrayList<>();

	protected DefaultClientWebRequestBuilder() {
	}

	public DefaultClientWebRequestBuilder(HttpMethod httpMethod, String urlTemplate,
			Object... urlVariables) {
		this.httpMethod = httpMethod;
		this.httpHeaders = new HttpHeaders();
		this.url = this.uriTemplateHandler.expand(urlTemplate, urlVariables);
	}

	public DefaultClientWebRequestBuilder(HttpMethod httpMethod, URI url) {
		this.httpMethod = httpMethod;
		this.httpHeaders = new HttpHeaders();
		this.url = url;
	}

	/**
	 * Add an HTTP request header
	 */
	public DefaultClientWebRequestBuilder header(String name, String... values) {
		Arrays.stream(values).forEach(value -> this.httpHeaders.add(name, value));
		return this;
	}

	/**
	 * Add all provided HTTP request headers
	 */
	public DefaultClientWebRequestBuilder headers(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
		return this;
	}

	/**
	 * Set the Content-Type request header to the given {@link MediaType}
	 */
	public DefaultClientWebRequestBuilder contentType(MediaType contentType) {
		this.httpHeaders.setContentType(contentType);
		return this;
	}

	/**
	 * Set the Content-Type request header to the given media type
	 */
	public DefaultClientWebRequestBuilder contentType(String contentType) {
		this.httpHeaders.setContentType(MediaType.parseMediaType(contentType));
		return this;
	}

	/**
	 * Set the Accept request header to the given {@link MediaType}s
	 */
	public DefaultClientWebRequestBuilder accept(MediaType... mediaTypes) {
		this.httpHeaders.setAccept(Arrays.asList(mediaTypes));
		return this;
	}

	/**
	 * Set the Accept request header to the given media types
	 */
	public DefaultClientWebRequestBuilder accept(String... mediaTypes) {
		this.httpHeaders.setAccept(
				Arrays.stream(mediaTypes).map(type -> MediaType.parseMediaType(type))
						.collect(Collectors.toList()));
		return this;
	}

	/**
	 * Add a Cookie to the HTTP request
	 */
	public DefaultClientWebRequestBuilder cookie(String name, String value) {
		return cookie(new HttpCookie(name, value));
	}

	/**
	 * Add a Cookie to the HTTP request
	 */
	public DefaultClientWebRequestBuilder cookie(HttpCookie cookie) {
		this.cookies.add(cookie.getName(), cookie);
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
	public DefaultClientWebRequestBuilder apply(ClientWebRequestPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "`postProcessor` is required");
		this.postProcessors.add(postProcessor);
		return this;
	}

	/**
	 * Use the given object as the request body
	 */
	public DefaultClientWebRequestBuilder body(Object content) {
		this.body = Mono.just(content);
		this.elementType = ResolvableType.forInstance(content);
		return this;
	}

	/**
	 * Use the given {@link Publisher} as the request body and use its {@link ResolvableType}
	 * as type information for the element published by this reactive stream
	 */
	public DefaultClientWebRequestBuilder body(Publisher<?> content, ResolvableType publisherType) {
		this.body = content;
		this.elementType = publisherType;
		return this;
	}

	@Override
	public ClientWebRequest build() {
		ClientWebRequest clientWebRequest = new ClientWebRequest(this.httpMethod, this.url);
		clientWebRequest.setHttpHeaders(this.httpHeaders);
		clientWebRequest.setCookies(this.cookies);
		clientWebRequest.setBody(this.body);
		clientWebRequest.setElementType(this.elementType);
		for (ClientWebRequestPostProcessor postProcessor : this.postProcessors) {
			clientWebRequest = postProcessor.postProcess(clientWebRequest);
		}
		return clientWebRequest;
	}

}