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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * An {@link WebMvcConfigurer} implementation that delegates to other {@link WebMvcConfigurer} instances.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
class WebMvcConfigurerComposite implements WebMvcConfigurer {

	private final List<WebMvcConfigurer> delegates = new ArrayList<WebMvcConfigurer>();

	public void addWebMvcConfigurers(List<WebMvcConfigurer> configurers) {
		if (configurers != null) {
			this.delegates.addAll(configurers);
		}
	}

	public void addFormatters(FormatterRegistry registry) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.addFormatters(registry);
		}
	}

	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.configureMessageConverters(converters);
		}
	}

	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.addArgumentResolvers(argumentResolvers);
		}
	}

	public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.addReturnValueHandlers(returnValueHandlers);
		}
	}

	public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.configureHandlerExceptionResolvers(exceptionResolvers);
		}
	}

	public void addInterceptors(InterceptorRegistry registry) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.addInterceptors(registry);
		}
	}

	public void addViewControllers(ViewControllerRegistry registry) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.addViewControllers(registry);
		}
	}

	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.addResourceHandlers(registry);
		}
	}

	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		for (WebMvcConfigurer delegate : delegates) {
			delegate.configureDefaultServletHandling(configurer);
		}
	}

	public Validator getValidator() {
		Map<WebMvcConfigurer, Validator> validators = new HashMap<WebMvcConfigurer, Validator>();
		for (WebMvcConfigurer delegate : delegates) {
			Validator validator = delegate.getValidator();
			if (validator != null) {
				validators.put(delegate, validator);
			}
		}
		if (validators.size() == 0) {
			return null;
		}
		else if (validators.size() == 1) {
			return validators.values().iterator().next();
		}
		else {
			throw new IllegalStateException(
					"Multiple custom validators provided from [" + validators.keySet() + "]");
		}
	}

}
