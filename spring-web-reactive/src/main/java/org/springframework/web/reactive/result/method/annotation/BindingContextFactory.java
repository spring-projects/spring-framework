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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * A helper class for {@link RequestMappingHandlerAdapter} that assists with
 * creating a {@code BindingContext} and initialize it, and its model, through
 * {@code @InitBinder} and {@code @ModelAttribute} methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class BindingContextFactory {

	private final RequestMappingHandlerAdapter adapter;


	public BindingContextFactory(RequestMappingHandlerAdapter adapter) {
		this.adapter = adapter;
	}


	public RequestMappingHandlerAdapter getAdapter() {
		return this.adapter;
	}

	private WebBindingInitializer getBindingInitializer() {
		return getAdapter().getWebBindingInitializer();
	}

	private List<SyncHandlerMethodArgumentResolver> getInitBinderArgumentResolvers() {
		return getAdapter().getInitBinderArgumentResolvers();
	}

	private List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return getAdapter().getArgumentResolvers();
	}

	private ReactiveAdapterRegistry getAdapterRegistry() {
		return getAdapter().getReactiveAdapterRegistry();
	}

	private Stream<Method> getInitBinderMethods(HandlerMethod handlerMethod) {
		return getAdapter().getInitBinderMethods(handlerMethod.getBeanType()).stream();
	}

	private Stream<Method> getModelAttributeMethods(HandlerMethod handlerMethod) {
		return getAdapter().getModelAttributeMethods(handlerMethod.getBeanType()).stream();
	}


	/**
	 * Create and initialize a BindingContext for the current request.
	 * @param handlerMethod the request handling method
	 * @param exchange the current exchange
	 * @return Mono with the BindingContext instance
	 */
	public Mono<BindingContext> createBindingContext(HandlerMethod handlerMethod,
			ServerWebExchange exchange) {

		List<SyncInvocableHandlerMethod> invocableMethods = getInitBinderMethods(handlerMethod)
				.map(method -> {
					Object bean = handlerMethod.getBean();
					SyncInvocableHandlerMethod invocable = new SyncInvocableHandlerMethod(bean, method);
					invocable.setSyncArgumentResolvers(getInitBinderArgumentResolvers());
					return invocable;
				})
				.collect(Collectors.toList());

		BindingContext bindingContext =
				new InitBinderBindingContext(getBindingInitializer(), invocableMethods);

		return initModel(handlerMethod, bindingContext, exchange).then(Mono.just(bindingContext));
	}

	@SuppressWarnings("Convert2MethodRef")
	private Mono<Void> initModel(HandlerMethod handlerMethod, BindingContext context,
			ServerWebExchange exchange) {

		List<Mono<HandlerResult>> resultMonos = getModelAttributeMethods(handlerMethod)
				.map(method -> {
					Object bean = handlerMethod.getBean();
					InvocableHandlerMethod invocable = new InvocableHandlerMethod(bean, method);
					invocable.setArgumentResolvers(getArgumentResolvers());
					return invocable;
				})
				.map(invocable -> invocable.invoke(exchange, context))
				.collect(Collectors.toList());

		return Mono
				.when(resultMonos, resultArr -> processModelMethodMonos(resultArr, context))
				.then(voidMonos -> Mono.when(voidMonos));
	}

	private List<Mono<Void>> processModelMethodMonos(Object[] resultArr, BindingContext context) {
		return Arrays.stream(resultArr)
				.map(result -> processModelMethodResult((HandlerResult) result, context))
				.collect(Collectors.toList());
	}

	private Mono<Void> processModelMethodResult(HandlerResult result, BindingContext context) {
		Object value = result.getReturnValue().orElse(null);
		if (value == null) {
			return Mono.empty();
		}

		ResolvableType type = result.getReturnType();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapterFrom(type.getRawClass(), value);
		Class<?> valueType = (adapter != null ? type.resolveGeneric(0) : type.resolve());

		if (Void.class.equals(valueType) || void.class.equals(valueType)) {
			return (adapter != null ? adapter.toMono(value) : Mono.empty());
		}

		String name = getAttributeName(valueType, result.getReturnTypeSource());
		context.getModel().asMap().putIfAbsent(name, value);
		return Mono.empty();
	}

	private String getAttributeName(Class<?> valueType, MethodParameter parameter) {
		Method method = parameter.getMethod();
		ModelAttribute annot = AnnotatedElementUtils.findMergedAnnotation(method, ModelAttribute.class);
		if (annot != null && StringUtils.hasText(annot.value())) {
			return annot.value();
		}
		// TODO: Conventions does not deal with async wrappers
		return ClassUtils.getShortNameAsProperty(valueType);
	}

}
