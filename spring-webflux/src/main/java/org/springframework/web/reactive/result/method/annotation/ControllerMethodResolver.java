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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.Assert;
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
 * resolving, initializing, and caching annotated methods declared in
 * {@code @Controller} and {@code @ControllerAdvice} components:
 * <ul>
 * <li>{@code @InitBinder}
 * <li>{@code @ModelAttribute}
 * <li>{@code @RequestMapping}
 * <li>{@code @ExceptionHandler}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ControllerMethodResolver {

	private static Log logger = LogFactory.getLog(ControllerMethodResolver.class);


	private final List<SyncHandlerMethodArgumentResolver> initBinderResolvers;

	private final List<HandlerMethodArgumentResolver> modelAttributeResolvers;

	private final List<HandlerMethodArgumentResolver> requestMappingResolvers;

	private final List<HandlerMethodArgumentResolver> exceptionHandlerResolvers;


	private final Map<Class<?>, Set<Method>> initBinderMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, Set<Method>> modelAttributeMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache = new ConcurrentHashMap<>(64);


	private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);


	ControllerMethodResolver(ArgumentResolverConfigurer argumentResolverConfigurer,
			List<HttpMessageReader<?>> messageReaders, ReactiveAdapterRegistry reactiveRegistry,
			ConfigurableApplicationContext applicationContext) {

		Assert.notNull(argumentResolverConfigurer, "ArgumentResolverConfigurer is required");
		Assert.notNull(reactiveRegistry, "ReactiveAdapterRegistry is required");
		Assert.notNull(applicationContext, "ConfigurableApplicationContext is required");

		ArgumentResolverRegistrar registrar;

		registrar= ArgumentResolverRegistrar.configurer(argumentResolverConfigurer).basic();
		addResolversTo(registrar, reactiveRegistry, applicationContext);
		this.initBinderResolvers = registrar.getSyncResolvers();

		registrar = ArgumentResolverRegistrar.configurer(argumentResolverConfigurer).modelAttributeSupport();
		addResolversTo(registrar, reactiveRegistry, applicationContext);
		this.modelAttributeResolvers = registrar.getResolvers();

		registrar = ArgumentResolverRegistrar.configurer(argumentResolverConfigurer).fullSupport(messageReaders);
		addResolversTo(registrar, reactiveRegistry, applicationContext);
		this.requestMappingResolvers = registrar.getResolvers();

		registrar = ArgumentResolverRegistrar.configurer(argumentResolverConfigurer).basic();
		addResolversTo(registrar, reactiveRegistry, applicationContext);
		this.exceptionHandlerResolvers = registrar.getResolvers();

		initControllerAdviceCaches(applicationContext);
	}

	private void addResolversTo(ArgumentResolverRegistrar registrar,
			ReactiveAdapterRegistry reactiveRegistry, ConfigurableApplicationContext context) {

		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

		// Annotation-based...
		registrar.add(new RequestParamMethodArgumentResolver(beanFactory, reactiveRegistry, false));
		registrar.add(new RequestParamMapMethodArgumentResolver(reactiveRegistry));
		registrar.add(new PathVariableMethodArgumentResolver(beanFactory, reactiveRegistry));
		registrar.add(new PathVariableMapMethodArgumentResolver(reactiveRegistry));
		registrar.addIfRequestBody(readers -> new RequestBodyArgumentResolver(readers, reactiveRegistry));
		registrar.addIfModelAttribute(() -> new ModelAttributeMethodArgumentResolver(reactiveRegistry, false));
		registrar.add(new RequestHeaderMethodArgumentResolver(beanFactory, reactiveRegistry));
		registrar.add(new RequestHeaderMapMethodArgumentResolver(reactiveRegistry));
		registrar.add(new CookieValueMethodArgumentResolver(beanFactory, reactiveRegistry));
		registrar.add(new ExpressionValueMethodArgumentResolver(beanFactory, reactiveRegistry));
		registrar.add(new SessionAttributeMethodArgumentResolver(beanFactory, reactiveRegistry));
		registrar.add(new RequestAttributeMethodArgumentResolver(beanFactory, reactiveRegistry));

		// Type-based...
		registrar.addIfRequestBody(readers -> new HttpEntityArgumentResolver(readers, reactiveRegistry));
		registrar.add(new ModelArgumentResolver(reactiveRegistry));
		registrar.addIfModelAttribute(() -> new ErrorsMethodArgumentResolver(reactiveRegistry));
		registrar.add(new ServerWebExchangeArgumentResolver(reactiveRegistry));
		registrar.add(new PrincipalArgumentResolver(reactiveRegistry));
		registrar.add(new WebSessionArgumentResolver(reactiveRegistry));

		// Custom...
		registrar.addCustomResolvers();

		// Catch-all...
		registrar.add(new RequestParamMethodArgumentResolver(beanFactory, reactiveRegistry, true));
		registrar.addIfModelAttribute(() -> new ModelAttributeMethodArgumentResolver(reactiveRegistry, true));
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
				this.modelAttributeAdviceCache.put(bean, attrMethods);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @ModelAttribute methods in " + bean);
				}
			}
			Set<Method> binderMethods = selectMethods(beanType, BINDER_METHODS);
			if (!binderMethods.isEmpty()) {
				this.initBinderAdviceCache.put(bean, binderMethods);
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
	 * Return an {@link InvocableHandlerMethod} for the given
	 * {@code @RequestMapping} method initialized with argument resolvers.
	 */
	public InvocableHandlerMethod getRequestMappingMethod(HandlerMethod handlerMethod) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		invocable.setArgumentResolvers(this.requestMappingResolvers);
		return invocable;
	}

	/**
	 * Find {@code @InitBinder} methods in {@code @ControllerAdvice} components
	 * or in the controller of the given {@code @RequestMapping} method.
	 */
	public List<SyncInvocableHandlerMethod> getInitBinderMethods(HandlerMethod handlerMethod) {

		List<SyncInvocableHandlerMethod> result = new ArrayList<>();
		Class<?> handlerType = handlerMethod.getBeanType();

		// Global methods first
		this.initBinderAdviceCache.entrySet().forEach(entry -> {
			if (entry.getKey().isApplicableToBeanType(handlerType)) {
				Object bean = entry.getKey().resolveBean();
				entry.getValue().forEach(method -> result.add(getInitBinderMethod(bean, method)));
			}
		});

		this.initBinderMethodCache
				.computeIfAbsent(handlerType, aClass -> selectMethods(handlerType, BINDER_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(getInitBinderMethod(bean, method));
				});

		return result;
	}

	private SyncInvocableHandlerMethod getInitBinderMethod(Object bean, Method method) {
		SyncInvocableHandlerMethod invocable = new SyncInvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.initBinderResolvers);
		return invocable;
	}

	/**
	 * Find {@code @ModelAttribute} methods in {@code @ControllerAdvice}
	 * components or in the controller of the given {@code @RequestMapping} method.
	 */
	public List<InvocableHandlerMethod> getModelAttributeMethods(HandlerMethod handlerMethod) {

		List<InvocableHandlerMethod> result = new ArrayList<>();
		Class<?> handlerType = handlerMethod.getBeanType();

		// Global methods first
		this.modelAttributeAdviceCache.entrySet().forEach(entry -> {
			if (entry.getKey().isApplicableToBeanType(handlerType)) {
				Object bean = entry.getKey().resolveBean();
				entry.getValue().forEach(method -> result.add(createAttributeMethod(bean, method)));
			}
		});

		this.modelAttributeMethodCache
				.computeIfAbsent(handlerType, aClass -> selectMethods(handlerType, ATTRIBUTE_METHODS))
				.forEach(method -> {
					Object bean = handlerMethod.getBean();
					result.add(createAttributeMethod(bean, method));
				});

		return result;
	}

	private InvocableHandlerMethod createAttributeMethod(Object bean, Method method) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.modelAttributeResolvers);
		return invocable;
	}

	/**
	 * Find an {@code @ExceptionHandler} method in {@code @ControllerAdvice}
	 * components or in the controller of the given {@code @RequestMapping} method.
	 */
	public Optional<InvocableHandlerMethod> getExceptionHandlerMethod(Throwable ex,
			HandlerMethod handlerMethod) {

		Class<?> handlerType = handlerMethod.getBeanType();

		// Controller-local first...
		Object targetBean = handlerMethod.getBean();
		Method targetMethod = this.exceptionHandlerCache
				.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new)
				.resolveMethodByThrowable(ex);

		if (targetMethod == null) {
			// Global exception handlers...
			for (ControllerAdviceBean advice : this.exceptionHandlerAdviceCache.keySet()) {
				if (advice.isApplicableToBeanType(handlerType)) {
					targetBean = advice.resolveBean();
					targetMethod = this.exceptionHandlerAdviceCache.get(advice).resolveMethodByThrowable(ex);
					if (targetMethod != null) {
						break;
					}
				}
			}
		}

		if (targetMethod == null) {
			return Optional.empty();
		}

		InvocableHandlerMethod invocable = new InvocableHandlerMethod(targetBean, targetMethod);
		invocable.setArgumentResolvers(this.exceptionHandlerResolvers);
		return Optional.of(invocable);
	}


	/** Filter for {@link InitBinder @InitBinder} methods. */
	private static final ReflectionUtils.MethodFilter BINDER_METHODS = method ->
			AnnotationUtils.findAnnotation(method, InitBinder.class) != null;

	/** Filter for {@link ModelAttribute @ModelAttribute} methods. */
	private static final ReflectionUtils.MethodFilter ATTRIBUTE_METHODS = method ->
			(AnnotationUtils.findAnnotation(method, RequestMapping.class) == null) &&
					(AnnotationUtils.findAnnotation(method, ModelAttribute.class) != null);


	private static class ArgumentResolverRegistrar {

		private final List<HandlerMethodArgumentResolver> customResolvers;

		private final List<HttpMessageReader<?>> messageReaders;

		private final boolean modelAttributeSupported;

		private final List<HandlerMethodArgumentResolver> result = new ArrayList<>();


		private ArgumentResolverRegistrar(ArgumentResolverConfigurer configurer,
				List<HttpMessageReader<?>> messageReaders, boolean modelAttribute) {

			this.customResolvers = configurer.getCustomResolvers();
			this.messageReaders = messageReaders != null ? new ArrayList<>(messageReaders) : null;
			this.modelAttributeSupported = modelAttribute;
		}


		public void add(HandlerMethodArgumentResolver resolver) {
			this.result.add(resolver);
		}

		public void addIfRequestBody(Function<List<HttpMessageReader<?>>, HandlerMethodArgumentResolver> function) {
			if (this.messageReaders != null) {
				add(function.apply(this.messageReaders));
			}
		}

		public void addIfModelAttribute(Supplier<HandlerMethodArgumentResolver> supplier) {
			if (this.modelAttributeSupported) {
				add(supplier.get());
			}
		}

		public void addCustomResolvers() {
			this.customResolvers.forEach(this::add);
		}


		public List<HandlerMethodArgumentResolver> getResolvers() {
			return this.result;
		}

		public List<SyncHandlerMethodArgumentResolver> getSyncResolvers() {
			return this.result.stream()
					.filter(resolver -> resolver instanceof SyncHandlerMethodArgumentResolver)
					.map(resolver -> (SyncHandlerMethodArgumentResolver) resolver)
					.collect(Collectors.toList());
		}


		public static Builder configurer(ArgumentResolverConfigurer configurer) {
			return new Builder(configurer);
		}


		public static class Builder {

			private final ArgumentResolverConfigurer configurer;


			public Builder(ArgumentResolverConfigurer configurer) {
				this.configurer = configurer;
			}


			public ArgumentResolverRegistrar fullSupport(List<HttpMessageReader<?>> readers) {
				Assert.notEmpty(readers, "No message readers");
				return new ArgumentResolverRegistrar(this.configurer, readers, true);
			}

			public ArgumentResolverRegistrar modelAttributeSupport() {
				return new ArgumentResolverRegistrar(this.configurer, null, true);
			}

			public ArgumentResolverRegistrar basic() {
				return new ArgumentResolverRegistrar(this.configurer, null, false);
			}
		}

	}

}
