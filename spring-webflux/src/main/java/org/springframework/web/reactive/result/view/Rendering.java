/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;

/**
 * Public API for HTML rendering. Supported as a return value in Spring WebFlux
 * controllers. Comparable to the use of {@code ModelAndView} as a return value
 * in Spring MVC controllers.
 *
 * <p>Controllers typically return a {@link String} view name and rely on the
 * "implicit" model which can also be injected into the controller method.
 * Or controllers may return model attribute(s) and rely on a default view name
 * being selected based on the request path.
 *
 * <p>{@code Rendering} can be used to combine a view name with model attributes,
 * set the HTTP status or headers, and for other more advanced options around
 * redirect scenarios.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Rendering {

	/**
	 * Return the selected {@link String} view name or {@link View} object.
	 */
	@Nullable
	Object view();

	/**
	 * Return attributes to add to the model.
	 */
	Map<String, Object> modelAttributes();

	/**
	 * Return the HTTP status to set the response to.
	 */
	@Nullable
	HttpStatus status();

	/**
	 * Return headers to add to the response.
	 */
	HttpHeaders headers();


	/**
	 * Create a new builder for response rendering based on the given view name.
	 * @param name the view name to be resolved to a {@link View}
	 * @return the builder
	 */
	static Builder<?> view(String name) {
		return new DefaultRenderingBuilder(name);
	}

	/**
	 * Create a new builder for a redirect through a {@link RedirectView}.
	 * @param url the redirect URL
	 * @return the builder
	 */
	static RedirectBuilder redirectTo(String url) {
		return new DefaultRenderingBuilder(new RedirectView(url));
	}


	/**
	 * Defines a builder for {@link Rendering}.
	 *
	 * @param <B> a self reference to the builder type
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * Add the given model attribute with the supplied name.
		 * @see Model#addAttribute(String, Object)
		 */
		B modelAttribute(String name, Object value);

		/**
		 * Add an attribute to the model using a
		 * {@link org.springframework.core.Conventions#getVariableName generated name}.
		 * @see Model#addAttribute(Object)
		 */
		B modelAttribute(Object value);

		/**
		 * Add all given attributes to the model using
		 * {@link org.springframework.core.Conventions#getVariableName generated names}.
		 * @see Model#addAllAttributes(Collection)
		 */
		B modelAttributes(Object... values);

		/**
		 * Add the given attributes to the model.
		 * @see Model#addAllAttributes(Map)
		 */
		B model(Map<String, ?> map);

		/**
		 * Specify the status to use for the response.
		 */
		B status(HttpStatus status);

		/**
		 * Specify a header to add to the response.
		 */
		B header(String headerName, String... headerValues);

		/**
		 * Specify headers to add to the response.
		 */
		B headers(HttpHeaders headers);

		/**
		 * Build the {@link Rendering} instance.
		 */
		Rendering build();
	}


	/**
	 * Extends {@link Builder} with extra options for redirect scenarios.
	 */
	interface RedirectBuilder extends Builder<RedirectBuilder> {

		/**
		 * Whether to the provided redirect URL should be prepended with the
		 * application context path (if any).
		 * <p>By default this is set to {@code true}.
		 * @see RedirectView#setContextRelative(boolean)
		 */
		RedirectBuilder contextRelative(boolean contextRelative);

		/**
		 * Whether to append the query string of the current URL to the target
		 * redirect URL or not.
		 * <p>By default this is set to {@code false}.
		 * @see RedirectView#setPropagateQuery(boolean)
		 */
		RedirectBuilder propagateQuery(boolean propagate);
	}

}
