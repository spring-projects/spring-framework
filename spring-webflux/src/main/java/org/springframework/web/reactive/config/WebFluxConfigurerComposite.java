/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerHttpMessageReader;
import org.springframework.http.codec.ServerHttpMessageWriter;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;

/**
 * A {@link WebFluxConfigurer} that delegates to one or more others.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebFluxConfigurerComposite implements WebFluxConfigurer {

	private final List<WebFluxConfigurer> delegates = new ArrayList<>();


	public void addWebFluxConfigurers(List<WebFluxConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.delegates.addAll(configurers);
		}
	}


	@Override
	public void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		this.delegates.stream().forEach(delegate -> delegate.configureContentTypeResolver(builder));
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.addCorsMappings(registry));
	}

	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.delegates.stream().forEach(delegate -> delegate.configurePathMatching(configurer));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.addResourceHandlers(registry));
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.delegates.stream().forEach(delegate -> delegate.addArgumentResolvers(resolvers));
	}

	@Override
	public void configureMessageReaders(List<ServerHttpMessageReader<?>> readers) {
		this.delegates.stream().forEach(delegate -> delegate.configureMessageReaders(readers));
	}

	@Override
	public void extendMessageReaders(List<ServerHttpMessageReader<?>> readers) {
		this.delegates.stream().forEach(delegate -> delegate.extendMessageReaders(readers));
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.addFormatters(registry));
	}

	@Override
	public Optional<Validator> getValidator() {
		return createSingleBean(WebFluxConfigurer::getValidator, Validator.class);
	}

	@Override
	public Optional<MessageCodesResolver> getMessageCodesResolver() {
		return createSingleBean(WebFluxConfigurer::getMessageCodesResolver, MessageCodesResolver.class);
	}

	@Override
	public void configureMessageWriters(List<ServerHttpMessageWriter<?>> writers) {
		this.delegates.stream().forEach(delegate -> delegate.configureMessageWriters(writers));
	}

	@Override
	public void extendMessageWriters(List<ServerHttpMessageWriter<?>> writers) {
		this.delegates.stream().forEach(delegate -> delegate.extendMessageWriters(writers));
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		this.delegates.stream().forEach(delegate -> delegate.configureViewResolvers(registry));
	}

	private <T> Optional<T> createSingleBean(Function<WebFluxConfigurer, Optional<T>> factory,
			Class<T> beanType) {

		List<Optional<T>> result = this.delegates.stream()
				.map(factory).filter(Optional::isPresent).collect(Collectors.toList());

		if (result.isEmpty()) {
			return Optional.empty();
		}
		else if (result.size() == 1) {
			return result.get(0);
		}
		else {
			throw new IllegalStateException("More than one WebFluxConfigurer implements " +
					beanType.getSimpleName() + " factory method.");
		}
	}

}
