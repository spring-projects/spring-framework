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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.AssertionErrors;
import org.springframework.web.reactive.function.client.ClientResponse;

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


	/**
	 * Assert the status of the response.
	 * @return further options for asserting the status of the response
	 */
	public ResponseStatusAssertions assertStatus() {
		return new ResponseStatusAssertions(this);
	}

	/**
	 * Assert specific, commonly used response headers.
	 * @return further options for asserting headers
	 */
	public ResponseHeadersAssertions assertHeaders() {
		HttpHeaders headers = this.exchangeInfo.getResponse().headers().asHttpHeaders();
		return new ResponseHeadersAssertions(this, headers);
	}

	/**
	 * Assert value(s) of the specified header name.
	 * @param headerName the header name
	 * @return options for asserting the header value(s)
	 */
	public ResponseHeaderAssertions assertHeader(String headerName) {
		HttpHeaders headers = this.exchangeInfo.getResponse().headers().asHttpHeaders();
		List<String> values = headers.getOrDefault(headerName, Collections.emptyList());
		return new ResponseHeaderAssertions(this, headerName, values);
	}

	/**
	 * Assert the response does not have any content.
	 */
	public ResponseBodyAssertions assertBody() {
		return new ResponseBodyAssertions(this);
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
		return new ResponseEntityAssertions<T>(this, entityType);
	}

	/**
	 * Log debug information about the exchange.
	 */
	public LoggingExchangeConsumer log() {
		return new LoggingExchangeConsumer(this);
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
