/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.BlockingExecutionConfigurer;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
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
		this.controllers = instantiateIfNecessary(controllers);
	}

	private static List<Object> instantiateIfNecessary(Object[] specified) {
		List<Object> instances = new ArrayList<>(specified.length);
		for (Object obj : specified) {
			instances.add(obj instanceof Class<?> clazz ? BeanUtils.instantiateClass(clazz) : obj);
		}
		return instances;
	}


	@Override
	public DefaultControllerSpec controllerAdvice(Object... controllerAdvices) {
		this.controllerAdvice.addAll(instantiateIfNecessary(controllerAdvices));
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
	public DefaultControllerSpec argumentResolvers(Consumer<ArgumentResolverConfigurer> consumer) {
		this.configurer.argumentResolverConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec pathMatching(Consumer<PathMatchConfigurer> consumer) {
		this.configurer.pathMatchConsumer = consumer;
		return this;
	}

	@Override
	public DefaultControllerSpec httpMessageCodecs(Consumer<ServerCodecConfigurer> consumer) {
		this.configurer.messageCodecsConsumer = consumer;
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
	public WebTestClient.ControllerSpec apiVersioning(Consumer<ApiVersionConfigurer> configurer) {
		this.configurer.versionConsumer = configurer;
		return this;
	}

	@Override
	public DefaultControllerSpec viewResolvers(Consumer<ViewResolverRegistry> consumer) {
		this.configurer.viewResolversConsumer = consumer;
		return this;
	}

	@Override
	public WebTestClient.ControllerSpec blockingExecution(Consumer<BlockingExecutionConfigurer> consumer) {
		this.configurer.executionConsumer = consumer;
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


	private static class TestWebFluxConfigurer implements WebFluxConfigurer {

		private @Nullable Consumer<RequestedContentTypeResolverBuilder> contentTypeResolverConsumer;

		private @Nullable Consumer<CorsRegistry> corsRegistryConsumer;

		private @Nullable Consumer<ArgumentResolverConfigurer> argumentResolverConsumer;

		private @Nullable Consumer<PathMatchConfigurer> pathMatchConsumer;

		private @Nullable Consumer<ServerCodecConfigurer> messageCodecsConsumer;

		private @Nullable Consumer<FormatterRegistry> formattersConsumer;

		private @Nullable Validator validator;

		private @Nullable Consumer<ApiVersionConfigurer> versionConsumer;

		private @Nullable Consumer<ViewResolverRegistry> viewResolversConsumer;

		private @Nullable Consumer<BlockingExecutionConfigurer> executionConsumer;

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
		public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
			if (this.argumentResolverConsumer != null) {
				this.argumentResolverConsumer.accept(configurer);
			}
		}

		@Override
		public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
			if (this.messageCodecsConsumer != null) {
				this.messageCodecsConsumer.accept(configurer);
			}
		}

		@Override
		public void addFormatters(FormatterRegistry registry) {
			if (this.formattersConsumer != null) {
				this.formattersConsumer.accept(registry);
			}
		}

		@Override
		public @Nullable Validator getValidator() {
			return this.validator;
		}

		@Override
		public void configureApiVersioning(ApiVersionConfigurer configurer) {
			if (this.versionConsumer != null) {
				this.versionConsumer.accept(configurer);
			}
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			if (this.viewResolversConsumer != null) {
				this.viewResolversConsumer.accept(registry);
			}
		}

		@Override
		public void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
			if (this.executionConsumer != null) {
				this.executionConsumer.accept(configurer);
			}
		}
	}

}
