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

package org.springframework.web.reactive.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * A subclass of {@code WebFluxConfigurationSupport} that detects and delegates
 * to all beans of type {@link WebFluxConfigurer} allowing them to customize the
 * configuration provided by {@code WebFluxConfigurationSupport}. This is the
 * class actually imported by {@link EnableWebFlux @EnableWebFlux}.
 *
 * @author Brian Clozel
 * @since 5.0
 */
@Configuration(proxyBeanMethods = false)
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
	protected void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		this.configurers.configureArgumentResolvers(configurer);
	}

	@Override
	protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		this.configurers.configureHttpMessageCodecs(configurer);
	}

	@Override
	protected void addFormatters(FormatterRegistry registry) {
		this.configurers.addFormatters(registry);
	}

	@Override
	protected Validator getValidator() {
		Validator validator = this.configurers.getValidator();
		return (validator != null ? validator : super.getValidator());
	}

	@Override
	protected MessageCodesResolver getMessageCodesResolver() {
		MessageCodesResolver messageCodesResolver = this.configurers.getMessageCodesResolver();
		return (messageCodesResolver != null ? messageCodesResolver : super.getMessageCodesResolver());
	}

	@Override
	protected void configureViewResolvers(ViewResolverRegistry registry) {
		this.configurers.configureViewResolvers(registry);
	}
}
