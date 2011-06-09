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
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.transform.Source;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * A base class that provides default configuration for Spring MVC applications by registering Spring MVC 
 * infrastructure components to be detected by the {@link DispatcherServlet}. Typically applications should not
 * have to start by extending this class. A much easier place to start is to annotate your @{@link Configuration}
 * class with @{@link EnableWebMvc}. See @{@link EnableWebMvc} and {@link WebMvcConfigurer}.
 * 
 * <p>If using @{@link EnableWebMvc} and extending from {@link WebMvcConfigurerAdapter} does not give you the level 
 * of flexibility you need, consider extending directly from this class instead. Remember to add @{@link Configuration}
 * to you subclass and @{@link Bean} to any @{@link Bean} methods you choose to override. A few example reasons for 
 * extending this class include providing a custom {@link MessageCodesResolver}, changing the order of 
 * {@link HandlerMapping} instances, plugging in a variant of any of the beans provided by this class, and so on.
 * 
 * <p>This class registers the following {@link HandlerMapping}s:</p>
 * <ul>
 * 	<li>{@link RequestMappingHandlerMapping} ordered at 0 for mapping requests to annotated controller methods.
 * 	<li>{@link SimpleUrlHandlerMapping} ordered at 1 to map URL paths directly to view names.
 * 	<li>{@link BeanNameUrlHandlerMapping} ordered at 2 to map URL paths to controller bean names.
 * 	<li>{@link SimpleUrlHandlerMapping} ordered at {@code Integer.MAX_VALUE-1} to serve static resource requests.
 * 	<li>{@link SimpleUrlHandlerMapping} ordered at {@code Integer.MAX_VALUE} to forward requests to the default servlet.
 * </ul>
 *
 * <p>Registers {@link HandlerAdapter}s:
 * <ul>
 * 	<li>{@link RequestMappingHandlerAdapter} for processing requests using annotated controller methods.
 * 	<li>{@link HttpRequestHandlerAdapter} for processing requests with {@link HttpRequestHandler}s.
 * 	<li>{@link SimpleControllerHandlerAdapter} for processing requests with interface-based {@link Controller}s.
 * </ul>
 *
 * <p>Registers a {@link HandlerExceptionResolverComposite} with this chain of exception resolvers:
 * <ul>
 * 	<li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through @{@link ExceptionHandler} methods.
 * 	<li>{@link ResponseStatusExceptionResolver} for exceptions annotated with @{@link ResponseStatus}.
 * 	<li>{@link DefaultHandlerExceptionResolver} for resolving known Spring exception types
 * </ul>
 *
 * <p>Registers the following other instances:
 * <ul>
 * 	<li>{@link FormattingConversionService} for use with annotated controller methods and the spring:eval JSP tag.
 * 	<li>{@link Validator} for validating model attributes on annotated controller methods.
 * </ul>
 *
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 * @see WebMvcConfigurerAdapter
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {

	private ServletContext servletContext;

	private ApplicationContext applicationContext;

	private List<Object> interceptors;

	private List<HttpMessageConverter<?>> messageConverters;

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * Returns a {@link RequestMappingHandlerMapping} ordered at 0 for mapping requests to annotated controllers.
	 */
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
		mapping.setInterceptors(getInterceptors());
		mapping.setOrder(0);
		return mapping;
	}
	
	/**
	 * Provides access to the shared handler interceptors used to configure {@link HandlerMapping} instances with.
	 * This method cannot be overridden, use {@link #configureInterceptors(InterceptorConfigurer)} instead. 
	 */
	protected final Object[] getInterceptors() {
		if (interceptors == null) {
			InterceptorConfigurer configurer = new InterceptorConfigurer();
			configureInterceptors(configurer);
			configurer.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService()));
			interceptors = configurer.getInterceptors();
		}
		return interceptors.toArray();
	}
	
	/**
	 * Override this method to configure handler interceptors including interceptors mapped to path patterns.
	 * @see InterceptorConfigurer
	 */
	protected void configureInterceptors(InterceptorConfigurer configurer) {
	}

	/**
	 * Returns a {@link SimpleUrlHandlerMapping} ordered at 1 to map URL paths directly to view names.
	 * To configure view controllers see {@link #configureViewControllers(ViewControllerConfigurer)}. 
	 */
	@Bean
	public SimpleUrlHandlerMapping viewControllerHandlerMapping() {
		ViewControllerConfigurer configurer = new ViewControllerConfigurer();
		configurer.setOrder(1);
		configureViewControllers(configurer);
		
		SimpleUrlHandlerMapping handlerMapping = configurer.getHandlerMapping();
		handlerMapping.setInterceptors(getInterceptors());
		return handlerMapping;
	}

	/**
	 * Override this method to configure view controllers. View controllers provide a direct mapping between a 
	 * URL path and view name. This is useful when serving requests that don't require application-specific 
	 * controller logic and can be forwarded directly to a view for rendering.
	 * @see ViewControllerConfigurer
	 */
	protected void configureViewControllers(ViewControllerConfigurer configurer) {
	}
	
	/**
	 * Returns a {@link BeanNameUrlHandlerMapping} ordered at 2 to map URL paths to controller bean names.
	 */
	@Bean
	public BeanNameUrlHandlerMapping beanNameHandlerMapping() {
		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
		mapping.setOrder(2);
		mapping.setInterceptors(getInterceptors());
		return mapping;
	}

	/**
	 * Returns a {@link SimpleUrlHandlerMapping} ordered at Integer.MAX_VALUE-1 to serve static resource requests.
	 * To configure resource handling, see {@link #configureResourceHandling(ResourceConfigurer)}.
	 */
	@Bean
	public SimpleUrlHandlerMapping resourceHandlerMapping() {
		ResourceConfigurer configurer = new ResourceConfigurer(applicationContext, servletContext);
		configurer.setOrder(Integer.MAX_VALUE-1);
		configureResourceHandling(configurer);
		return configurer.getHandlerMapping();
	}

	/**
	 * Override this method to configure serving static resources such as images and css files through Spring MVC.
	 * @see ResourceConfigurer
	 */
	protected void configureResourceHandling(ResourceConfigurer configurer) {
	}

	/**
	 * Returns a {@link SimpleUrlHandlerMapping} ordered at Integer.MAX_VALUE to serve static resources by 
	 * forwarding to the Servlet container's default servlet. To configure default servlet handling see
	 * {@link #configureDefaultServletHandling(DefaultServletHandlerConfigurer)}.  
	 */
	@Bean
	public SimpleUrlHandlerMapping defaultServletHandlerMapping() {
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(servletContext);
		configureDefaultServletHandling(configurer);
		return configurer.getHandlerMapping();
	}

	/**
	 * Override this method to configure serving static resources through the Servlet container's default Servlet.
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * Returns a {@link RequestMappingHandlerAdapter} for processing requests using annotated controller methods.
	 * Also see {@link #initWebBindingInitializer()} for configuring data binding globally.
	 */
	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		ConfigurableWebBindingInitializer webBindingInitializer = new ConfigurableWebBindingInitializer();
		webBindingInitializer.setConversionService(mvcConversionService());
		webBindingInitializer.setValidator(mvcValidator());
		extendWebBindingInitializer(webBindingInitializer);
		
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
		adapter.setMessageConverters(getMessageConverters());
		adapter.setWebBindingInitializer(webBindingInitializer);
		return adapter;
	}

	/**
	 * Override this method to customize the {@link ConfigurableWebBindingInitializer} the 
	 * {@link RequestMappingHandlerAdapter} is configured with.
	 */
	protected void extendWebBindingInitializer(ConfigurableWebBindingInitializer webBindingInitializer) {
	}

	/**
	 * Provides access to the shared {@link HttpMessageConverter}s used by the 
	 * {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}. 
	 * This method cannot be extended directly, use {@link #configureMessageConverters(List)} add custom converters. 
	 * Also see {@link #addDefaultHttpMessageConverters(List)} to easily add a set of default converters.
	 */
	protected final List<HttpMessageConverter<?>> getMessageConverters() {
		if (messageConverters == null) {
			messageConverters = new ArrayList<HttpMessageConverter<?>>();
			configureMessageConverters(messageConverters);
			if (messageConverters.isEmpty()) {
				addDefaultHttpMessageConverters(messageConverters);
			}
		}
		return messageConverters;
	}

	/**
	 * Override this method to add custom {@link HttpMessageConverter}s to use with 
	 * the {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}.
	 * If any converters are added, default converters will not be added automatically.
	 * See {@link #addDefaultHttpMessageConverters(List)} for adding default converters to the list.
	 * @param messageConverters the list to add converters to
	 */
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * A method available to subclasses for adding default {@link HttpMessageConverter}s. 
	 * @param messageConverters the list to add converters to
	 */
	protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setWriteAcceptCharset(false);

		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(stringConverter);
		messageConverters.add(new ResourceHttpMessageConverter());
		messageConverters.add(new SourceHttpMessageConverter<Source>());
		messageConverters.add(new XmlAwareFormHttpMessageConverter());

		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent("javax.xml.bind.Binder", classLoader)) {
			messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
		}
		if (ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", classLoader)) {
			messageConverters.add(new MappingJacksonHttpMessageConverter());
		}
		if (ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", classLoader)) {
			messageConverters.add(new AtomFeedHttpMessageConverter());
			messageConverters.add(new RssChannelHttpMessageConverter());
		}
	}
	
	/**
	 * Returns a {@link FormattingConversionService} for use with annotated controller methods and the 
	 * {@code spring:eval} JSP tag.
	 */
	@Bean
	public FormattingConversionService mvcConversionService() {
		return new DefaultFormattingConversionService();
	}

	/**
	 * Returns {@link Validator} for validating {@code @ModelAttribute} and {@code @RequestBody} arguments of 
	 * annotated controller methods. If a JSR-303 implementation is available on the classpath, the returned
	 * instance is LocalValidatorFactoryBean. Otherwise a no-op validator is returned.
	 */
	@Bean
	public Validator mvcValidator() {
		if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
			Class<?> clazz;
			try {
				String className = "org.springframework.validation.beanvalidation.LocalValidatorFactoryBean";
				clazz = ClassUtils.forName(className, WebMvcConfigurationSupport.class.getClassLoader());
			} catch (ClassNotFoundException e) {
				throw new BeanInitializationException("Could not find default validator");
			} catch (LinkageError e) {
				throw new BeanInitializationException("Could not find default validator");
			}
			return (Validator) BeanUtils.instantiate(clazz);
		}
		else {
			return new Validator() {
				public boolean supports(Class<?> clazz) {
					return false;
				}
				public void validate(Object target, Errors errors) {
				}
			};
		}
	}

	/**
	 * Returns a {@link HttpRequestHandlerAdapter} for processing requests with {@link HttpRequestHandler}s.
	 */
	@Bean
	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	/**
	 * Returns a {@link SimpleControllerHandlerAdapter} for processing requests with interface-based controllers.
	 */
	@Bean
	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	/**
	 * Returns a {@link HandlerExceptionResolverComposite} with this chain of exception resolvers:
	 * <ul>
	 * 	<li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through @{@link ExceptionHandler} methods.
	 * 	<li>{@link ResponseStatusExceptionResolver} for exceptions annotated with @{@link ResponseStatus}.
	 * 	<li>{@link DefaultHandlerExceptionResolver} for resolving known Spring exception types
	 * </ul>
	 */
	@Bean
	public HandlerExceptionResolverComposite handlerExceptionResolver() throws Exception {
		ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver = new ExceptionHandlerExceptionResolver();
		exceptionHandlerExceptionResolver.setMessageConverters(getMessageConverters());
		exceptionHandlerExceptionResolver.afterPropertiesSet();

		List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<HandlerExceptionResolver>();
		exceptionResolvers.add(exceptionHandlerExceptionResolver);
		exceptionResolvers.add(new ResponseStatusExceptionResolver());
		exceptionResolvers.add(new DefaultHandlerExceptionResolver());

		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		composite.setOrder(0);
		composite.setExceptionResolvers(exceptionResolvers);
		return composite;
	}

}
