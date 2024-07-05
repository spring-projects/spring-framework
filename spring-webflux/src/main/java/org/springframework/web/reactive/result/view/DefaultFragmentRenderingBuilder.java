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

package org.springframework.web.reactive.result.view;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link FragmentRendering.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
class DefaultFragmentRenderingBuilder implements FragmentRendering.Builder {

	private final Flux<Fragment> fragments;

	@Nullable
	private HttpStatusCode status;

	@Nullable
	private HttpHeaders headers;


	DefaultFragmentRenderingBuilder(Collection<Fragment> fragments) {
		this(Flux.fromIterable(fragments));
	}

	DefaultFragmentRenderingBuilder(Object fragments) {
		this(adaptProducer(fragments));
	}

	DefaultFragmentRenderingBuilder(Publisher<Fragment> fragments) {
		this.fragments = Flux.from(fragments);
	}

	private static Publisher<Fragment> adaptProducer(Object fragments) {
		ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(fragments.getClass());
		Assert.isTrue(adapter != null, "Unknown producer " + fragments.getClass());
		return adapter.toPublisher(fragments);
	}


	@Override
	public FragmentRendering.Builder status(HttpStatusCode status) {
		this.status = status;
		return this;
	}

	@Override
	public FragmentRendering.Builder header(String headerName, String... headerValues) {
		initHeaders().put(headerName, Arrays.asList(headerValues));
		return this;
	}

	@Override
	public FragmentRendering.Builder headers(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(initHeaders());
		return this;
	}

	private HttpHeaders initHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
		}
		return this.headers;
	}

	@Override
	public FragmentRendering build() {
		return new DefaultFragmentRendering(
				this.status, (this.headers != null ? this.headers : HttpHeaders.EMPTY), this.fragments);
	}


	/**
	 * Default implementation of {@link FragmentRendering}.
	 */
	private record DefaultFragmentRendering(@Nullable HttpStatusCode status, HttpHeaders headers, Flux<Fragment> fragments)
			implements FragmentRendering {
	}

}
