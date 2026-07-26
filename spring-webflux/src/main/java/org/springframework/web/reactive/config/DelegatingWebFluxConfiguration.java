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

package org.springframework.web.reactive.config;

import java.util.List;

import guru.mocker.annotation.mixin.Mixin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.ErrorResponse;

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
public class DelegatingWebFluxConfiguration extends DelegatingWebFluxConfigurationForwarder {

	public DelegatingWebFluxConfiguration() {
		this(new WebFluxConfigurerComposite());
	}

	@Mixin(grandparent = WebFluxConfigurationSupport.class)
	public DelegatingWebFluxConfiguration(WebFluxConfigurerComposite webFluxConfigurerComposite) {
		super(webFluxConfigurerComposite);
	}

	@Autowired(required = false)
	public void setConfigurers(List<WebFluxConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			webFluxConfigurerComposite.addWebFluxConfigurers(configurers);
		}
	}

	@Override
	public void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
		webFluxConfigurerComposite.addErrorResponseInterceptors(interceptors);
	}


}
