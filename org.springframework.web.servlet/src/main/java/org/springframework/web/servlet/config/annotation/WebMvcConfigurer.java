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

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.sun.corba.se.impl.presentation.rmi.ExceptionHandler;

/**
 * Defines configuration callback methods for customizing the default Spring MVC code-based configuration enabled 
 * through @{@link EnableWebMvc}. 
 * 
 * <p>Classes annotated with @{@link EnableWebMvc} can implement this interface in order to be called back and 
 * given a chance to customize the default configuration. The most convenient way to implement this interface 
 * is to extend {@link WebMvcConfigurerAdapter}, which provides empty method implementations.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author David Syer
 * @since 3.1
 */
public interface WebMvcConfigurer {

	/**
	 * Add {@link Converter}s and {@link Formatter}s in addition to the ones registered by default.
	 */
	void addFormatters(FormatterRegistry registry);

	/**
	 * Configure the list of {@link HttpMessageConverter}s to use when resolving method arguments or handling
	 * return values in @{@link RequestMapping} and @{@link ExceptionHandler} methods. 
	 * Adding converters to the list turns off the default converters that would otherwise be registered by default.
	 * @param converters a list to add message converters to; initially an empty list.
	 */
	void configureMessageConverters(List<HttpMessageConverter<?>> converters);

	/**
	 * Provide a custom {@link Validator} type replacing the one that would be created by default otherwise. If this
	 * method returns {@code null}, and assuming a JSR-303 implementation is available on the classpath, a validator
	 * of type {@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean} is created by default.
	 */
	Validator getValidator();

	/**
	 * Add custom {@link HandlerMethodArgumentResolver}s to use in addition to the ones registered by default.
	 * <p>Custom argument resolvers are invoked before built-in resolvers except for those that rely on the presence 
	 * of annotations (e.g. {@code @RequestParameter}, {@code @PathVariable}, etc.). The latter can be customized 
	 * by configuring the {@link RequestMappingHandlerAdapter} directly. 
	 * @param argumentResolvers the list of custom converters; initially an empty list.
	 */
	void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers);

	/**
	 * Add custom {@link HandlerMethodReturnValueHandler}s in addition to the ones registered by default.
	 * <p>Custom return value handlers are invoked before built-in ones except for those that rely on the presence 
	 * of annotations (e.g. {@code @ResponseBody}, {@code @ModelAttribute}, etc.). The latter can be customized
	 * by configuring the {@link RequestMappingHandlerAdapter} directly.
	 * @param returnValueHandlers the list of custom handlers; initially an empty list.
	 */
	void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers);

	/**
	 * Configure the list of {@link HandlerExceptionResolver}s to use for handling unresolved controller exceptions.
	 * Adding resolvers to the list turns off the default resolvers that would otherwise be registered by default.
	 * @param exceptionResolvers a list to add exception resolvers to; initially an empty list.
	 */
	void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers);

	/**
	 * Add Spring MVC lifecycle interceptors for pre- and post-processing of controller method invocations.
	 * Interceptors can be registered to apply to all requests or to a set of URL path patterns.
	 * @see InterceptorRegistry
	 */
	void addInterceptors(InterceptorRegistry registry);

	/**
	 * Add view controllers to create a direct mapping between a URL path and view name. This is useful when
	 * you just want to forward the request to a view such as a JSP without the need for controller logic. 
	 * @see ViewControllerRegistry
	 */
	void addViewControllers(ViewControllerRegistry registry);

	/**
	 * Add resource handlers to use to serve static resources such as images, js, and, css files through 
	 * the Spring MVC {@link DispatcherServlet} including the setting of cache headers optimized for efficient 
	 * loading in a web browser. Resources can be served out of locations under web application root, 
	 * from the classpath, and others.
	 * @see ResourceHandlerRegistry
	 */
	void addResourceHandlers(ResourceHandlerRegistry registry);

	/**
	 * Configure a handler for delegating unhandled requests by forwarding to the Servlet container's "default"
	 * servlet. The use case for this is when the {@link DispatcherServlet} is mapped to "/" thus overriding 
	 * the Servlet container's default handling of static resources.
	 * @see DefaultServletHandlerConfigurer
	 */
	void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer);

}
