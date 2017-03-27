/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;

import static org.springframework.core.MethodIntrospector.selectMethods;

/**
 * Package-private class to assist {@link RequestMappingHandlerAdapter} with
 * resolving and caching {@code @InitBinder}, {@code @ModelAttribute}, and
 * {@code @ExceptionHandler} methods declared in the {@code @Controller} or in
 * {@code @ControllerAdvice} components.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ControllerMethodResolver {

	private static Log logger = LogFactory.getLog(ControllerMethodResolver.class);


	private final List<HandlerMethodArgumentResolver> argumentResolvers;

	private final List<SyncHandlerMethodArgumentResolver> initBinderArgumentResolvers;


	private final Map<Class<?>, Set<Method>> binderMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, Set<Method>> attributeMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);


	private final Map<ControllerAdviceBean, Set<Method>> binderAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, Set<Method>> attributeAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);


	ControllerMethodResolver(List<HandlerMethodArgumentResolver> argumentResolvers,
			List<SyncHandlerMethodArgumentResolver> initBinderArgumentResolvers,
			ApplicationContext applicationContext) {

		this.argumentResolvers = argumentResolvers;
		this.initBinderArgumentResolvers = initBinderArgumentResolvers;

		initControllerAdviceCaches(applicationContext);
	}

	private void initControllerAdviceCaches(ApplicationContext applicationContext) {
		if (applicationContext == null) {
			return;
		}
		if (logger.isInfoEnabled()) {
			logger.info("Looking for @ControllerAdvice: " + applicationContext);
		}

		List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
		AnnotationAwareOrderComparator.sort(beans);

		for (ControllerAdviceBean bean : beans) {
			Class<?> beanType = bean.getBeanType();
			Set<Method> attrMethods = selectMethods(beanType, ATTRIBUTE_METHODS);
			if (!attrMethods.isEmpty()) {
				this.attributeAdviceCache.put(bean, attrMethods);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @ModelAttribute methods in " + bean);
				}
			}
			Set<Method> binderMethods = selectMethods(beanType, BINDER_METHODS);
			if (!binderMethods.isEmpty()) {
				this.binderAdviceCache.put(bean, binderMethods);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @InitBinder methods in " + bean);
				}
			}
			ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
			if (resolver.hasExceptionMappings()) {
				this.exceptionHandlerAdviceCache.put(bean, resolver);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @ExceptionHandler methods in " + bean);
				}
			}
		}
	}


	/**
	 * Find {@code @InitBinder} methods from {@code @ControllerAdvice}
	 * components or from the same controller as the given request handling method.
	 */
	public List<SyncInvocableHandlerMethod> resolveInitBinderMethods(HandlerMethod handlerMethod) {

		List<SyncInvocableHandlerMethod> result = new ArrayList<>();
		Class<?> handlerType = handlerMethod.getBeanType();

		// Global methods first
		this.binderAdviceCache.entrySet().forEach(entry -> {
			if (entry.getKey().isApplicableToBeanType(handlerType)) {
				Object bean = entry.getKey().resolveBean();
				entry.getValue().forEach(method -> result.add(createBinderMethod(bean, method)));
			}
		});

		this.binderMethodCache
				.computeIfAbsent(handlerType, aClass -> selectMethods(handlerType, BINDER_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(createBinderMethod(bean, method));
				});

		return result;
	}

	private SyncInvocableHandlerMethod createBinderMethod(Object bean, Method method) {
		SyncInvocableHandlerMethod invocable = new SyncInvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.initBinderArgumentResolvers);
		return invocable;
	}

	/**
	 * Find {@code @ModelAttribute} methods from {@code @ControllerAdvice}
	 * components or from the same controller as the given request handling method.
	 */
	public List<InvocableHandlerMethod> resolveModelAttributeMethods(HandlerMethod handlerMethod) {

		List<InvocableHandlerMethod> result = new ArrayList<>();
		Class<?> handlerType = handlerMethod.getBeanType();

		// Global methods first
		this.attributeAdviceCache.entrySet().forEach(entry -> {
			if (entry.getKey().isApplicableToBeanType(handlerType)) {
				Object bean = entry.getKey().resolveBean();
				entry.getValue().forEach(method -> result.add(createHandlerMethod(bean, method)));
			}
		});

		this.attributeMethodCache
				.computeIfAbsent(handlerType, aClass -> selectMethods(handlerType, ATTRIBUTE_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(createHandlerMethod(bean, method));
				});

		return result;
	}

	private InvocableHandlerMethod createHandlerMethod(Object bean, Method method) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.argumentResolvers);
		return invocable;
	}

	/**
	 * Find a matching {@code @ExceptionHandler} method from
	 * {@code @ControllerAdvice} components or from the same controller as the
	 * given request handling method.
	 */
	public InvocableHandlerMethod resolveExceptionHandlerMethod(Throwable ex, HandlerMethod handlerMethod) {

		Class<?> handlerType = handlerMethod.getBeanType();

		ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache
				.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new);

		return Optional
				.ofNullable(resolver.resolveMethodByThrowable(ex))
				.map(method -> createHandlerMethod(handlerMethod.getBean(), method))
				.orElseGet(() ->
						this.exceptionHandlerAdviceCache.entrySet().stream()
								.map(entry -> {
									if (entry.getKey().isApplicableToBeanType(handlerType)) {
										Method method = entry.getValue().resolveMethodByThrowable(ex);
										if (method != null) {
											Object bean = entry.getKey().resolveBean();
											return createHandlerMethod(bean, method);
										}
									}
									return null;
								})
								.filter(Objects::nonNull)
								.findFirst()
								.orElse(null));
	}


	/** Filter for {@link InitBinder @InitBinder} methods. */
	private static final ReflectionUtils.MethodFilter BINDER_METHODS = method ->
			AnnotationUtils.findAnnotation(method, InitBinder.class) != null;

	/** Filter for {@link ModelAttribute @ModelAttribute} methods. */
	private static final ReflectionUtils.MethodFilter ATTRIBUTE_METHODS = method ->
			(AnnotationUtils.findAnnotation(method, RequestMapping.class) == null) &&
					(AnnotationUtils.findAnnotation(method, ModelAttribute.class) != null);

}
