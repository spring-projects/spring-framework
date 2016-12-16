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

package org.springframework.web.reactive.function.server;

import java.util.Collection;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Rendering-specific subtype of {@link ServerResponse} that exposes model and template data.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface RenderingResponse extends ServerResponse {

	/**
	 * Return the name of the template to be rendered.
	 */
	String name();

	/**
	 * Return the unmodifiable model map.
	 */
	Map<String, Object> model();

	// Builder

	/**
	 * Create a builder with the given template name.
	 *
	 * @param name the name of the template to render
	 * @return the created builder
	 */
	static Builder create(String name) {
		Assert.notNull(name, "'name' must not be null");
		return new DefaultRenderingResponseBuilder(name);
	}

	/**
	 * Defines a builder for {@code RenderingResponse}.
	 */
	interface Builder {

		/**
		 * Add the supplied attribute to the model using a
		 * {@linkplain org.springframework.core.Conventions#getVariableName generated name}.
		 * <p><emphasis>Note: Empty {@link Collection Collections} are not added to
		 * the model when using this method because we cannot correctly determine
		 * the true convention name. View code should check for {@code null} rather
		 * than for empty collections.</emphasis>
		 * @param attribute the model attribute value (never {@code null})
		 */
		Builder modelAttribute(Object attribute);

		/**
		 * Add the supplied attribute value under the supplied name.
		 * @param name the name of the model attribute (never {@code null})
		 * @param value the model attribute value (can be {@code null})
		 */
		Builder modelAttribute(String name, Object value);

		/**
		 * Copy all attributes in the supplied array into the model, using attribute
		 * name generation for each element.
		 * @see #modelAttribute(Object)
		 */
		Builder modelAttributes(Object... attributes);

		/**
		 * Copy all attributes in the supplied {@code Collection} into the model, using attribute
		 * name generation for each element.
		 * @see #modelAttribute(Object)
		 */
		Builder modelAttributes(Collection<?> attributes);

		/**
		 * Copy all attributes in the supplied {@code Map} into the model.
		 * @see #modelAttribute(String, Object)
		 */
		Builder modelAttributes(Map<String, ?> attributes);

		/**
		 * Add the given header value(s) under the given name.
		 * @param headerName the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * Copy the given headers into the entity's headers map.
		 * @param headers the existing HttpHeaders to copy from
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder headers(HttpHeaders headers);

		/**
		 * Set the status.
		 * @param status the response status
		 * @return this builder
		 */
		Builder status(HttpStatus status);

		/**
		 * Build the response.
		 *
		 * @return the built response
		 */
		Mono<RenderingResponse> build();

	}


}
