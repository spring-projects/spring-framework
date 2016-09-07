/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.validation.Validator;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;


/**
 * Supports the invocation of {@code @RequestMapping} methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, BeanFactoryAware, InitializingBean {

	private static Log logger = LogFactory.getLog(RequestMappingHandlerAdapter.class);


	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private List<HandlerMethodArgumentResolver> argumentResolvers;

	private final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(10);

	private ConversionService conversionService = new DefaultFormattingConversionService();

	private Validator validator;

	private ConfigurableBeanFactory beanFactory;

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache = new ConcurrentHashMap<>(64);



	public RequestMappingHandlerAdapter() {
		this.messageConverters.add(new CodecHttpMessageConverter<>(new ByteBufferDecoder()));
		this.messageConverters.add(new CodecHttpMessageConverter<>(new StringDecoder()));
	}


	/**
	 * Provide custom argument resolvers without overriding the built-in ones.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * Return the custom argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * Configure the complete list of supported argument types thus overriding
	 * the resolvers that would otherwise be configured by default.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.argumentResolvers = new ArrayList<>(resolvers);
	}

	/**
	 * Return the configured argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers;
	}

	/**
	 * Configure message converters to read the request body with.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters.clear();
		this.messageConverters.addAll(messageConverters);
	}

	/**
	 * Return the configured message converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Configure a ConversionService for type conversion of controller method
	 * arguments as well as for converting from different async types to
	 * {@code Flux} and {@code Mono}.
	 *
	 * TODO: this may be replaced by DataBinder
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Return the configured ConversionService.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Configure a Validator for validation of controller method arguments such
	 * as {@code @RequestBody}.
	 *
	 * TODO: this may be replaced by DataBinder
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Return the configured Validator.
	 */
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * A {@link ConfigurableBeanFactory} is expected for resolving expressions
	 * in method argument default values.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	public ConfigurableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.argumentResolvers == null) {
			this.argumentResolvers = initArgumentResolvers();
		}
	}

	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Annotation-based argument resolution
		ConversionService cs = getConversionService();
		resolvers.add(new RequestParamMethodArgumentResolver(cs, getBeanFactory(), false));
		resolvers.add(new RequestParamMapMethodArgumentResolver());
		resolvers.add(new PathVariableMethodArgumentResolver(cs, getBeanFactory()));
		resolvers.add(new PathVariableMapMethodArgumentResolver());
		resolvers.add(new RequestBodyArgumentResolver(getMessageConverters(), cs, getValidator()));
		resolvers.add(new RequestHeaderMethodArgumentResolver(cs, getBeanFactory()));
		resolvers.add(new RequestHeaderMapMethodArgumentResolver());
		resolvers.add(new CookieValueMethodArgumentResolver(cs, getBeanFactory()));
		resolvers.add(new ExpressionValueMethodArgumentResolver(cs, getBeanFactory()));
		resolvers.add(new SessionAttributeMethodArgumentResolver(cs, getBeanFactory()));
		resolvers.add(new RequestAttributeMethodArgumentResolver(cs , getBeanFactory()));

		// Type-based argument resolution
		resolvers.add(new ModelArgumentResolver());

		// Custom resolvers
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		// Catch-all
		resolvers.add(new RequestParamMethodArgumentResolver(cs, getBeanFactory(), true));
		return resolvers;
	}

	@Override
	public boolean supports(Object handler) {
		return HandlerMethod.class.equals(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		invocable.setHandlerMethodArgumentResolvers(getArgumentResolvers());
		ModelMap model = new ExtendedModelMap();
		return invocable.invokeForRequest(exchange, model)
				.map(result -> result.setExceptionHandler(ex -> handleException(ex, handlerMethod, exchange)))
				.otherwise(ex -> handleException(ex, handlerMethod, exchange));
	}

	private Mono<HandlerResult> handleException(Throwable ex, HandlerMethod handlerMethod,
			ServerWebExchange exchange) {

		if (ex instanceof Exception) {
			InvocableHandlerMethod invocable = findExceptionHandler(handlerMethod, (Exception) ex);
			if (invocable != null) {
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Invoking @ExceptionHandler method: " + invocable);
					}
					invocable.setHandlerMethodArgumentResolvers(getArgumentResolvers());
					ExtendedModelMap errorModel = new ExtendedModelMap();
					return invocable.invokeForRequest(exchange, errorModel, ex);
				}
				catch (Exception invocationEx) {
					if (logger.isErrorEnabled()) {
						logger.error("Failed to invoke @ExceptionHandler method: " + invocable, invocationEx);
					}
				}
			}
		}
		return Mono.error(ex);
	}

	protected InvocableHandlerMethod findExceptionHandler(HandlerMethod handlerMethod, Exception exception) {
		if (handlerMethod == null) {
			return null;
		}
		Class<?> handlerType = handlerMethod.getBeanType();
		ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(handlerType);
		if (resolver == null) {
			resolver = new ExceptionHandlerMethodResolver(handlerType);
			this.exceptionHandlerCache.put(handlerType, resolver);
		}
		Method method = resolver.resolveMethod(exception);
		return (method != null ? new InvocableHandlerMethod(handlerMethod.getBean(), method) : null);
	}

}