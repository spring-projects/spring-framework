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
package org.springframework.test.web.reactive.server;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.AssertionErrors;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * Entry point for applying assertions and actions on a performed exchange.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ExchangeActions {

	private final ExchangeInfo exchangeInfo;


	public ExchangeActions(ExchangeInfo info) {
		this.exchangeInfo = info;
	}


	private ClientResponse getResponse() {
		return this.exchangeInfo.getResponse();
	}

	private Duration getResponseTimeout() {
		return this.exchangeInfo.getResponseTimeout();
	}


	/**
	 * Assert the status of the response.
	 * @return further options for asserting the status of the response
	 */
	public ResponseStatusAssertions assertStatus() {
		return new ResponseStatusAssertions(this, this.exchangeInfo);
	}

	/**
	 * Assert specific, commonly used response headers.
	 * @return further options for asserting headers
	 */
	public ResponseHeadersAssertions assertHeaders() {
		return new ResponseHeadersAssertions(this, getResponse().headers().asHttpHeaders());
	}

	/**
	 * Assert value(s) of the specified header name.
	 * @param headerName the header name
	 * @return options for asserting the header value(s)
	 */
	public ResponseHeaderAssertions assertHeader(String headerName) {
		HttpHeaders headers = getResponse().headers().asHttpHeaders();
		List<String> values = headers.getOrDefault(headerName, Collections.emptyList());
		return new ResponseHeaderAssertions(this, headerName, values);
	}

	/**
	 * Assert the response does not have any content.
	 */
	public ExchangeActions assertBodyIsEmpty() {
		Flux<?> body = this.exchangeInfo.getResponse().bodyToFlux(ByteBuffer.class);
		StepVerifier.create(body).expectComplete().verify(this.exchangeInfo.getResponseTimeout());
		return this;
	}

	/**
	 * Assert the content of the response.
	 * @return further options for asserting response entities
	 */
	public <T> ResponseEntityAssertions<T> assertEntity(Class<T> entityType) {
		return assertEntity(ResolvableType.forClass(entityType));
	}

	/**
	 * Assert the content of the response.
	 * @return further options for asserting response entities
	 */
	public <T> ResponseEntityAssertions<T> assertEntity(ResolvableType entityType) {
		return new ResponseEntityAssertions<T>(this, this.exchangeInfo, entityType);
	}

	/**
	 * Assert the response decoded as a Map of the given key and value types.
	 */
	public <K, V> MapAssertions<K, V> assertBodyAsMap(Class<K> keyType, Class<V> valueType) {
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, keyType, valueType);
		Mono<Map<K, V>> mono = this.exchangeInfo.getResponse().body(toMono(type));
		Map<K, V> map = mono.block(this.exchangeInfo.getResponseTimeout());
		return new MapAssertions<>(this, map, "Response body map");
	}

	/**
	 * Log debug information about the exchange.
	 */
	public LoggingExchangeConsumer log() {
		return new LoggingExchangeConsumer(this, this.exchangeInfo);
	}

	/**
	 * Apply custom assertions on the performed exchange with the help of
	 * {@link AssertionErrors} or an assertion library such as AssertJ.
	 * <p>Consider using statically imported methods to improve readability
	 * @param consumer consumer that will apply assertions.
	 */
	public ExchangeActions andAssert(Consumer<ExchangeInfo> consumer) {
		consumer.accept(this.exchangeInfo);
		return this;
	}

	/**
	 * Apply custom actions on the performed exchange.
	 * <p>Consider using statically imported methods to improve readability
	 * @param consumer consumer that will apply the custom action
	 */
	public ExchangeActions andDo(Consumer<ExchangeInfo> consumer) {
		consumer.accept(this.exchangeInfo);
		return this;
	}

	/**
	 * Return {@link ExchangeInfo} for direct access to request and response.
	 */
	public ExchangeInfo andReturn() {
		return this.exchangeInfo;
	}

}
