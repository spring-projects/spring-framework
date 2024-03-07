/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.concurrent.ExecutionException;

import org.reactivestreams.Publisher;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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
		return createInternal(asyncResponse, null);
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
		return createInternal(asyncResponse, timeout);
	}

	private static AsyncServerResponse createInternal(Object asyncResponse, @Nullable Duration timeout) {
		Assert.notNull(asyncResponse, "AsyncResponse must not be null");

		CompletableFuture<ServerResponse> futureResponse = toCompletableFuture(asyncResponse);
		if (futureResponse.isDone() &&
				!futureResponse.isCancelled() &&
				!futureResponse.isCompletedExceptionally()) {

			try {
				ServerResponse completedResponse = futureResponse.get();
				return new CompletedAsyncServerResponse(completedResponse);
			}
			catch (InterruptedException | ExecutionException ignored) {
				// fall through to use DefaultAsyncServerResponse
			}
		}
		return new DefaultAsyncServerResponse(futureResponse, timeout);
	}

	@SuppressWarnings("unchecked")
	private static CompletableFuture<ServerResponse> toCompletableFuture(Object obj) {
		if (obj instanceof CompletableFuture<?> futureResponse) {
			return (CompletableFuture<ServerResponse>) futureResponse;
		}
		else if (DefaultAsyncServerResponse.reactiveStreamsPresent) {
			ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
			ReactiveAdapter publisherAdapter = registry.getAdapter(obj.getClass());
			if (publisherAdapter != null) {
				Publisher<ServerResponse> publisher = publisherAdapter.toPublisher(obj);
				ReactiveAdapter futureAdapter = registry.getAdapter(CompletableFuture.class);
				if (futureAdapter != null) {
					return (CompletableFuture<ServerResponse>) futureAdapter.fromPublisher(publisher);
				}
			}
		}
		throw new IllegalArgumentException("Asynchronous type not supported: " + obj.getClass());
	}

}

