/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.scheduler.Scheduler;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.validation.method.MethodValidator;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMappingInfo;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.method.annotation.HandlerMethodValidator;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Package-private class to assist {@link RequestMappingHandlerAdapter} with
 * resolving, initializing, and caching annotated methods declared in
 * {@code @Controller} and {@code @ControllerAdvice} components.
 *
 * <p>Assists with the following annotations:
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

	/**
	 * MethodFilter that matches {@link InitBinder @InitBinder} methods.
	 */
	private static final MethodFilter INIT_BINDER_METHODS = method ->
			AnnotatedElementUtils.hasAnnotation(method, InitBinder.class);

	/**
	 * MethodFilter that matches {@link ModelAttribute @ModelAttribute} methods.
	 */
	private static final MethodFilter MODEL_ATTRIBUTE_METHODS = method ->
			(!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) &&
					AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class));

	private static final boolean BEAN_VALIDATION_PRESENT =
			ClassUtils.isPresent("jakarta.validation.Validator", HandlerMethod.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(ControllerMethodResolver.class);


	private final List<SyncHandlerMethodArgumentResolver> initBinderResolvers;

	private final List<HandlerMethodArgumentResolver> modelAttributeResolvers;

	private final List<HandlerMethodArgumentResolver> requestMappingResolvers;

	private final List<HandlerMethodArgumentResolver> exceptionHandlerResolvers;

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;

	private final RequestedContentTypeResolver contentTypeResolver;

	@Nullable
	private final Scheduler invocationScheduler;

	@Nullable
	private final Predicate<? super HandlerMethod> blockingMethodPredicate;

	@Nullable
	private final MethodValidator methodValidator;

	private final Map<Class<?>, Set<Method>> initBinderMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, Set<Method>> modelAttributeMethodCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache = new ConcurrentHashMap<>(64);

	private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache = new LinkedHashMap<>(64);

	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);

	private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache = new ConcurrentHashMap<>(64);



	ControllerMethodResolver(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context, RequestedContentTypeResolver contentTypeResolver,
			List<HttpMessageReader<?>> readers, @Nullable WebBindingInitializer webBindingInitializer,
			@Nullable Scheduler invocationScheduler, @Nullable Predicate<? super HandlerMethod> blockingMethodPredicate) {

		Assert.notNull(customResolvers, "ArgumentResolverConfigurer is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		Assert.notNull(context, "ApplicationContext is required");
		Assert.notNull(contentTypeResolver, "RequestedContentTypeResolver is required");
		Assert.notNull(readers, "HttpMessageReader List is required");

		this.initBinderResolvers = initBinderResolvers(customResolvers, adapterRegistry, context);
		this.modelAttributeResolvers = modelMethodResolvers(customResolvers, adapterRegistry, context);
		this.requestMappingResolvers = requestMappingResolvers(customResolvers, adapterRegistry, context, readers);
		this.exceptionHandlerResolvers = exceptionHandlerResolvers(customResolvers, adapterRegistry, context);
		this.reactiveAdapterRegistry = adapterRegistry;
		this.contentTypeResolver = contentTypeResolver;
		this.invocationScheduler = invocationScheduler;
		this.blockingMethodPredicate = blockingMethodPredicate;

		if (BEAN_VALIDATION_PRESENT) {
			this.methodValidator = HandlerMethodValidator.from(webBindingInitializer, null,
					methodParamPredicate(this.requestMappingResolvers, ModelAttributeMethodArgumentResolver.class),
					methodParamPredicate(this.requestMappingResolvers, RequestParamMethodArgumentResolver.class));
		}
		else {
			this.methodValidator = null;
		}

		initControllerAdviceCaches(context);
	}

	private List<SyncHandlerMethodArgumentResolver> initBinderResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, adapterRegistry, context, false, Collections.emptyList()).stream()
				.filter(SyncHandlerMethodArgumentResolver.class::isInstance)
				.map(SyncHandlerMethodArgumentResolver.class::cast)
				.toList();
	}

	private static List<HandlerMethodArgumentResolver> modelMethodResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, adapterRegistry, context, true, Collections.emptyList());
	}

	private static List<HandlerMethodArgumentResolver> requestMappingResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context, List<HttpMessageReader<?>> readers) {

		return initResolvers(customResolvers, adapterRegistry, context, true, readers);
	}

	private static List<HandlerMethodArgumentResolver> exceptionHandlerResolvers(
			ArgumentResolverConfigurer customResolvers, ReactiveAdapterRegistry adapterRegistry,
			ConfigurableApplicationContext context) {

		return initResolvers(customResolvers, adapterRegistry, context, false, Collections.emptyList());
	}

	private static List<HandlerMethodArgumentResolver> initResolvers(ArgumentResolverConfigurer customResolvers,
			ReactiveAdapterRegistry adapterRegistry, ConfigurableApplicationContext context,
			boolean supportDataBinding, List<HttpMessageReader<?>> readers) {

		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		boolean requestMappingMethod = !readers.isEmpty() && supportDataBinding;

		// Annotation-based...
		List<HandlerMethodArgumentResolver> result = new ArrayList<>(30);
		result.add(new RequestParamMethodArgumentResolver(beanFactory, adapterRegistry, false));
		result.add(new RequestParamMapMethodArgumentResolver(adapterRegistry));
		result.add(new PathVariableMethodArgumentResolver(beanFactory, adapterRegistry));
		result.add(new PathVariableMapMethodArgumentResolver(adapterRegistry));
		result.add(new MatrixVariableMethodArgumentResolver(beanFactory, adapterRegistry));
		result.add(new MatrixVariableMapMethodArgumentResolver(adapterRegistry));
		if (!readers.isEmpty()) {
			result.add(new RequestBodyMethodArgumentResolver(readers, adapterRegistry));
			result.add(new RequestPartMethodArgumentResolver(readers, adapterRegistry));
		}
		if (supportDataBinding) {
			result.add(new ModelAttributeMethodArgumentResolver(adapterRegistry, false));
		}
		result.add(new RequestHeaderMethodArgumentResolver(beanFactory, adapterRegistry));
		result.add(new RequestHeaderMapMethodArgumentResolver(adapterRegistry));
		result.add(new CookieValueMethodArgumentResolver(beanFactory, adapterRegistry));
		result.add(new ExpressionValueMethodArgumentResolver(beanFactory, adapterRegistry));
		result.add(new SessionAttributeMethodArgumentResolver(beanFactory, adapterRegistry));
		result.add(new RequestAttributeMethodArgumentResolver(beanFactory, adapterRegistry));

		// Type-based...
		if (!readers.isEmpty()) {
			result.add(new HttpEntityMethodArgumentResolver(readers, adapterRegistry));
		}
		result.add(new ModelMethodArgumentResolver(adapterRegistry));
		if (supportDataBinding) {
			result.add(new ErrorsMethodArgumentResolver(adapterRegistry));
		}
		result.add(new ServerWebExchangeMethodArgumentResolver(adapterRegistry));
		result.add(new PrincipalMethodArgumentResolver(adapterRegistry));
		if (requestMappingMethod) {
			result.add(new SessionStatusMethodArgumentResolver());
		}
		result.add(new WebSessionMethodArgumentResolver(adapterRegistry));
		if (KotlinDetector.isKotlinPresent()) {
			result.add(new ContinuationHandlerMethodArgumentResolver());
		}

		// Custom...
		result.addAll(customResolvers.getCustomResolvers());

		// Catch-all...
		result.add(new RequestParamMethodArgumentResolver(beanFactory, adapterRegistry, true));
		if (supportDataBinding) {
			result.add(new ModelAttributeMethodArgumentResolver(adapterRegistry, true));
		}

		return result;
	}

	private void initControllerAdviceCaches(ApplicationContext applicationContext) {
		List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
		for (ControllerAdviceBean bean : beans) {
			Class<?> beanType = bean.getBeanType();
			if (beanType != null) {
				Set<Method> attrMethods = MethodIntrospector.selectMethods(beanType, MODEL_ATTRIBUTE_METHODS);
				if (!attrMethods.isEmpty()) {
					this.modelAttributeAdviceCache.put(bean, attrMethods);
				}
				Set<Method> binderMethods = MethodIntrospector.selectMethods(beanType, INIT_BINDER_METHODS);
				if (!binderMethods.isEmpty()) {
					this.initBinderAdviceCache.put(bean, binderMethods);
				}
				ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
				if (resolver.hasExceptionMappings()) {
					this.exceptionHandlerAdviceCache.put(bean, resolver);
				}
			}
		}

		if (logger.isDebugEnabled()) {
			int modelSize = this.modelAttributeAdviceCache.size();
			int binderSize = this.initBinderAdviceCache.size();
			int handlerSize = this.exceptionHandlerAdviceCache.size();
			if (modelSize == 0 && binderSize == 0 && handlerSize == 0) {
				logger.debug("ControllerAdvice beans: none");
			}
			else {
				logger.debug("ControllerAdvice beans: " + modelSize + " @ModelAttribute, " + binderSize +
						" @InitBinder, " + handlerSize + " @ExceptionHandler");
			}
		}
	}

	private static Predicate<MethodParameter> methodParamPredicate(
			List<HandlerMethodArgumentResolver> resolvers, Class<?> resolverType) {

		return parameter -> {
			for (HandlerMethodArgumentResolver resolver : resolvers) {
				if (resolver.supportsParameter(parameter)) {
					return resolverType.isInstance(resolver);
				}
			}
			return false;
		};
	}

	/**
	 * Return a {@link Scheduler} for the given method if it is considered
	 * blocking by the underlying blocking method predicate, or null if no
	 * particular scheduler should be used for this method invocation.
	 */
	@Nullable
	public Scheduler getSchedulerFor(HandlerMethod handlerMethod) {
		if (this.invocationScheduler != null) {
			Assert.state(this.blockingMethodPredicate != null, "Expected HandlerMethod Predicate");
			if (this.blockingMethodPredicate.test(handlerMethod)) {
				return this.invocationScheduler;
			}
		}
		return null;
	}

	/**
	 * Return an {@link InvocableHandlerMethod} for the given
	 * {@code @RequestMapping} method initialized with argument resolvers.
	 */
	public InvocableHandlerMethod getRequestMappingMethod(HandlerMethod handlerMethod) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		invocable.setArgumentResolvers(this.requestMappingResolvers);
		invocable.setReactiveAdapterRegistry(this.reactiveAdapterRegistry);
		invocable.setMethodValidator(this.methodValidator);
		invocable.setInvocationScheduler(getSchedulerFor(handlerMethod));
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
		this.initBinderAdviceCache.forEach((adviceBean, methods) -> {
			if (adviceBean.isApplicableToBeanType(handlerType)) {
				Object bean = adviceBean.resolveBean();
				methods.forEach(method -> result.add(getInitBinderMethod(bean, method)));
			}
		});

		this.initBinderMethodCache
				.computeIfAbsent(handlerType,
						clazz -> MethodIntrospector.selectMethods(handlerType, INIT_BINDER_METHODS))
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
		this.modelAttributeAdviceCache.forEach((adviceBean, methods) -> {
			if (adviceBean.isApplicableToBeanType(handlerType)) {
				Object bean = adviceBean.resolveBean();
				methods.forEach(method -> result.add(createAttributeMethod(bean, method)));
			}
		});

		this.modelAttributeMethodCache
				.computeIfAbsent(handlerType,
						clazz -> MethodIntrospector.selectMethods(handlerType, MODEL_ATTRIBUTE_METHODS))
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
	 * Look for an {@code @ExceptionHandler} method within the class of the given
	 * controller method, and also within {@code @ControllerAdvice} classes that
	 * are applicable to the class of the given controller method.
	 * @param ex the exception to find a handler for
	 * @param exchange the current HTTP exchange
	 * @param handlerMethod the controller method that raised the exception,
	 *        or if {@code null}, check only {@code @ControllerAdvice} classes.
	 */
	@Nullable
	@SuppressWarnings("NullAway")
	public InvocableHandlerMethod getExceptionHandlerMethod(Throwable ex, ServerWebExchange exchange, @Nullable HandlerMethod handlerMethod) {

		Class<?> handlerType = (handlerMethod != null ? handlerMethod.getBeanType() : null);
		List<MediaType> requestedMediaTypes = List.of(MediaType.ALL);
		try {
			requestedMediaTypes = this.contentTypeResolver.resolveMediaTypes(exchange);
		}
		catch (NotAcceptableStatusException exc) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not parse Accept header for requested media types", exc);
			}
		}

		// Controller-local first
		if (handlerType != null) {
			for (MediaType mediaType : requestedMediaTypes) {
				ExceptionHandlerMappingInfo mappingInfo = this.exceptionHandlerCache
						.computeIfAbsent(handlerType, ExceptionHandlerMethodResolver::new)
						.resolveExceptionMapping(ex, mediaType);
				if (mappingInfo != null) {
					if (!mappingInfo.getProducibleTypes().isEmpty()) {
						exchange.getAttributes().put(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mappingInfo.getProducibleTypes());
					}
					return createInvocableHandlerMethod(handlerMethod.getBean(), mappingInfo.getHandlerMethod());
				}
			}
		}

		// Global exception handlers
		for (MediaType mediaType : requestedMediaTypes) {
			for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
				ControllerAdviceBean advice = entry.getKey();
				if (advice.isApplicableToBeanType(handlerType)) {
					ExceptionHandlerMappingInfo mappingInfo = entry.getValue().resolveExceptionMapping(ex, mediaType);
					if (mappingInfo != null) {
						if (!mappingInfo.getProducibleTypes().isEmpty()) {
							exchange.getAttributes().put(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mappingInfo.getProducibleTypes());
						}
						return createInvocableHandlerMethod(advice.resolveBean(), mappingInfo.getHandlerMethod());
					}
				}
			}
		}

		return null;
	}

	private InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(bean, method);
		invocable.setArgumentResolvers(this.exceptionHandlerResolvers);
		return invocable;
	}

	public boolean hasMethodValidator() {
		return (this.methodValidator != null);
	}

	/**
	 * Return the handler for the type-level {@code @SessionAttributes} annotation
	 * based on the given controller method.
	 */
	public SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
		Class<?> handlerType = handlerMethod.getBeanType();
		return this.sessionAttributesHandlerCache.computeIfAbsent(handlerType, SessionAttributesHandler::new);
	}

}
