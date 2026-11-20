/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client.support;

import java.util.List;

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
class ClientResponseWrapperTests {

	private ClientResponse mockResponse = mock();

	private ClientResponseWrapper wrapper = new ClientResponseWrapper(mockResponse);


	@Test
	void response() {
		assertThat(wrapper.response()).isSameAs(mockResponse);
	}

	@Test
	void statusCode() {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		given(mockResponse.statusCode()).willReturn(status);

		assertThat(wrapper.statusCode()).isSameAs(status);
	}

	@Test
	void headers() {
		ClientResponse.Headers headers = mock();
		given(mockResponse.headers()).willReturn(headers);

		assertThat(wrapper.headers()).isSameAs(headers);
	}

	@Test
	void cookies() {
		MultiValueMap<String, ResponseCookie> cookies = mock();
		given(mockResponse.cookies()).willReturn(cookies);

		assertThat(wrapper.cookies()).isSameAs(cookies);
	}

	@Test
	void bodyExtractor() {
		Mono<String> result = Mono.just("foo");
		BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);
		given(mockResponse.body(extractor)).willReturn(result);

		assertThat(wrapper.body(extractor)).isSameAs(result);
	}

	@Test
	void bodyToMonoClass() {
		Mono<String> result = Mono.just("foo");
		given(mockResponse.bodyToMono(String.class)).willReturn(result);

		assertThat(wrapper.bodyToMono(String.class)).isSameAs(result);
	}

	@Test
	void bodyToMonoParameterizedTypeReference() {
		Mono<String> result = Mono.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() {};
		given(mockResponse.bodyToMono(reference)).willReturn(result);

		assertThat(wrapper.bodyToMono(reference)).isSameAs(result);
	}

	@Test
	void bodyToFluxClass() {
		Flux<String> result = Flux.just("foo");
		given(mockResponse.bodyToFlux(String.class)).willReturn(result);

		assertThat(wrapper.bodyToFlux(String.class)).isSameAs(result);
	}

	@Test
	void bodyToFluxParameterizedTypeReference() {
		Flux<String> result = Flux.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() {};
		given(mockResponse.bodyToFlux(reference)).willReturn(result);

		assertThat(wrapper.bodyToFlux(reference)).isSameAs(result);
	}

	@Test
	void toEntityClass() {
		Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
		given(mockResponse.toEntity(String.class)).willReturn(result);

		assertThat(wrapper.toEntity(String.class)).isSameAs(result);
	}

	@Test
	void toEntityParameterizedTypeReference() {
		Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() {};
		given(mockResponse.toEntity(reference)).willReturn(result);

		assertThat(wrapper.toEntity(reference)).isSameAs(result);
	}

	@Test
	void toEntityListClass() {
		Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
		given(mockResponse.toEntityList(String.class)).willReturn(result);

		assertThat(wrapper.toEntityList(String.class)).isSameAs(result);
	}

	@Test
	void toEntityListParameterizedTypeReference() {
		Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<>() {};
		given(mockResponse.toEntityList(reference)).willReturn(result);

		assertThat(wrapper.toEntityList(reference)).isSameAs(result);
	}



}
