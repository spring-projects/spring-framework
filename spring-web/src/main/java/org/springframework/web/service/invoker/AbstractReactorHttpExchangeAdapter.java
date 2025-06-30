/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.service.invoker;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * Convenient base class for a {@link ReactorHttpExchangeAdapter} implementation
 * adapting to the synchronous {@link HttpExchangeAdapter} contract.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public abstract class AbstractReactorHttpExchangeAdapter implements ReactorHttpExchangeAdapter {

	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

	private @Nullable Duration blockTimeout;


	/**
	 * Protected constructor, for subclasses.
	 */
	protected AbstractReactorHttpExchangeAdapter() {
	}


	/**
	 * Configure the {@link ReactiveAdapterRegistry} to use.
	 * <p>By default, this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry reactiveAdapterRegistry) {
		this.reactiveAdapterRegistry = reactiveAdapterRegistry;
	}

	@Override
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return this.reactiveAdapterRegistry;
	}

	/**
	 * Configure how long to block for the response of an HTTP service method
	 * as described in {@link #getBlockTimeout()}.
	 */
	public void setBlockTimeout(@Nullable Duration blockTimeout) {
		this.blockTimeout = blockTimeout;
	}

	@Override
	public @Nullable Duration getBlockTimeout() {
		return this.blockTimeout;
	}


	@Override
	public void exchange(HttpRequestValues requestValues) {
		if (this.blockTimeout != null) {
			exchangeForMono(requestValues).block(this.blockTimeout);
		}
		else {
			exchangeForMono(requestValues).block();
		}
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
		HttpHeaders headers = (this.blockTimeout != null ?
				exchangeForHeadersMono(requestValues).block(this.blockTimeout) :
				exchangeForHeadersMono(requestValues).block());
		Assert.state(headers != null, "Expected HttpHeaders");
		return headers;
	}

	@Override
	@Nullable
	public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return (this.blockTimeout != null ?
				exchangeForBodyMono(requestValues, bodyType).block(this.blockTimeout) :
				exchangeForBodyMono(requestValues, bodyType).block());
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
		ResponseEntity<Void> entity = (this.blockTimeout != null ?
				exchangeForBodilessEntityMono(requestValues).block(this.blockTimeout) :
				exchangeForBodilessEntityMono(requestValues).block());
		Assert.state(entity != null, "Expected ResponseEntity");
		return entity;
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(
			HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

		ResponseEntity<T> entity = (this.blockTimeout != null ?
				exchangeForEntityMono(requestValues, bodyType).block(this.blockTimeout) :
				exchangeForEntityMono(requestValues, bodyType).block());
		Assert.state(entity != null, "Expected ResponseEntity");
		return entity;
	}

}
