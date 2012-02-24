/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Extends {@link WebMvcConfigurationSupport} with the ability to detect beans
 * of type {@link WebMvcConfigurer} and give them a chance to customize the 
 * provided configuration by delegating to them at the appropriate times.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 * 
 * @see EnableWebMvc
 */
@Configuration
public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport {

	private final WebMvcConfigurerComposite configurers = new WebMvcConfigurerComposite();

	@Autowired(required = false)
	public void setConfigurers(List<WebMvcConfigurer> configurers) {
		if (configurers == null || configurers.isEmpty()) {
			return;
		}
		this.configurers.addWebMvcConfigurers(configurers);
	}

	@Override
	protected void addInterceptors(InterceptorRegistry registry) {
		configurers.addInterceptors(registry);
	}

	@Override
	protected void addViewControllers(ViewControllerRegistry registry) {
		configurers.addViewControllers(registry);
	}

	@Override
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		configurers.addResourceHandlers(registry);
	}

	@Override
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurers.configureDefaultServletHandling(configurer);
	}
	
	@Override
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		configurers.addArgumentResolvers(argumentResolvers);
	}

	@Override
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		configurers.addReturnValueHandlers(returnValueHandlers);
	}

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		configurers.configureMessageConverters(converters);
	}

	@Override
	protected void addFormatters(FormatterRegistry registry) {
		configurers.addFormatters(registry);
	}

	@Override
	protected Validator getValidator() {
		return configurers.getValidator();
	}

	@Override
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		configurers.configureHandlerExceptionResolvers(exceptionResolvers);
	}

}
