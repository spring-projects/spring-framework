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

package org.springframework.web.servlet.view;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;

/**
 * Public API to render HTML fragments. A fragment is a portion of an HTML page.
 * Normally HTML is rendered with a single model and view. This API allows
 * using multiple model and view pairs, one for each HTML fragment.
 *
 * <p>For use with frontends technologies such as
 * <a href="https://htmx.org/">htmx</a> where multiple page fragments may be
 * rendered in one response.
 *
 * <p>Supported as a return value from controller methods.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public interface FragmentsRendering extends SmartView {

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
	 * Create a builder with one HTML fragment, also inheriting attributes from
	 * the shared model for the request.
	 * @param viewName the name of the view for the fragment
	 * @return the created builder
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
	 * @return the created builder
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
	static Builder fragments(Collection<ModelAndView> fragments) {
		return new DefaultFragmentsRenderingBuilder().fragments(fragments);
	}

	/**
	 * Create a builder with one HTML fragment, also inheriting attributes from
	 * the shared model for the request.
	 * @param viewName the name of the view for the fragment
	 * @return the created builder
	 * @deprecated in favor of {@link #fragment(String)}
	 */
	@Deprecated(since = "6.2.1", forRemoval = true)
	static Builder with(String viewName) {
		return fragment(viewName);
	}

	/**
	 * Create a builder with one HTML fragment.
	 * @param viewName the view name for the fragment
	 * @param model attributes for the fragment, in addition to attributes from the
	 * shared model for the request
	 * @return the created builder
	 * @deprecated in favor of {@link #fragment(String, Map)}
	 */
	@Deprecated(since = "6.2.1", forRemoval = true)
	static Builder with(String viewName, Map<String, Object> model) {
		return fragment(viewName, model);
	}

	/**
	 * Create a builder with multiple HTML fragments.
	 * @param fragments the fragments to add; each fragment also inherits
	 * attributes from the shared model for the request
	 * @return the created builder
	 * @deprecated in favor of {@link #fragments(Collection)}
	 */
	@Deprecated(since = "6.2.1", forRemoval = true)
	static Builder with(Collection<ModelAndView> fragments) {
		return fragments(fragments);
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
		 * @param viewName the name of the view for the fragment
		 * @param model fragment attributes in addition to attributes from the
		 * shared model for the request
		 * @return this builder
		 */
		Builder fragment(String viewName, Map<String, Object> model);

		/**
		 * Add an HTML fragment. The fragment will use attributes from the shared
		 * model for the request.
		 * @param viewName the name of the view for the fragment
		 * @return this builder
		 */
		Builder fragment(String viewName);

		/**
		 * Add an HTML fragment.
		 * @param fragment the fragment to add; the fragment also inherits
		 * attributes from the shared model for the request
		 * @return this builder
		 */
		Builder fragment(ModelAndView fragment);

		/**
		 * Add multiple HTML fragments.
		 * @param fragments the fragments to add; each fragment also inherits model
		 * attributes from the shared model for the request
		 * @return this builder
		 */
		Builder fragments(Collection<ModelAndView> fragments);

		/**
		 * Build a {@link FragmentsRendering} instance.
		 */
		FragmentsRendering build();

	}

}
