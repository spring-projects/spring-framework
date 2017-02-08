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
import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.AssertionErrors;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * An API to apply assertion and other actions against a performed exchange.
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
	 * Assert options for any response header specified by name.
	 * @return options for asserting headers
	 */
	public StringMultiValueMapEntryAssertions assertHeader(String headerName) {
		HttpHeaders headers = getResponse().headers().asHttpHeaders();
		return new StringMultiValueMapEntryAssertions(this, headerName, headers, "Response header");
	}

	/**
	 * Assert the response is empty.
	 */
	public void assertNoContent() {
		Flux<?> body = getResponse().bodyToFlux(ByteBuffer.class);
		StepVerifier.create(body).expectComplete().verify(getResponseTimeout());
	}

	/**
	 * Assert the content of the response.
	 * @param entityType the type of entity to decode the response as
	 * @param <T> the type of entity
	 * @return further options for asserting response entities
	 */
	public <T> ResponseContentAssertions<T> assertEntity(Class<T> entityType) {
		return new ResponseContentAssertions<T>(this.exchangeInfo, ResolvableType.forClass(entityType));
	}

	/**
	 * Variant of {@link #assertEntity(Class)} with a {@link ResolvableType}.
	 * @param entityType the type of entity to decode the response as
	 * @return further options for asserting response entities
	 */
	public <T> ResponseContentAssertions<T> assertEntity(ResolvableType entityType) {
		return new ResponseContentAssertions<T>(this.exchangeInfo, entityType);
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

}
