/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Package-private class to assist {@link RequestMappingHandlerAdapter} with
 * default model initialization through {@code @ModelAttribute} methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ModelInitializer {

	private static final Log logger = LogFactory.getLog(ModelInitializer.class);

	private final ControllerMethodResolver methodResolver;

	private final ReactiveAdapterRegistry adapterRegistry;


	public ModelInitializer(ControllerMethodResolver methodResolver, ReactiveAdapterRegistry adapterRegistry) {
		Assert.notNull(methodResolver, "ControllerMethodResolver is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.methodResolver = methodResolver;
		this.adapterRegistry = adapterRegistry;
	}


	/**
	 * Initialize the {@link org.springframework.ui.Model Model} based on a
	 * (type-level) {@code @SessionAttributes} annotation and
	 * {@code @ModelAttribute} methods.
	 * @param handlerMethod the target controller method
	 * @param bindingContext the context containing the model
	 * @param exchange the current exchange
	 * @return a {@code Mono} for when the model is populated.
	 */
	@SuppressWarnings("Convert2MethodRef")
	public Mono<Void> initModel(HandlerMethod handlerMethod, InitBinderBindingContext bindingContext,
			ServerWebExchange exchange) {

		List<InvocableHandlerMethod> modelMethods =
				this.methodResolver.getModelAttributeMethods(handlerMethod);

		SessionAttributesHandler sessionAttributesHandler =
				this.methodResolver.getSessionAttributesHandler(handlerMethod);

		if (!sessionAttributesHandler.hasSessionAttributes()) {
			return invokeModelAttributeMethods(bindingContext, modelMethods, exchange);
		}

		return exchange.getSession()
				.flatMap(session -> {
					Map<String, Object> attributes = sessionAttributesHandler.retrieveAttributes(session);
					bindingContext.getModel().mergeAttributes(attributes);
					bindingContext.setSessionContext(sessionAttributesHandler, session);
					return invokeModelAttributeMethods(bindingContext, modelMethods, exchange)
							.doOnSuccess(aVoid ->
								findModelAttributes(handlerMethod, sessionAttributesHandler).forEach(name -> {
									if (!bindingContext.getModel().containsAttribute(name)) {
										Object value = session.getRequiredAttribute(name);
										bindingContext.getModel().addAttribute(name, value);
									}
								}));
				});
	}

	private Mono<Void> invokeModelAttributeMethods(BindingContext bindingContext,
			List<InvocableHandlerMethod> modelMethods, ServerWebExchange exchange) {

		List<Mono<HandlerResult>> resultList = buildModelAttributeMethodGraph(bindingContext, modelMethods, exchange);

		return Mono
				.zip(resultList, objectArray ->
						Arrays.stream(objectArray)
								.map(object -> handleResult(((HandlerResult) object), bindingContext))
								.collect(Collectors.toList()))
				.flatMap(Mono::when);
	}

	private List<Mono<HandlerResult>> buildModelAttributeMethodGraph(BindingContext bindingContext,
			List<InvocableHandlerMethod> modelMethods, ServerWebExchange exchange) {

		Map<String, InvocableHandlerMethod> produces = new HashMap<>(modelMethods.size());

		modelMethods.forEach(invocable -> {
			produces.put(getNameForReturnValue(null, invocable.getReturnType()), invocable);
		});

		Map<String, Mono<HandlerResult>> monoMap = new HashMap<>(produces.size());

		List<Mono<HandlerResult>> monos = new ArrayList<>(modelMethods.size());
		modelMethods.forEach(invocable -> {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("%s requires %s",
						invocable.getMethod().getName(),
						Arrays.stream(invocable.getMethodParameters())
								.filter(parameter -> parameter.hasParameterAnnotation(ModelAttribute.class))
								.map(ModelInitializer::getNameForParameter)
								.collect(Collectors.joining(","))
				));
			}
			monos.add(makeMono(bindingContext, exchange, produces, monoMap, invocable));
		});

		return monos;
	}

	private Mono<HandlerResult> makeMono(BindingContext bindingContext, ServerWebExchange exchange,
			Map<String, InvocableHandlerMethod> produces, Map<String, Mono<HandlerResult>> required,
			InvocableHandlerMethod invocable) {

		List<String> parameters = Arrays.stream(invocable.getMethodParameters())
				.filter(parameter -> parameter.hasParameterAnnotation(ModelAttribute.class))
				.map(ModelInitializer::getNameForParameter)
				.collect(Collectors.toList());

		List<Mono<HandlerResult>> requirements = new ArrayList<>();
		for (String parameter : parameters) {
			if(!required.containsKey(parameter)) {
				InvocableHandlerMethod child = produces.get(parameter);
				if(child == null) {
					required.put(parameter, Mono.empty());
				} else {
					required.put(parameter, makeMono(bindingContext, exchange, produces, required, child));
				}
			}
			requirements.add(required.get(parameter));
		}

		Mono<HandlerResult> mono =
		//		(requirements.isEmpty()
//						? Mono.empty()
		//				:
		Mono.zip(requirements, Function.identity())
		//)
					.then(invocable.invoke(exchange, bindingContext));
		required.put(
				ModelInitializer.getNameForReturnValue(null, invocable.getReturnType()),
				mono);

		return mono;
	}

	private Mono<Void> handleResult(HandlerResult handlerResult, BindingContext bindingContext) {
		Object value = handlerResult.getReturnValue();
		if (value != null) {
			ResolvableType type = handlerResult.getReturnType();
			ReactiveAdapter adapter = this.adapterRegistry.getAdapter(type.getRawClass(), value);
			if (isAsyncVoidType(type, adapter)) {
				return Mono.from(adapter.toPublisher(value));
			}
			String name = getAttributeName(handlerResult.getReturnTypeSource());
			bindingContext.getModel().asMap().putIfAbsent(name, value);
		}
		return Mono.empty();
	}

	private boolean isAsyncVoidType(ResolvableType type, @Nullable  ReactiveAdapter adapter) {
		return adapter != null && (adapter.isNoValue() || type.resolveGeneric() == Void.class);
	}

	private String getAttributeName(MethodParameter param) {
		return Optional
				.ofNullable(AnnotatedElementUtils.findMergedAnnotation(param.getAnnotatedElement(), ModelAttribute.class))
				.filter(ann -> StringUtils.hasText(ann.value()))
				.map(ModelAttribute::value)
				.orElseGet(() -> Conventions.getVariableNameForParameter(param));
	}

	/** Find {@code @ModelAttribute} arguments also listed as {@code @SessionAttributes}. */
	private List<String> findModelAttributes(HandlerMethod handlerMethod,
			SessionAttributesHandler sessionAttributesHandler) {

		List<String> result = new ArrayList<>();
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				String name = getNameForParameter(parameter);
				Class<?> paramType = parameter.getParameterType();
				if (sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
					result.add(name);
				}
			}
		}
		return result;
	}

	/**
	 * Derive the model attribute name for the given method parameter based on
	 * a {@code @ModelAttribute} parameter annotation (if present) or falling
	 * back on parameter type based conventions.
	 * @param parameter a descriptor for the method parameter
	 * @return the derived name
	 * @see Conventions#getVariableNameForParameter(MethodParameter)
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		String name = (ann != null ? ann.value() : null);
		return (StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter));
	}

	/**
	 * Derive the model attribute name for the given return value. Results will be
	 * based on:
	 * <ol>
	 * <li>the method {@code ModelAttribute} annotation value
	 * <li>the declared return type if it is more specific than {@code Object}
	 * <li>the actual return value type
	 * </ol>
	 * @param returnValue the value returned from a method invocation
	 * @param returnType a descriptor for the return type of the method
	 * @return the derived name (never {@code null} or empty String)
	 */
	public static String getNameForReturnValue(@Nullable Object returnValue, MethodParameter returnType) {
		ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
		if (ann != null && StringUtils.hasText(ann.value())) {
			return ann.value();
		}
		else {
			Method method = returnType.getMethod();
			Assert.state(method != null, "No handler method");
			Class<?> containingClass = returnType.getContainingClass();
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}
}
