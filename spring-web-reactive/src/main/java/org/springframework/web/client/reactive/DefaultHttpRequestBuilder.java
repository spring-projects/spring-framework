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
import java.util.Optional;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Builds a {@link ClientHttpRequest}
 *
 * <p>See static factory methods in {@link HttpRequestBuilders}
 *
 * @author Brian Clozel
 * @see HttpRequestBuilders
 */
public class DefaultHttpRequestBuilder implements HttpRequestBuilder {

	private final UriTemplateHandler uriTemplateHandler = new DefaultUriTemplateHandler();

	protected HttpMethod httpMethod;

	protected HttpHeaders httpHeaders;

	protected URI url;

	protected Publisher contentPublisher;

	protected final List<HttpCookie> cookies = new ArrayList<HttpCookie>();

	protected DefaultHttpRequestBuilder() {
	}

	public DefaultHttpRequestBuilder(HttpMethod httpMethod, String urlTemplate, Object... urlVariables) throws RestClientException {
		this.httpMethod = httpMethod;
		this.httpHeaders = new HttpHeaders();
		this.url = this.uriTemplateHandler.expand(urlTemplate, urlVariables);
	}

	public DefaultHttpRequestBuilder(HttpMethod httpMethod, URI url) {
		this.httpMethod = httpMethod;
		this.httpHeaders = new HttpHeaders();
		this.url = url;
	}

	public DefaultHttpRequestBuilder header(String name, String... values) {
		Arrays.stream(values).forEach(value -> this.httpHeaders.add(name, value));
		return this;
	}

	public DefaultHttpRequestBuilder headers(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
		return this;
	}

	public DefaultHttpRequestBuilder contentType(MediaType contentType) {
		this.httpHeaders.setContentType(contentType);
		return this;
	}

	public DefaultHttpRequestBuilder contentType(String contentType) {
		this.httpHeaders.setContentType(MediaType.parseMediaType(contentType));
		return this;
	}

	public DefaultHttpRequestBuilder accept(MediaType... mediaTypes) {
		this.httpHeaders.setAccept(Arrays.asList(mediaTypes));
		return this;
	}

	public DefaultHttpRequestBuilder accept(String... mediaTypes) {
		this.httpHeaders.setAccept(Arrays.stream(mediaTypes)
				.map(type -> MediaType.parseMediaType(type))
				.collect(Collectors.toList()));
		return this;
	}

	public DefaultHttpRequestBuilder content(Object content) {
		this.contentPublisher = Mono.just(content);
		return this;
	}

	public DefaultHttpRequestBuilder contentStream(Publisher<?> content) {
		this.contentPublisher = Flux.from(content);
		return this;
	}

	/**
	 * Allows performing more complex operations with a strategy. For example, a
	 * {@link RequestPostProcessor} implementation might accept the arguments of
	 * username and password and set an HTTP Basic authentication header.
	 *
	 * @param postProcessor the {@link RequestPostProcessor} to use. Cannot be null.
	 *
	 * @return this instance for further modifications.
	 */
	public DefaultHttpRequestBuilder apply(RequestPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "`postProcessor` is required");
		postProcessor.postProcess(this);
		return this;
	}

	public ClientHttpRequest build(ClientHttpRequestFactory factory, List<Encoder<?>> messageEncoders) {
		ClientHttpRequest request = factory.createRequest(this.httpMethod, this.url, this.httpHeaders);
		request.getHeaders().putAll(this.httpHeaders);

		if (this.contentPublisher != null) {
			ResolvableType requestBodyType = ResolvableType.forInstance(this.contentPublisher);
			MediaType mediaType = request.getHeaders().getContentType();

			Optional<Encoder<?>> messageEncoder = messageEncoders
					.stream()
					.filter(e -> e.canEncode(requestBodyType, mediaType))
					.findFirst();

			if (messageEncoder.isPresent()) {
				request.setBody(messageEncoder.get()
						.encode(this.contentPublisher, request.bufferFactory(),
								requestBodyType, mediaType));
			}
			else {
				throw new WebClientException("Can't write request body " +
						"of type '" + requestBodyType.toString() +
						"' for content-type '" + mediaType.toString() + "'");
			}
		}
		return request;
	}

}