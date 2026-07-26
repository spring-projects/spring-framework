/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.List;

import guru.mocker.annotation.mixin.Mixin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.ErrorResponse;

/**
 * A subclass of {@code WebMvcConfigurationSupport} that detects and delegates
 * to all beans of type {@link WebMvcConfigurer} allowing them to customize the
 * configuration provided by {@code WebMvcConfigurationSupport}. This is the
 * class actually imported by {@link EnableWebMvc @EnableWebMvc}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@Configuration(proxyBeanMethods = false)
public class DelegatingWebMvcConfiguration extends DelegatingWebMvcConfigurationForwarder implements WebMvcConfigurer {

	@Mixin(grandparent = WebMvcConfigurationSupport.class)
	public DelegatingWebMvcConfiguration(WebMvcConfigurerComposite configurers) {
		super(configurers);
	}

	public DelegatingWebMvcConfiguration() {
		this(new WebMvcConfigurerComposite());
	}

	@Autowired(required = false)
	public void setConfigurers(List<WebMvcConfigurer> configurersList) {
		if (!CollectionUtils.isEmpty(configurersList)) {
			configurers.addWebMvcConfigurers(configurersList);
		}
	}

	// Manually implement deprecated methods (Mixin skips deprecated methods intentionally)
	@Override
	@Deprecated(since = "7.0", forRemoval = true)
	@SuppressWarnings("removal")
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		configurers.configureMessageConverters(converters);
	}

	@Override
	@Deprecated(since = "7.0", forRemoval = true)
	@SuppressWarnings("removal")
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		configurers.extendMessageConverters(converters);
	}

	// Override parent's protected method (Mixin generates interface method instead)
	@Override
	protected void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
		configurers.addErrorResponseInterceptors(interceptors);
	}

}
