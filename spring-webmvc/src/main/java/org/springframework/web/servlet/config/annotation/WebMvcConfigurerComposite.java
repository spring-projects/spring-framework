/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.List;

import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
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

	@Override
	public void addFormatters(FormatterRegistry registry) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.addFormatters(registry);
		}
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.configureContentNegotiation(configurer);
		}
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.configureAsyncSupport(configurer);
		}
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.configurePathMatch(configurer);
		}
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.configureMessageConverters(converters);
		}
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.extendMessageConverters(converters);
		}
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.addArgumentResolvers(argumentResolvers);
		}
	}

	@Override
	public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.addReturnValueHandlers(returnValueHandlers);
		}
	}

	@Override
	public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.configureHandlerExceptionResolvers(exceptionResolvers);
		}
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.addInterceptors(registry);
		}
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.addViewControllers(registry);
		}
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.configureViewResolvers(registry);
		}
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.addResourceHandlers(registry);
		}
	}

	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.configureDefaultServletHandling(configurer);
		}
	}

	@Override
	public Validator getValidator() {
		List<Validator> candidates = new ArrayList<Validator>();
		for (WebMvcConfigurer configurer : this.delegates) {
			Validator validator = configurer.getValidator();
			if (validator != null) {
				candidates.add(validator);
			}
		}
		return selectSingleInstance(candidates, Validator.class);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		for (WebMvcConfigurer delegate : this.delegates) {
			delegate.addCorsMappings(registry);
		}
	}

	private <T> T selectSingleInstance(List<T> instances, Class<T> instanceType) {
		if (instances.size() > 1) {
			throw new IllegalStateException(
					"Only one [" + instanceType + "] was expected but multiple instances were provided: " + instances);
		}
		else if (instances.size() == 1) {
			return instances.get(0);
		}
		else {
			return null;
		}
	}

	@Override
	public MessageCodesResolver getMessageCodesResolver() {
		List<MessageCodesResolver> candidates = new ArrayList<MessageCodesResolver>();
		for (WebMvcConfigurer configurer : this.delegates) {
			MessageCodesResolver messageCodesResolver = configurer.getMessageCodesResolver();
			if (messageCodesResolver != null) {
				candidates.add(messageCodesResolver);
			}
		}
		return selectSingleInstance(candidates, MessageCodesResolver.class);
	}

}
