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
import org.springframework.web.service.invoker.HttpRequestSpec;


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
	public Mono<Void> requestToVoid(HttpRequestSpec requestSpec) {
		return toBodySpec(requestSpec).exchangeToMono(ClientResponse::releaseBody);
	}

	@Override
	public Mono<HttpHeaders> requestToHeaders(HttpRequestSpec requestSpec) {
		return toBodySpec(requestSpec).retrieve().toBodilessEntity().map(ResponseEntity::getHeaders);
	}

	@Override
	public <T> Mono<T> requestToBody(HttpRequestSpec reqrequestSpecest, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(reqrequestSpecest).retrieve().bodyToMono(bodyType);
	}

	@Override
	public <T> Flux<T> requestToBodyFlux(HttpRequestSpec requestSpec, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(requestSpec).retrieve().bodyToFlux(bodyType);
	}

	@Override
	public Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestSpec requestSpec) {
		return toBodySpec(requestSpec).retrieve().toBodilessEntity();
	}

	@Override
	public <T> Mono<ResponseEntity<T>> requestToEntity(HttpRequestSpec spec, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(spec).retrieve().toEntity(bodyType);
	}

	@Override
	public <T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(HttpRequestSpec spec, ParameterizedTypeReference<T> bodyType) {
		return toBodySpec(spec).retrieve().toEntityFlux(bodyType);
	}

	@SuppressWarnings("ReactiveStreamsUnusedPublisher")
	private WebClient.RequestBodySpec toBodySpec(HttpRequestSpec requestSpec) {

		HttpMethod httpMethod = requestSpec.getHttpMethodRequired();
		WebClient.RequestBodyUriSpec uriSpec = this.webClient.method(httpMethod);

		WebClient.RequestBodySpec bodySpec;
		if (requestSpec.getUri() != null) {
			bodySpec = uriSpec.uri(requestSpec.getUri());
		}
		else if (requestSpec.getUriTemplate() != null) {
			bodySpec = (!requestSpec.getUriVariables().isEmpty() ?
					uriSpec.uri(requestSpec.getUriTemplate(), requestSpec.getUriVariables()) :
					uriSpec.uri(requestSpec.getUriTemplate(), requestSpec.getUriVariableValues()));
		}
		else {
			bodySpec = uriSpec.uri("");
		}

		bodySpec.headers(headers -> headers.putAll(requestSpec.getHeaders()));
		bodySpec.cookies(cookies -> cookies.putAll(requestSpec.getCookies()));

		if (requestSpec.getBodyValue() != null) {
			bodySpec.bodyValue(requestSpec.getBodyValue());
		}
		else if (requestSpec.getBodyPublisher() != null) {
			bodySpec.body(requestSpec.getBodyPublisher(), requestSpec.getBodyPublisherElementType());
		}

		return bodySpec;
	}

}
