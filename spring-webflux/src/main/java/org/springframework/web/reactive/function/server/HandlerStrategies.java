/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.i18n.LocaleContextResolver;

/**
 * Defines the strategies to be used for processing {@link HandlerFunction HandlerFunctions}.
 *
 * <p>An instance of this class is immutable. Instances are typically created through the
 * mutable {@link Builder}: either through {@link #builder()} to set up default strategies,
 * or {@link #empty()} to start from scratch.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 * @see RouterFunctions#toHttpHandler(RouterFunction, HandlerStrategies)
 */
public interface HandlerStrategies {

	/**
	 * Return the {@link HttpMessageReader HttpMessageReaders} to be used for request body conversion.
	 * @return the message readers
	 */
	List<HttpMessageReader<?>> messageReaders();

	/**
	 * Return the {@link HttpMessageWriter HttpMessageWriters} to be used for response body conversion.
	 * @return the message writers
	 */
	List<HttpMessageWriter<?>> messageWriters();

	/**
	 * Return the {@link ViewResolver ViewResolvers} to be used for view name resolution.
	 * @return the view resolvers
	 */
	List<ViewResolver> viewResolvers();

	/**
	 * Return the {@link WebFilter WebFilters} to be used for filtering the request and response.
	 * @return the web filters
	 */
	List<WebFilter> webFilters();

	/**
	 * Return the {@link WebExceptionHandler WebExceptionHandlers} to be used for handling exceptions.
	 * @return the exception handlers
	 */
	List<WebExceptionHandler> exceptionHandlers();

	/**
	 * Return the {@link LocaleContextResolver} to be used for resolving locale context.
	 * @return the locale context resolver
	 */
	LocaleContextResolver localeContextResolver();


	// Static builder methods

	/**
	 * Return a new {@code HandlerStrategies} with default initialization.
	 * @return the new {@code HandlerStrategies}
	 */
	static HandlerStrategies withDefaults() {
		return builder().build();
	}

	/**
	 * Return a mutable builder for a {@code HandlerStrategies} with default initialization.
	 * @return the builder
	 */
	static Builder builder() {
		DefaultHandlerStrategiesBuilder builder = new DefaultHandlerStrategiesBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * Return a mutable, empty builder for a {@code HandlerStrategies}.
	 * @return the builder
	 */
	static Builder empty() {
		return new DefaultHandlerStrategiesBuilder();
	}


	/**
	 * A mutable builder for a {@link HandlerStrategies}.
	 */
	interface Builder {

		/**
		 * Customize the list of server-side HTTP message readers and writers.
		 * @param consumer the consumer to customize the codecs
		 * @return this builder
		 */
		Builder codecs(Consumer<ServerCodecConfigurer> consumer);

		/**
		 * Add the given view resolver to this builder.
		 * @param viewResolver the view resolver to add
		 * @return this builder
		 */
		Builder viewResolver(ViewResolver viewResolver);

		/**
		 * Add the given web filter to this builder.
		 * @param filter the filter to add
		 * @return this builder
		 */
		Builder webFilter(WebFilter filter);

		/**
		 * Add the given exception handler to this builder.
		 * @param exceptionHandler the exception handler to add
		 * @return this builder
		 */
		Builder exceptionHandler(WebExceptionHandler exceptionHandler);

		/**
		 * Add the given locale context resolver to this builder.
		 * @param localeContextResolver the locale context resolver to add
		 * @return this builder
		 */
		Builder localeContextResolver(LocaleContextResolver localeContextResolver);

		/**
		 * Builds the {@link HandlerStrategies}.
		 * @return the built strategies
		 */
		HandlerStrategies build();
	}

}
