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
 * Public API for HTML rendering of a collection of fragments each with a view
 * and independent model. For use with frontends technologies such as
 * <a href="https://htmx.org/">htmx</a> where multiple page fragments may be
 * rendered in one response. Supported as a return value from Spring MVC
 * controller methods.
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
	 * Create a builder and add a fragment with a view name and a model.
	 * @param viewName the name of the view for the fragment
	 * @param model attributes for the fragment in addition to model
	 * attributes inherited from the shared model for the request
	 * @return the created builder
	 */
	static Builder with(String viewName, Map<String, Object> model) {
		return new DefaultFragmentsRenderingBuilder().fragment(viewName, model);
	}

	/**
	 * Variant of {@link #with(String, Map)} with a view name only, but also
	 * inheriting model attributes from the shared model for the request.
	 * @param viewName the name of the view for the fragment
	 * @return the created builder
	 */
	static Builder with(String viewName) {
		return new DefaultFragmentsRenderingBuilder().fragment(viewName);
	}

	/**
	 * Variant of {@link #with(String, Map)} with a collection of fragments.
	 * @param fragments the fragments to add; each fragment also inherits model
	 * attributes from the shared model for the request
	 * @return the created builder
	 */
	static Builder with(Collection<ModelAndView> fragments) {
		return new DefaultFragmentsRenderingBuilder().fragments(fragments);
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
		 * Add a fragment with a view name and a model.
		 * @param viewName the name of the view for the fragment
		 * @param model attributes for the fragment in addition to model
		 * attributes inherited from the shared model for the request
		 * @return this builder
		 */
		Builder fragment(String viewName, Map<String, Object> model);

		/**
		 * Variant of {@link #fragment(String, Map)} with a view name only, but
		 * also inheriting model attributes from the shared model for the request.
		 * @param viewName the name of the view for the fragment
		 * @return this builder
		 */
		Builder fragment(String viewName);

		/**
		 * Variant of {@link #fragment(String, Map)} with a {@link ModelAndView}.
		 * @param fragment the fragment to add; the fragment also inherits model
		 * attributes from the shared model for the request
		 * @return this builder
		 */
		Builder fragment(ModelAndView fragment);

		/**
		 * Variant of {@link #fragment(String, Map)} with a collection of {@link ModelAndView}s.
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
