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

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Assertions on an {@code ExchangeResult}.
 *
 * <p>Use {@link ExchangeResult#assertThat()} to access these assertions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseAssertions<T> {

	private final ExchangeResult<T> exchangeResult;


	ResponseAssertions(ExchangeResult<T> exchangeResult) {
		this.exchangeResult = exchangeResult;
	}


	/**
	 * Assertions on the response status.
	 */
	public StatusAssertions<T> status() {
		return new StatusAssertions<>(this.exchangeResult.getResponseStatus(), this);
	}

	/**
	 * Assertions on response headers.
	 */
	public HeaderAssertions<T> header() {
		return new HeaderAssertions<>(this.exchangeResult.getResponseHeaders(), this);
	}

	/**
	 * Assert the response body is equal to the given value.
	 */
	public void bodyEquals(T value) {
		assertEquals("Response body", value, this.exchangeResult.getResponseBody());
	}

	/**
	 * Options for logging diagnostic information.
	 */
	public LoggingActions<T> log() {
		return new LoggingActions<T>(this.exchangeResult, this);
	}

}
