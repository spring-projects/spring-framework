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

/**
 * {@code ExchangeResult} sub-class that exposes the response body fully
 * extracted to a representation of type {@code <T>}.
 *
 * @param <T> the response body type
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see FluxExchangeResult
 */
public class EntityExchangeResult<T> extends ExchangeResult {

	private final T body;


	EntityExchangeResult(ExchangeResult result, T body) {
		super(result);
		this.body = body;
	}


	/**
	 * Return the entity extracted from the response body.
	 */
	public T getResponseBody() {
		return this.body;
	}

}
