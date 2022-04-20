/*
 * Copyright 2002-2022 the original author or authors.
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


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpClientAdapter;
import org.springframework.web.service.invoker.HttpRequestDefinition;


/**
 * {@link HttpClientAdapter} implementation for {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class WebClientAdapter implements HttpClientAdapter {

	private final WebClient webClient;


	public WebClientAdapter(WebClient webClient) {
		this.webClient = webClient;
	}


	@Override
	public Mono<Void> requestToVoid(HttpRequestDefinition request) {
		return toBodySpec(request).exchangeToMono(ClientResponse::releaseBody);
	}

	@Override
	public Mono<HttpHeaders> requestToHeaders(HttpRequestDefinition request) {
		return toBodySpec(request).retrieve().toBodilessEntity().map(ResponseEntity::getHeaders);
	}

	@Override
	public <T> Mono<T> requestToBody(HttpRequestDefinition request, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(request).retrieve().bodyToMono(bodyType);
	}

	@Override
	public <T> Flux<T> requestToBodyFlux(HttpRequestDefinition request, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(request).retrieve().bodyToFlux(bodyType);
	}

	@Override
	public Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestDefinition request) {
		return toBodySpec(request).retrieve().toBodilessEntity();
	}

	@Override
	public <T> Mono<ResponseEntity<T>> requestToEntity(HttpRequestDefinition request, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(request).retrieve().toEntity(bodyType);
	}

	@Override
	public <T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(HttpRequestDefinition request, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(request).retrieve().toEntityFlux(bodyType);
	}

	@SuppressWarnings("ReactiveStreamsUnusedPublisher")
	private WebClient.RequestBodySpec toBodySpec(HttpRequestDefinition request) {

		HttpMethod httpMethod = request.getHttpMethodRequired();
		WebClient.RequestBodyUriSpec uriSpec = this.webClient.method(httpMethod);

		WebClient.RequestBodySpec bodySpec;
		if (request.getUri() != null) {
			bodySpec = uriSpec.uri(request.getUri());
		}
		else if (request.getUriTemplate() != null) {
			bodySpec = (!request.getUriVariables().isEmpty() ?
					uriSpec.uri(request.getUriTemplate(), request.getUriVariables()) :
					uriSpec.uri(request.getUriTemplate(), request.getUriVariableValues()));
		}
		else {
			bodySpec = uriSpec.uri("");
		}

		bodySpec.headers(headers -> headers.putAll(request.getHeaders()));
		bodySpec.cookies(cookies -> cookies.putAll(request.getCookies()));

		if (request.getBodyValue() != null) {
			bodySpec.bodyValue(request.getBodyValue());
		}
		else if (request.getBodyPublisher() != null) {
			bodySpec.body(request.getBodyPublisher(), request.getBodyPublisherElementType());
		}

		return bodySpec;
	}

}
