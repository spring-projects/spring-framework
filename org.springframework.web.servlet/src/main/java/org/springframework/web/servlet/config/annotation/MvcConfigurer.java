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

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;

import com.sun.corba.se.impl.presentation.rmi.ExceptionHandler;

/**
 * Defines options for customizing or adding to the default Spring MVC configuration enabled through the use
 * of @{@link EnableMvcConfiguration}. The @{@link Configuration} class annotated with @{@link EnableMvcConfiguration}
 * is the most obvious place to implement this interface. However all @{@link Configuration} classes and more generally
 * all Spring beans that implement this interface will be detected at startup and given a chance to customize Spring
 * MVC configuration provided it is enabled through @{@link EnableMvcConfiguration}.
 *
 * <p>Implementations of this interface will find it convenient to extend {@link MvcConfigurerSupport} that
 * provides default method implementations and allows overriding only methods of interest.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author David Syer
 * @since 3.1
 */
public interface MvcConfigurer {

	/**
	 * Register application-specific {@link Converter}s and {@link Formatter}s for use in Spring MVC.
	 */
	void registerFormatters(FormatterRegistry formatterRegistry);

	/**
	 * Customize the list of {@link HttpMessageConverter}s to use when resolving method arguments or handling
	 * return  values from @{@link RequestMapping} and @{@link ExceptionHandler} methods.
	 * @param converters the list of converters, initially populated with the default set of converters
	 */
	void configureMessageConverters(List<HttpMessageConverter<?>> converters);

	/**
	 * Provide a custom {@link Validator} type replacing the one that would be created by default otherwise. If this
	 * method returns {@code null}, and assuming a JSR-303 implementation is available on the classpath, a validator
	 * of type {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean} is created by default.
	 */
	Validator getValidator();

	/**
	 * Add custom {@link HandlerMethodArgumentResolver}s to use for resolving argument values
	 * on @{@link RequestMapping} and @{@link ExceptionHandler} methods.
	 * @param argumentResolvers the list of custom converters, initially empty
	 */
	void addCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers);

	/**
	 * Add custom {@link HandlerMethodReturnValueHandler}s to use for handling return values
	 * from @{@link RequestMapping} and @{@link ExceptionHandler} methods.
	 * @param returnValueHandlers the list of custom handlers, initially empty
	 */
	void addCustomReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers);

	/**
	 * Customize the list of {@link HandlerExceptionResolver}s to use for handling controller exceptions.
	 * @param exceptionResolvers the list of resolvers, initially populated with the default set of resolvers
	 */
	void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers);

	/**
	 * Add Spring MVC interceptors. Interceptors can be of type {@link HandlerInterceptor} or
	 * {@link WebRequestInterceptor}.  They allow requests to be pre/post processed before/after controller
	 * invocation. Interceptors can be registered to apply to all requests or limited to a set of path patterns.
	 * @see InterceptorConfigurer
	 */
	void addInterceptors(InterceptorConfigurer interceptorConfigurer);

	/**
	 * Map URL paths to view names. This is convenient when a request can be rendered without a controller.
	 */
	void addViewControllers(ViewControllerConfigurer viewControllerConfigurer);

	/**
	 * Configure a handler for serving static resources such as images, js, and, css files through Spring MVC
	 * including setting cache headers optimized for efficient loading in a web browser. Resources can be served
	 * out of locations under web application root, from the classpath, and others.
	 */
	void configureResourceHandling(ResourceConfigurer resourceConfigurer);

	/**
	 * Configure a handler for delegating unhandled requests by forwarding to the Servlet container's default
	 * servlet. This is commonly used when the {@link DispatcherServlet} is mapped to "/", which results in
	 * cleaner URLs (without a servlet prefix) but may need to still allow some requests (e.g. static resources)
	 * to be handled by the Servlet container's default servlet.
	 */
	void configureDefaultServletHandling(DefaultServletHandlerConfigurer handlerConfigurer);

}
