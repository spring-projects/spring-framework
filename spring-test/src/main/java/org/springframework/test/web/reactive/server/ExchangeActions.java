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

import java.util.Collections;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;

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
	 * Assertions on the body of the response decoded as one or more response
	 * entities of the given type.
	 * @return options for asserting response entities
	 */
	public <T> ResponseEntityAssertions<T> assertEntity(Class<T> entityType) {
		return new ResponseEntityAssertions<T>(this, ResolvableType.forClass(entityType));
	}

	/**
	 * Assertions on the body of the response.
	 */
	public ResponseBodyAssertions assertBody() {
		return new ResponseBodyAssertions(this);
	}

	/**
	 * Log debug information about the exchange.
	 */
	public LoggingExchangeConsumer log() {
		return new LoggingExchangeConsumer(this);
	}

	/**
	 * Return {@link ExchangeInfo} for direct access to request and response.
	 */
	public ExchangeInfo andReturn() {
		return this.exchangeInfo;
	}

}
