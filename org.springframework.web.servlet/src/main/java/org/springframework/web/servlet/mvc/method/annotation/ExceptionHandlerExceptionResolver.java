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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;
import org.springframework.web.method.annotation.support.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.DefaultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.support.HttpEntityMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.support.ModelAndViewMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.support.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletRequestMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletResponseMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletWebArgumentResolverAdapter;
import org.springframework.web.servlet.mvc.method.annotation.support.ViewMethodReturnValueHandler;

/**
 * An {@link AbstractHandlerMethodExceptionResolver} that supports using {@link ExceptionHandler}-annotated methods
 * to resolve exceptions.
 *
 * <p>{@link ExceptionHandlerMethodResolver} is a key contributing class that stores method-to-exception mappings extracted
 * from {@link ExceptionHandler} annotations or from the list of method arguments on the exception-handling method.
 * {@link ExceptionHandlerMethodResolver} assists with actually locating a method for a thrown exception.
 *
 * <p>Once located the invocation of the exception-handling method is done using much of the same classes
 * used for {@link RequestMapping} methods, which is described under {@link RequestMappingHandlerAdapter}.
 *
 * <p>See {@link ExceptionHandler} for information on supported method arguments and return values for
 * exception-handling methods.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ExceptionHandlerExceptionResolver extends AbstractHandlerMethodExceptionResolver implements
		InitializingBean {

	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

	private List<HttpMessageConverter<?>> messageConverters;

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerMethodResolvers =
		new ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver>();

	private HandlerMethodArgumentResolverComposite argumentResolvers;
	
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	/**
	 * Creates an instance of {@link ExceptionHandlerExceptionResolver}.
	 */
	public ExceptionHandlerExceptionResolver() {
		
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false); // See SPR-7316
		
		messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(stringHttpMessageConverter);
		messageConverters.add(new SourceHttpMessageConverter<Source>());
		messageConverters.add(new XmlAwareFormHttpMessageConverter());
	}

	/**
	 * Set one or more custom argument resolvers to use with {@link ExceptionHandler} methods. Custom argument resolvers
	 * are given a chance to resolve argument values ahead of the standard argument resolvers registered by default.
	 * <p>An existing {@link WebArgumentResolver} can either adapted with {@link ServletWebArgumentResolverAdapter}
	 * or preferably converted to a {@link HandlerMethodArgumentResolver} instead.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers= argumentResolvers;
	}	

	/**
	 * Set the argument resolvers to use with {@link ExceptionHandler} methods.
	 * This is an optional property providing full control over all argument resolvers in contrast to
	 * {@link #setCustomArgumentResolvers(List)}, which does not override default registrations.
	 * @param argumentResolvers argument resolvers for {@link ExceptionHandler} methods
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers != null) {
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.argumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * Set custom return value handlers to use to handle the return values of {@link ExceptionHandler} methods.
	 * Custom return value handlers are given a chance to handle a return value before the standard
	 * return value handlers registered by default.
	 * @param returnValueHandlers custom return value handlers for {@link ExceptionHandler} methods
	 */
	public void setCustomReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.customReturnValueHandlers = returnValueHandlers;
	}

	/**
	 * Set the {@link HandlerMethodReturnValueHandler}s to use to use with {@link ExceptionHandler} methods.
	 * This is an optional property providing full control over all return value handlers in contrast to
	 * {@link #setCustomReturnValueHandlers(List)}, which does not override default registrations.
	 * @param returnValueHandlers the return value handlers for {@link ExceptionHandler} methods
	 */
	public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers != null) {
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			this.returnValueHandlers.addHandlers(returnValueHandlers);
		}
	}
	
	/**
	 * Set the message body converters to use.
	 * <p>These converters are used to convert from and to HTTP requests and responses.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	public void afterPropertiesSet() {
		if (argumentResolvers == null) {
			argumentResolvers = new HandlerMethodArgumentResolverComposite();
			argumentResolvers.addResolvers(customArgumentResolvers);
			argumentResolvers.addResolvers(getDefaultArgumentResolvers());
		}
		if (returnValueHandlers == null) {
			returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			returnValueHandlers.addHandlers(customReturnValueHandlers);
			returnValueHandlers.addHandlers(getDefaultReturnValueHandlers(messageConverters));
		}
	}

	public static List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();
		resolvers.add(new ServletRequestMethodArgumentResolver());
		resolvers.add(new ServletResponseMethodArgumentResolver());
		return resolvers;
	}

	public static List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers(
			List<HttpMessageConverter<?>> messageConverters) {
		
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<HandlerMethodReturnValueHandler>();
		
		// Annotation-based handlers
		handlers.add(new RequestResponseBodyMethodProcessor(messageConverters));
		handlers.add(new ModelAttributeMethodProcessor(false));
		
		// Type-based handlers
		handlers.add(new ModelAndViewMethodReturnValueHandler());
		handlers.add(new ModelMethodProcessor());
		handlers.add(new ViewMethodReturnValueHandler());
		handlers.add(new HttpEntityMethodProcessor(messageConverters));
		
		// Default handler
		handlers.add(new DefaultMethodReturnValueHandler());
		
		return handlers;
	}

	/**
	 * Find an @{@link ExceptionHandler} method and invoke it to handle the 
	 * raised exception.
	 */
	@Override
	protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request, 
														   HttpServletResponse response,
														   HandlerMethod handlerMethod, 
														   Exception exception) {
		if (handlerMethod == null) {
			return null;
		}
		
		ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception);
		if (exceptionHandlerMethod == null) {
			return null;
		}

		exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);

		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking @ExceptionHandler method: " + exceptionHandlerMethod);
			}
			exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, exception);
		}
		catch (Exception invocationEx) {
			logger.error("Failed to invoke @ExceptionHandler method: " + exceptionHandlerMethod, invocationEx);
			return null;
		}
		
		if (!mavContainer.isResolveView()) {
			return new ModelAndView();
		}
		else {
			ModelAndView mav = new ModelAndView().addAllObjects(mavContainer.getModel());
			mav.setViewName(mavContainer.getViewName());
			if (!mavContainer.isViewReference()) {
				mav.setView((View) mavContainer.getView());
			}
			return mav;				
		}
	}

	/**
	 * Find the @{@link ExceptionHandler} method for the given exception.
	 * The default implementation searches @{@link ExceptionHandler} methods 
	 * in the class hierarchy of the method that raised the exception.
	 * @param handlerMethod the method where the exception was raised
	 * @param exception the raised exception
	 * @return a method to handle the exception, or {@code null}
	 */
	protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
		Class<?> handlerType = handlerMethod.getBeanType();
		Method method = getExceptionHandlerMethodResolver(handlerType).resolveMethod(exception);
		return (method != null) ? new ServletInvocableHandlerMethod(handlerMethod.getBean(), method) : null;
	}

	/**
	 * Return a method resolver for the given handler type, never {@code null}.
	 */
	private ExceptionHandlerMethodResolver getExceptionHandlerMethodResolver(Class<?> handlerType) {
		ExceptionHandlerMethodResolver resolver = this.exceptionHandlerMethodResolvers.get(handlerType);
		if (resolver == null) {
			resolver = new ExceptionHandlerMethodResolver(handlerType);
			this.exceptionHandlerMethodResolvers.put(handlerType, resolver);
		}
		return resolver;
	}

}
