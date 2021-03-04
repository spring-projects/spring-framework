/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class ClientResponseWrapperTests {

	private ClientResponse mockResponse;

	private ClientResponseWrapper wrapper;

	@BeforeEach
	public void createWrapper() {
		this.mockResponse = mock(ClientResponse.class);
		this.wrapper = new ClientResponseWrapper(mockResponse);
	}

	@Test
	public void response() {
		assertThat(wrapper.response()).isSameAs(mockResponse);
	}

	@Test
	public void statusCode() {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		given(mockResponse.statusCode()).willReturn(status);

		assertThat(wrapper.statusCode()).isSameAs(status);
	}

	@Test
	public void rawStatusCode() {
		int status = 999;
		given(mockResponse.rawStatusCode()).willReturn(status);

		assertThat(wrapper.rawStatusCode()).isEqualTo(status);
	}

	@Test
	public void headers() {
		ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
		given(mockResponse.headers()).willReturn(headers);

		assertThat(wrapper.headers()).isSameAs(headers);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cookies() {
		MultiValueMap<String, ResponseCookie> cookies = mock(MultiValueMap.class);
		given(mockResponse.cookies()).willReturn(cookies);

		assertThat(wrapper.cookies()).isSameAs(cookies);
	}

	@Test
	public void bodyExtractor() {
		Mono<String> result = Mono.just("foo");
		BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);
		given(mockResponse.body(extractor)).willReturn(result);

		assertThat(wrapper.body(extractor)).isSameAs(result);
	}

	@Test
	public void bodyToMonoClass() {
		Mono<String> result = Mono.just("foo");
		given(mockResponse.bodyToMono(String.class)).willReturn(result);

		assertThat(wrapper.bodyToMono(String.class)).isSameAs(result);
	}

	@Test
	public void bodyToMonoParameterizedTypeReference() {
		Mono<String> result = Mono.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		given(mockResponse.bodyToMono(reference)).willReturn(result);

		assertThat(wrapper.bodyToMono(reference)).isSameAs(result);
	}

	@Test
	public void bodyToFluxClass() {
		Flux<String> result = Flux.just("foo");
		given(mockResponse.bodyToFlux(String.class)).willReturn(result);

		assertThat(wrapper.bodyToFlux(String.class)).isSameAs(result);
	}

	@Test
	public void bodyToFluxParameterizedTypeReference() {
		Flux<String> result = Flux.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		given(mockResponse.bodyToFlux(reference)).willReturn(result);

		assertThat(wrapper.bodyToFlux(reference)).isSameAs(result);
	}

	@Test
	public void toEntityClass() {
		Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
		given(mockResponse.toEntity(String.class)).willReturn(result);

		assertThat(wrapper.toEntity(String.class)).isSameAs(result);
	}

	@Test
	public void toEntityParameterizedTypeReference() {
		Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		given(mockResponse.toEntity(reference)).willReturn(result);

		assertThat(wrapper.toEntity(reference)).isSameAs(result);
	}

	@Test
	public void toEntityListClass() {
		Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
		given(mockResponse.toEntityList(String.class)).willReturn(result);

		assertThat(wrapper.toEntityList(String.class)).isSameAs(result);
	}

	@Test
	public void toEntityListParameterizedTypeReference() {
		Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		given(mockResponse.toEntityList(reference)).willReturn(result);

		assertThat(wrapper.toEntityList(reference)).isSameAs(result);
	}



}
