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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.core.codec.support.ByteBufferDecoder;
import org.springframework.core.codec.support.ByteBufferEncoder;
import org.springframework.core.codec.support.JacksonJsonDecoder;
import org.springframework.core.codec.support.JacksonJsonEncoder;
import org.springframework.core.codec.support.Jaxb2Decoder;
import org.springframework.core.codec.support.Jaxb2Encoder;
import org.springframework.core.codec.support.JsonObjectDecoder;
import org.springframework.core.codec.support.StringDecoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;


/**
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, BeanFactoryAware, InitializingBean {

	private static Log logger = LogFactory.getLog(RequestMappingHandlerAdapter.class);


	private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

	private ConversionService conversionService = new DefaultConversionService();

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);

	private ConfigurableBeanFactory beanFactory;



	/**
	 * Configure the complete list of supported argument types thus overriding
	 * the resolvers that would otherwise be configured by default.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.argumentResolvers.clear();
		this.argumentResolvers.addAll(resolvers);
	}

	/**
	 * Return the configured argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService() {
		return this.conversionService;
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
		return beanFactory;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (ObjectUtils.isEmpty(this.argumentResolvers)) {

			List<HttpMessageConverter<?>> converters = Arrays.asList(
					new CodecHttpMessageConverter<ByteBuffer>(new ByteBufferEncoder(), new ByteBufferDecoder()),
					new CodecHttpMessageConverter<String>(new StringEncoder(), new StringDecoder()),
					new CodecHttpMessageConverter<Object>(new Jaxb2Encoder(), new Jaxb2Decoder()),
					new CodecHttpMessageConverter<Object>(new JacksonJsonEncoder(),
							new JacksonJsonDecoder(new JsonObjectDecoder())));

			// Annotation-based argument resolution
			ConversionService cs = getConversionService();
			this.argumentResolvers.add(new RequestParamMethodArgumentResolver(cs, getBeanFactory(), false));
			this.argumentResolvers.add(new RequestParamMapMethodArgumentResolver());
			this.argumentResolvers.add(new PathVariableMethodArgumentResolver(cs, getBeanFactory()));
			this.argumentResolvers.add(new PathVariableMapMethodArgumentResolver());
			this.argumentResolvers.add(new RequestBodyArgumentResolver(converters, cs));
			this.argumentResolvers.add(new RequestHeaderMethodArgumentResolver(cs, getBeanFactory()));
			this.argumentResolvers.add(new RequestHeaderMapMethodArgumentResolver());
			this.argumentResolvers.add(new CookieValueMethodArgumentResolver(cs, getBeanFactory()));
			this.argumentResolvers.add(new ExpressionValueMethodArgumentResolver(cs, getBeanFactory()));
			this.argumentResolvers.add(new SessionAttributeMethodArgumentResolver(cs, getBeanFactory()));
			this.argumentResolvers.add(new RequestAttributeMethodArgumentResolver(cs , getBeanFactory()));

			// Type-based argument resolution
			this.argumentResolvers.add(new ModelArgumentResolver());

			// Catch-all
			this.argumentResolvers.add(new RequestParamMethodArgumentResolver(cs, getBeanFactory(), true));
		}
	}

	@Override
	public boolean supports(Object handler) {
		return HandlerMethod.class.equals(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		invocable.setHandlerMethodArgumentResolvers(this.argumentResolvers);
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