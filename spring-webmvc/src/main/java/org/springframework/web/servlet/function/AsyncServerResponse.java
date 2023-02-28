/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;

import org.springframework.core.ReactiveAdapterRegistry;

/**
 * Asynchronous subtype of {@link ServerResponse} that exposes the future
 * response.
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 * @see ServerResponse#async(Object)
 */
public interface AsyncServerResponse extends ServerResponse {

	/**
	 * Blocks indefinitely until the future response is obtained.
	 */
	ServerResponse block();


	// Static creation methods

	/**
	 * Create a {@code AsyncServerResponse} with the given asynchronous response.
	 * Parameter {@code asyncResponse} can be a
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
	 * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
	 * asynchronous producer of a single {@code ServerResponse} that can be
	 * adapted via the {@link ReactiveAdapterRegistry}).
	 * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
	 * {@code Publisher<ServerResponse>}
	 * @return the asynchronous response
	 */
	static AsyncServerResponse create(Object asyncResponse) {
		return DefaultAsyncServerResponse.create(asyncResponse, null);
	}

	/**
	 * Create a (built) response with the given asynchronous response.
	 * Parameter {@code asyncResponse} can be a
	 * {@link CompletableFuture CompletableFuture&lt;ServerResponse&gt;} or
	 * {@link Publisher Publisher&lt;ServerResponse&gt;} (or any
	 * asynchronous producer of a single {@code ServerResponse} that can be
	 * adapted via the {@link ReactiveAdapterRegistry}).
	 * @param asyncResponse a {@code CompletableFuture<ServerResponse>} or
	 * {@code Publisher<ServerResponse>}
	 * @param timeout maximum time period to wait for before timing out
	 * @return the asynchronous response
	 */
	static AsyncServerResponse create(Object asyncResponse, Duration timeout) {
		return DefaultAsyncServerResponse.create(asyncResponse, timeout);
	}

}

