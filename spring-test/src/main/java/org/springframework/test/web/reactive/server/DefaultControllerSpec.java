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

package org.springframework.test.web.reactive.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Default implementation of {@link WebTestClient.ControllerSpec}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultControllerSpec extends AbstractMockServerSpec<WebTestClient.ControllerSpec>
		implements WebTestClient.ControllerSpec {

	private final List<Object> controllers;

	private final List<Object> controllerAdvice = new ArrayList<>(8);

	private final TestWebFluxConfigurer configurer = new TestWebFluxConfigurer();


	DefaultControllerSpec(Object... controllers) {
		Assert.isTrue(!ObjectUtils.isEmpty(controllers), "At least one controller is required");
		this.controllers = Arrays.asList(controllers);
	}


	@Override
	public DefaultControllerSpec controllerAdvice(Object... controllerAdvice) {
		this.controllerAdvice.addAll(Arrays.asList(controllerAdvice));
		return this;
	}

	@Override
	public DefaultControllerSpec contentTypeResolver(Consumer<RequestedContentTypeResolverBuilder> consumer) {
		this.configurer.contentTypeResolverConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec corsMappings(Consumer<CorsRegistry> consumer) {
		this.configurer.corsRegistryConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec pathMatching(Consumer<PathMatchConfigurer> consumer) {
		this.configurer.pathMatchConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec messageReaders(Consumer<List<HttpMessageReader<?>>> consumer) {
		this.configurer.readersConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec messageWriters(Consumer<List<HttpMessageWriter<?>>> consumer) {
		this.configurer.writersConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec formatters(Consumer<FormatterRegistry> consumer) {
		this.configurer.formattersConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec validator(Validator validator) {
		this.configurer.validator = validator;
		return this;
	}

	@Override
	public DefaultControllerSpec viewResolvers(Consumer<ViewResolverRegistry> consumer) {
		this.configurer.viewResolversConsumer = consumer;
		return this;
	}


	@Override
	protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
		return WebHttpHandlerBuilder.applicationContext(initApplicationContext());
	}

	private ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		this.controllers.forEach(controller -> {
			String name = controller.getClass().getName();
			context.registerBean(name, Object.class, () -> controller);
		});
		this.controllerAdvice.forEach(advice -> {
			String name = advice.getClass().getName();
			context.registerBean(name, Object.class, () -> advice);
		});
		context.register(DelegatingWebFluxConfiguration.class);
		context.registerBean(WebFluxConfigurer.class, () -> this.configurer);
		context.refresh();
		return context;
	}


	private class TestWebFluxConfigurer implements WebFluxConfigurer {

		private Consumer<RequestedContentTypeResolverBuilder> contentTypeResolverConsumer;

		private Consumer<CorsRegistry> corsRegistryConsumer;

		private Consumer<PathMatchConfigurer> pathMatchConsumer;

		private Consumer<List<HttpMessageReader<?>>> readersConsumer;

		private Consumer<List<HttpMessageWriter<?>>> writersConsumer;

		private Consumer<FormatterRegistry> formattersConsumer;

		private Validator validator;

		private Consumer<ViewResolverRegistry> viewResolversConsumer;


		@Override
		public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
			if (this.contentTypeResolverConsumer != null) {
				this.contentTypeResolverConsumer.accept(builder);
			}
		}

		@Override
		public void addCorsMappings(CorsRegistry registry) {
			if (this.corsRegistryConsumer != null) {
				this.corsRegistryConsumer.accept(registry);
			}
		}

		@Override
		public void configurePathMatching(PathMatchConfigurer configurer) {
			if (this.pathMatchConsumer != null) {
				this.pathMatchConsumer.accept(configurer);
			}
		}

		@Override
		public void extendMessageReaders(List<HttpMessageReader<?>> readers) {
			if (this.readersConsumer != null) {
				this.readersConsumer.accept(readers);
			}
		}

		@Override
		public void extendMessageWriters(List<HttpMessageWriter<?>> writers) {
			if (this.writersConsumer != null) {
				this.writersConsumer.accept(writers);
			}
		}

		@Override
		public void addFormatters(FormatterRegistry registry) {
			if (this.formattersConsumer != null) {
				this.formattersConsumer.accept(registry);
			}
		}

		@Override
		public Optional<Validator> getValidator() {
			return Optional.ofNullable(this.validator);
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			if (this.viewResolversConsumer != null) {
				this.viewResolversConsumer.accept(registry);
			}
		}
	}

}
