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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

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
		this.delegates.forEach(delegate -> delegate.configureContentTypeResolver(builder));
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addCorsMappings(registry));
	}

	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configurePathMatching(configurer));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addResourceHandlers(registry));
	}

	@Override
	public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configureArgumentResolvers(configurer));
	}

	@Override
	public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		this.delegates.forEach(delegate -> delegate.configureHttpMessageCodecs(configurer));
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		this.delegates.forEach(delegate -> delegate.addFormatters(registry));
	}

	@Override
	public Validator getValidator() {
		return createSingleBean(WebFluxConfigurer::getValidator, Validator.class);
	}

	@Override
	public MessageCodesResolver getMessageCodesResolver() {
		return createSingleBean(WebFluxConfigurer::getMessageCodesResolver, MessageCodesResolver.class);
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		this.delegates.forEach(delegate -> delegate.configureViewResolvers(registry));
	}

	@Nullable
	private <T> T createSingleBean(Function<WebFluxConfigurer, T> factory, Class<T> beanType) {
		List<T> result = this.delegates.stream().map(factory).filter(t -> t != null).collect(Collectors.toList());
		if (result.isEmpty()) {
			return null;
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
