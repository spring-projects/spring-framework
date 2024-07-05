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

import java.util.Collection;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;

/**
 * Public API for HTML rendering from a collection or from a stream of
 * {@link Fragment}s each with its own view and model. For use with
 * view technologies such as <a href="https://htmx.org/">htmx</a> where multiple
 * page fragments may be rendered in a single response. Supported as a return
 * value from a WebFlux controller method.
 *
 * <p>For full page rendering with a single model and view, use {@link Rendering}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public interface FragmentRendering {

	/**
	 * Return the HTTP status to set the response to.
	 */
	@Nullable
	HttpStatusCode status();

	/**
	 * Return headers to add to the response.
	 */
	HttpHeaders headers();

	/**
	 * Return the fragments to render.
	 */
	Flux<Fragment> fragments();


	/**
	 * Create a builder to render with a collection of Fragments.
	 */
	static Builder fromCollection(Collection<Fragment> fragments) {
		return new DefaultFragmentRenderingBuilder(fragments);
	}

	/**
	 * Create a builder to render with a {@link Publisher} of Fragments.
	 */
	static <P extends Publisher<Fragment>> Builder fromPublisher(P fragments) {
		return new DefaultFragmentRenderingBuilder(fragments);
	}

	/**
	 * Variant of {@link #fromPublisher(Publisher)} that allows using any
	 * producer that can be resolved to {@link Publisher} via
	 * {@link ReactiveAdapterRegistry}.
	 */
	static Builder fromProducer(Object fragments) {
		return new DefaultFragmentRenderingBuilder(fragments);
	}


	/**
	 * Defines a builder for {@link FragmentRendering}.
	 */
	interface Builder {

		/**
		 * Specify the status to use for the response.
		 * @param status the status to set
		 * @return this builder
		 */
		Builder status(HttpStatusCode status);

		/**
		 * Add the given, single header value under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * Provides access to every header declared so far with the possibility
		 * to add, replace, or remove values.
		 * @param headersConsumer the consumer to provide access to
		 * @return this builder
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Build the {@link FragmentRendering} instance.
		 */
		FragmentRendering build();
	}

}
