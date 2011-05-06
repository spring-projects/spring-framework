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

import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * An abstract class with empty method implementations of the {@link MvcConfigurer} interface for a simplified
 * implementation of {@link MvcConfigurer} so that subclasses can override selected methods only.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class MvcConfigurerSupport implements MvcConfigurer {

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void registerFormatters(FormatterRegistry formatterRegistry) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void configureValidator(Validator validator) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation returns {@code null}
	 */
	public Validator getValidator() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void addCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void addInterceptors(InterceptorConfigurer interceptorConfigurer) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void addViewControllers(ViewControllerConfigurer viewControllerConfigurer) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void configureResourceHandling(ResourceConfigurer resourceConfigurer) {
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation is empty.
	 */
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer handlerConfigurer) {
	}

}