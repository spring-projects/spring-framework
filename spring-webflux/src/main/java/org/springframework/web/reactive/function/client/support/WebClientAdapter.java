/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.function.client.support;

import java.net.URI;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.AbstractReactorHttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.ReactiveHttpRequestValues;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapter;
import org.springframework.web.util.UriBuilderFactory;

/**
 * {@link ReactorHttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link WebClient} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@code HttpServiceProxyFactory} configured with a given {@code WebClient}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public final class WebClientAdapter extends AbstractReactorHttpExchangeAdapter {

	private final WebClient webClient;


	/**
	 * Package private constructor. See static factory methods.
	 */
	private WebClientAdapter(WebClient webClient) {
		this.webClient = webClient;
	}


	@Override
	public boolean supportsRequestAttributes() {
		return true;
	}

	@Override
	public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
		return newRequest(requestValues).retrieve().toBodilessEntity().then();
	}

	@Override
	public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
		return newRequest(requestValues).retrieve().toBodilessEntity().map(ResponseEntity::getHeaders);
	}

	@Override
	public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return newRequest(requestValues).retrieve().bodyToMono(bodyType);
	}

	@Override
	public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return newRequest(requestValues).retrieve().bodyToFlux(bodyType);
	}

	@Override
	public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
		return newRequest(requestValues).retrieve().toBodilessEntity();
	}

	@Override
	public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return newRequest(requestValues).retrieve().toEntity(bodyType);
	}

	@Override
	public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return newRequest(requestValues).retrieve().toEntityFlux(bodyType);
	}

	/**
	 * Build a request from the given {@code HttpRequestValues}.
	 * @param values the values to use
	 * @return the request spec
	 * @since 7.0.6
	 */
	public WebClient.RequestBodySpec newRequest(HttpRequestValues values) {
		HttpMethod httpMethod = values.getHttpMethod();
		Assert.notNull(httpMethod, "HttpMethod is required");
		WebClient.RequestBodyUriSpec uriSpec = this.webClient.method(httpMethod);
		WebClient.RequestBodySpec bodySpec = setUri(uriSpec, values);
		bodySpec.headers(headers -> headers.putAll(values.getHeaders()));
		bodySpec.cookies(cookies -> cookies.putAll(values.getCookies()));
		bodySpec.apiVersion(values.getApiVersion());
		bodySpec.attributes(attributes -> attributes.putAll(values.getAttributes()));
		setBody(bodySpec, values);
		return bodySpec;
	}

	private static WebClient.RequestBodySpec setUri(
			WebClient.RequestBodyUriSpec spec, HttpRequestValues values) {

		if (values.getUri() != null) {
			return spec.uri(values.getUri());
		}

		if (values.getUriTemplate() != null) {
			UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
			if(uriBuilderFactory != null){
				URI uri = uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables());
				return spec.uri(uri);
			}
			else {
				return spec.uri(values.getUriTemplate(), values.getUriVariables());
			}
		}

		throw new IllegalStateException("Neither full URL nor URI template");
	}

	@SuppressWarnings({"ReactiveStreamsUnusedPublisher", "unchecked"})
	private <B> void setBody(WebClient.RequestBodySpec spec, HttpRequestValues values) {
		if (values.getBodyValue() != null) {
			if (values.getBodyValueType() != null) {
				B body = (B) values.getBodyValue();
				spec.bodyValue(body, (ParameterizedTypeReference<B>) values.getBodyValueType());
			}
			else {
				spec.bodyValue(values.getBodyValue());
			}
		}
		else if (values instanceof ReactiveHttpRequestValues rhrv) {
			Publisher<?> body = rhrv.getBodyPublisher();
			if (body != null) {
				ParameterizedTypeReference<?> elementType = rhrv.getBodyPublisherElementType();
				Assert.notNull(elementType, "Publisher body element type is required");
				spec.body(body, elementType);
			}
		}
	}


	/**
	 * Create a {@link WebClientAdapter} for the given {@code WebClient} instance.
	 * @param webClient the client to use
	 * @return the created adapter instance
	 * @since 6.1
	 */
	public static WebClientAdapter create(WebClient webClient) {
		return new WebClientAdapter(webClient);
	}

}
