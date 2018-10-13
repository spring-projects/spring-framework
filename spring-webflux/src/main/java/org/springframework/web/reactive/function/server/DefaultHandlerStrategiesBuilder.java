/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

/**
 * Default implementation of {@link HandlerStrategies.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultHandlerStrategiesBuilder implements HandlerStrategies.Builder {

	private final ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();

	private final List<ViewResolver> viewResolvers = new ArrayList<>();

	private final List<WebFilter> webFilters = new ArrayList<>();

	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	private LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();


	public DefaultHandlerStrategiesBuilder() {
		this.codecConfigurer.registerDefaults(false);
	}


	public void defaultConfiguration() {
		this.codecConfigurer.registerDefaults(true);
		this.exceptionHandlers.add(new WebFluxResponseStatusExceptionHandler());
		this.localeContextResolver = new AcceptHeaderLocaleContextResolver();
	}

	@Override
	public HandlerStrategies.Builder codecs(Consumer<ServerCodecConfigurer> consumer) {
		consumer.accept(this.codecConfigurer);
		return this;
	}

	@Override
	public HandlerStrategies.Builder viewResolver(ViewResolver viewResolver) {
		Assert.notNull(viewResolver, "ViewResolver must not be null");
		this.viewResolvers.add(viewResolver);
		return this;
	}

	@Override
	public HandlerStrategies.Builder webFilter(WebFilter filter) {
		Assert.notNull(filter, "WebFilter must not be null");
		this.webFilters.add(filter);
		return this;
	}

	@Override
	public HandlerStrategies.Builder exceptionHandler(WebExceptionHandler exceptionHandler) {
		Assert.notNull(exceptionHandler, "WebExceptionHandler must not be null");
		this.exceptionHandlers.add(exceptionHandler);
		return this;
	}

	@Override
	public HandlerStrategies.Builder localeContextResolver(LocaleContextResolver localeContextResolver) {
		Assert.notNull(localeContextResolver, "LocaleContextResolver must not be null");
		this.localeContextResolver = localeContextResolver;
		return this;
	}

	@Override
	public HandlerStrategies build() {
		return new DefaultHandlerStrategies(this.codecConfigurer.getReaders(),
				this.codecConfigurer.getWriters(), this.viewResolvers, this.webFilters,
				this.exceptionHandlers, this.localeContextResolver);
	}


	private static class DefaultHandlerStrategies implements HandlerStrategies {

		private final List<HttpMessageReader<?>> messageReaders;

		private final List<HttpMessageWriter<?>> messageWriters;

		private final List<ViewResolver> viewResolvers;

		private final List<WebFilter> webFilters;

		private final List<WebExceptionHandler> exceptionHandlers;

		private final LocaleContextResolver localeContextResolver;

		public DefaultHandlerStrategies(
				List<HttpMessageReader<?>> messageReaders,
				List<HttpMessageWriter<?>> messageWriters,
				List<ViewResolver> viewResolvers,
				List<WebFilter> webFilters,
				List<WebExceptionHandler> exceptionHandlers,
				LocaleContextResolver localeContextResolver) {

			this.messageReaders = unmodifiableCopy(messageReaders);
			this.messageWriters = unmodifiableCopy(messageWriters);
			this.viewResolvers = unmodifiableCopy(viewResolvers);
			this.webFilters = unmodifiableCopy(webFilters);
			this.exceptionHandlers = unmodifiableCopy(exceptionHandlers);
			this.localeContextResolver = localeContextResolver;
		}

		private static <T> List<T> unmodifiableCopy(List<? extends T> list) {
			return Collections.unmodifiableList(new ArrayList<>(list));
		}

		@Override
		public List<HttpMessageReader<?>> messageReaders() {
			return this.messageReaders;
		}

		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return this.messageWriters;
		}

		@Override
		public List<ViewResolver> viewResolvers() {
			return this.viewResolvers;
		}

		@Override
		public List<WebFilter> webFilters() {
			return this.webFilters;
		}

		@Override
		public List<WebExceptionHandler> exceptionHandlers() {
			return this.exceptionHandlers;
		}

		@Override
		public LocaleContextResolver localeContextResolver() {
			return this.localeContextResolver;
		}
	}

}
