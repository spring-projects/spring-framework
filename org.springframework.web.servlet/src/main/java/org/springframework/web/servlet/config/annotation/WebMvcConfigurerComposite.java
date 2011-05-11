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

	private final List<WebMvcConfigurer> configurers = new ArrayList<WebMvcConfigurer>();

	public void addConfigurers(List<WebMvcConfigurer> configurers) {
		if (configurers != null) {
			this.configurers.addAll(configurers);
		}
	}

	public void addFormatters(FormatterRegistry formatterRegistry) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.addFormatters(formatterRegistry);
		}
	}

	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.configureMessageConverters(converters);
		}
	}

	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.addArgumentResolvers(argumentResolvers);
		}
	}

	public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.addReturnValueHandlers(returnValueHandlers);
		}
	}

	public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.configureHandlerExceptionResolvers(exceptionResolvers);
		}
	}

	public void configureInterceptors(InterceptorConfigurer interceptorRegistry) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.configureInterceptors(interceptorRegistry);
		}
	}

	public void configureViewControllers(ViewControllerConfigurer viewControllerConfigurer) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.configureViewControllers(viewControllerConfigurer);
		}
	}

	public void configureResourceHandling(ResourceConfigurer resourceConfigurer) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.configureResourceHandling(resourceConfigurer);
		}
	}

	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer handlerConfigurer) {
		for (WebMvcConfigurer configurer : configurers) {
			configurer.configureDefaultServletHandling(handlerConfigurer);
		}
	}

	public Validator getValidator() {
		Map<WebMvcConfigurer, Validator> validators = new HashMap<WebMvcConfigurer, Validator>();
		for (WebMvcConfigurer configurer : configurers) {
			Validator validator = configurer.getValidator();
			if (validator != null) {
				validators.put(configurer, validator);
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
