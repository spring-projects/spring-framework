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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;

/**
 * A subclass of {@code WebFluxConfigurationSupport} that detects and delegates
 * to all beans of type {@link WebFluxConfigurer} allowing them to customize the
 * configuration provided by {@code WebFluxConfigurationSupport}. This is the
 * class actually imported by {@link EnableWebFlux @EnableWebFlux}.
 *
 * @author Brian Clozel
 * @since 5.0
 */
@Configuration
public class DelegatingWebFluxConfiguration extends WebFluxConfigurationSupport {

	private final WebFluxConfigurerComposite configurers = new WebFluxConfigurerComposite();

	@Autowired(required = false)
	public void setConfigurers(List<WebFluxConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addWebFluxConfigurers(configurers);
		}
	}

	@Override
	protected void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		this.configurers.configureContentTypeResolver(builder);
	}

	@Override
	protected void addCorsMappings(CorsRegistry registry) {
		this.configurers.addCorsMappings(registry);
	}

	@Override
	public void configurePathMatching(PathMatchConfigurer configurer) {
		this.configurers.configurePathMatching(configurer);
	}

	@Override
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		this.configurers.addResourceHandlers(registry);
	}

	@Override
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.configurers.addArgumentResolvers(resolvers);
	}

	@Override
	protected void configureMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.configurers.configureMessageReaders(messageReaders);
	}

	@Override
	protected void extendMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		this.configurers.extendMessageReaders(messageReaders);
	}

	@Override
	protected void addFormatters(FormatterRegistry registry) {
		this.configurers.addFormatters(registry);
	}

	@Override
	protected Validator getValidator() {
		return this.configurers.getValidator().orElse(super.getValidator());
	}

	@Override
	protected MessageCodesResolver getMessageCodesResolver() {
		return this.configurers.getMessageCodesResolver().orElse(super.getMessageCodesResolver());
	}

	@Override
	protected void configureMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
		this.configurers.configureMessageWriters(messageWriters);
	}

	@Override
	protected void extendMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
		this.configurers.extendMessageWriters(messageWriters);
	}

	@Override
	protected void configureViewResolvers(ViewResolverRegistry registry) {
		this.configurers.configureViewResolvers(registry);
	}
}
