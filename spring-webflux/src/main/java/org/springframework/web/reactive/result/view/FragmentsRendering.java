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
import java.util.Map;
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
 * Public API to render HTML fragments. A fragment is a portion of an HTML page.
 * Normally HTML is rendered with a single model and view. This API allows
 * using multiple model and view pairs, one for each HTML fragment.
 *
 * <p>For use with frontends technologies such as
 * <a href="https://htmx.org/">htmx</a> where multiple page fragments may be
 * rendered in one response.
 *
 * <p>Supported as a return value from annotated controller methods.
 * For full page rendering with a single model and view, use {@link Rendering}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public interface FragmentsRendering {

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
	 * Create a builder with one HTML fragment, also inheriting attributes from
	 * the shared model for the request.
	 * @param viewName the name of the view for the fragment
	 * @return this builder
	 * @since 6.2.1
	 */
	static Builder fragment(String viewName) {
		return new DefaultFragmentsRenderingBuilder().fragment(viewName);
	}

	/**
	 * Create a builder with one HTML fragment.
	 * @param viewName the view name for the fragment
	 * @param model attributes for the fragment, in addition to attributes from the
	 * shared model for the request
	 * @return this builder
	 * @since 6.2.1
	 */
	static Builder fragment(String viewName, Map<String, Object> model) {
		return new DefaultFragmentsRenderingBuilder().fragment(viewName, model);
	}

	/**
	 * Create a builder with multiple HTML fragments.
	 * @param fragments the fragments to add; each fragment also inherits
	 * attributes from the shared model for the request
	 * @return the created builder
	 * @since 6.2.1
	 */
	static Builder fragments(Collection<Fragment> fragments) {
		return new DefaultFragmentsRenderingBuilder().fragments(fragments);
	}

	/**
	 * Create a builder with a {@link Publisher} of fragments.
	 * @param fragmentsPublisher the fragments to add; each fragment also
	 * inherits model attributes from the shared model for the request
	 * @return the created builder
	 * @since 6.2.1
	 */
	static <P extends Publisher<Fragment>> Builder fragmentsPublisher(P fragmentsPublisher) {
		return new DefaultFragmentsRenderingBuilder(fragmentsPublisher);
	}

	/**
	 * Variant of {@link #fragmentsPublisher(Publisher)} that allows using any
	 * producer that can be resolved to {@link Publisher} via
	 * {@link ReactiveAdapterRegistry}.
	 * @since 6.2.1
	 */
	static Builder fragmentsProducer(Object fragmentsProducer) {
		ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(fragmentsProducer.getClass());
		Assert.isTrue(adapter != null, "Unknown producer " + fragmentsProducer.getClass());
		Publisher<Fragment> publisher = adapter.toPublisher(fragmentsProducer);
		return fragmentsPublisher(publisher);
	}


	/**
	 * Defines a builder for {@link FragmentsRendering}.
	 */
	interface Builder {

		/**
		 * Specify the status to use for the response.
		 * @param status the status to set
		 * @return this builder
		 */
		Builder status(HttpStatusCode status);

		/**
		 * Add one or more header values under the given name.
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
		 * Add an HTML fragment.
		 * @param viewName the view name for the fragment
		 * @param model fragment attributes in addition to attributes from the
		 * shared model for the request
		 * @return this builder
		 */
		Builder fragment(String viewName, Map<String, Object> model);

		/**
		 * Add an HTML fragment. The fragment will use attributes from the shared
		 * model for the request.
		 * @param viewName the view name for the fragment
		 * @return this builder
		 */
		Builder fragment(String viewName);

		/**
		 * Add an HTML fragment.
		 * @param fragment the fragment to add; the fragment also inherits
		 * attributes from the shared model for the request
		 * @return this builder
		 */
		Builder fragment(Fragment fragment);

		/**
		 * Add HTML fragments.
		 * @param fragments the fragments to add; each fragment also inherits
		 * attributes from the shared model for the request
		 * @return this builder
		 * @since 6.2.1
		 */
		Builder fragments(Collection<Fragment> fragments);

		/**
		 * Build the {@link FragmentsRendering} instance.
		 */
		FragmentsRendering build();
	}

}
