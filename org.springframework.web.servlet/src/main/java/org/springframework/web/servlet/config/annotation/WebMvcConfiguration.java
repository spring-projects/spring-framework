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
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.MappedInterceptor;
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
 * Provides default configuration for Spring MVC applications. Registers Spring MVC infrastructure components to be
 * detected by the {@link DispatcherServlet}. Further below is a list of registered instances. This configuration is
 * enabled through the {@link EnableWebMvc} annotation.
 *
 * <p>A number of options are available for customizing the default configuration provided by this class.
 * See {@link EnableWebMvc} and {@link WebMvcConfigurer} for details.
 *
 * <p>Registers these handler mappings:
 * <ul>
 * 	<li>{@link RequestMappingHandlerMapping} ordered at 0 for mapping requests to annotated controller methods.
 * 	<li>{@link SimpleUrlHandlerMapping} ordered at 1 to map URL paths directly to view names.
 * 	<li>{@link BeanNameUrlHandlerMapping} ordered at 2 to map URL paths to controller bean names.
 * 	<li>{@link SimpleUrlHandlerMapping} ordered at {@code Integer.MAX_VALUE-1} to serve static resource requests.
 * 	<li>{@link SimpleUrlHandlerMapping} ordered at {@code Integer.MAX_VALUE} to forward requests to the default servlet.
 * </ul>
 *
 * <p><strong>Note:</strong> that the SimpleUrlHandlerMapping instances above will have empty URL maps and
 * hence no effect until explicitly configured via one of the {@link WebMvcConfigurer} callbacks.
 *
 * <p>Registers these handler adapters:
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
 * <p>Registers the following others:
 * <ul>
 * 	<li>{@link FormattingConversionService} for use with annotated controller methods and the spring:eval JSP tag.
 * 	<li>{@link Validator} for validating model attributes on annotated controller methods.
 * </ul>
 *
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@Configuration
class WebMvcConfiguration implements ApplicationContextAware, ServletContextAware {

	private final WebMvcConfigurerComposite configurers = new WebMvcConfigurerComposite();

	private ServletContext servletContext;

	private ApplicationContext applicationContext;

	@Autowired(required = false)
	public void setConfigurers(List<WebMvcConfigurer> configurers) {
		this.configurers.addWebMvcConfigurers(configurers);
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
		mapping.setMappedInterceptors(getMappedInterceptors());
		mapping.setOrder(0);
		return mapping;
	}

	private MappedInterceptor[] getMappedInterceptors() {
		InterceptorConfigurer configurer = new InterceptorConfigurer();
		configurers.configureInterceptors(configurer);
		configurer.addInterceptor(new ConversionServiceExposingInterceptor(conversionService()));
		return configurer.getInterceptors();
	}

	@Bean
	public HandlerMapping viewControllerHandlerMapping() {
		ViewControllerConfigurer configurer = new ViewControllerConfigurer();
		configurer.setOrder(1);
		configurers.configureViewControllers(configurer);
		
		SimpleUrlHandlerMapping handlerMapping = configurer.getHandlerMapping();
		handlerMapping.setMappedInterceptors(getMappedInterceptors());
		return handlerMapping;
	}

	@Bean
	public BeanNameUrlHandlerMapping beanNameHandlerMapping() {
		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
		mapping.setOrder(2);
		mapping.setMappedInterceptors(getMappedInterceptors());
		return mapping;
	}

	@Bean
	public HandlerMapping resourceHandlerMapping() {
		ResourceConfigurer configurer = new ResourceConfigurer(applicationContext, servletContext);
		configurer.setOrder(Integer.MAX_VALUE-1);
		configurers.configureResourceHandling(configurer);
		return configurer.getHandlerMapping();
	}

	@Bean
	public HandlerMapping defaultServletHandlerMapping() {
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(servletContext);
		configurers.configureDefaultServletHandling(configurer);
		return configurer.getHandlerMapping();
	}

	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(conversionService());
		bindingInitializer.setValidator(validator());
		adapter.setWebBindingInitializer(bindingInitializer);

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();
		configurers.addArgumentResolvers(argumentResolvers);
		adapter.setCustomArgumentResolvers(argumentResolvers);

		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();
		configurers.addReturnValueHandlers(returnValueHandlers);
		adapter.setCustomReturnValueHandlers(returnValueHandlers);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		configurers.configureMessageConverters(converters);
		if (converters.size() == 0) {
			addDefaultHttpMessageConverters(converters);
		}
		adapter.setMessageConverters(converters);

		return adapter;
	}

	@Bean(name="mvcConversionService")
	public FormattingConversionService conversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		configurers.addFormatters(conversionService);
		return conversionService;
	}

	@Bean(name="mvcValidator")
	public Validator validator() {
		Validator validator = configurers.getValidator();
		if (validator != null) {
			return validator;
		}
		else if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
			Class<?> clazz;
			try {
				String className = "org.springframework.validation.beanvalidation.LocalValidatorFactoryBean";
				clazz = ClassUtils.forName(className, WebMvcConfiguration.class.getClassLoader());
			} catch (ClassNotFoundException e) {
				throw new BeanInitializationException("Could not find default validator");
			} catch (LinkageError e) {
				throw new BeanInitializationException("Could not find default validator");
			}
			return (Validator) BeanUtils.instantiate(clazz);
		}
		else {
			return new Validator() {
				public void validate(Object target, Errors errors) {
				}

				public boolean supports(Class<?> clazz) {
					return false;
				}
			};
		}
	}

	private void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> converters) {
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setWriteAcceptCharset(false);

		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(stringConverter);
		converters.add(new ResourceHttpMessageConverter());
		converters.add(new SourceHttpMessageConverter<Source>());
		converters.add(new XmlAwareFormHttpMessageConverter());

		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent("javax.xml.bind.Binder", classLoader)) {
			converters.add(new Jaxb2RootElementHttpMessageConverter());
		}
		if (ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", classLoader)) {
			converters.add(new MappingJacksonHttpMessageConverter());
		}
		if (ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", classLoader)) {
			converters.add(new AtomFeedHttpMessageConverter());
			converters.add(new RssChannelHttpMessageConverter());
		}
	}

	@Bean
	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	@Bean
	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	@Bean
	public HandlerExceptionResolver handlerExceptionResolver() throws Exception {
		List<HandlerExceptionResolver> resolvers = new ArrayList<HandlerExceptionResolver>();
		configurers.configureHandlerExceptionResolvers(resolvers);

		if (resolvers.size() == 0) {
			resolvers.add(createExceptionHandlerExceptionResolver());
			resolvers.add(new ResponseStatusExceptionResolver());
			resolvers.add(new DefaultHandlerExceptionResolver());
		}
		
		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		composite.setOrder(0);
		composite.setExceptionResolvers(resolvers);
		return composite;
	}

	private HandlerExceptionResolver createExceptionHandlerExceptionResolver() throws Exception {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		resolver.setOrder(0);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		configurers.configureMessageConverters(converters);
		if (converters.size() == 0) {
			addDefaultHttpMessageConverters(converters);
		}
		resolver.setMessageConverters(converters);

		resolver.afterPropertiesSet();
		return resolver;
	}

}
