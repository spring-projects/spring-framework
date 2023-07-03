/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import reactor.core.publisher.Mono;

import org.springframework.core.CoroutinesUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.method.MethodValidator;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;

/**
 * Extension of {@link HandlerMethod} that invokes the underlying method with
 * argument values resolved from the current HTTP request through a list of
 * {@link HandlerMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Mono<Object[]> EMPTY_ARGS = Mono.just(new Object[0]);

	private static final Class<?>[] EMPTY_GROUPS = new Class<?>[0];

	private static final Object NO_ARG_VALUE = new Object();


	private final HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

	@Nullable
	private MethodValidator methodValidator;


	/**
	 * Create an instance from a {@code HandlerMethod}.
	 */
	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	/**
	 * Create an instance from a bean instance and a method.
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}


	/**
	 * Configure the argument resolvers to use for resolving method
	 * argument values against a {@code ServerWebExchange}.
	 */
	public void setArgumentResolvers(List<? extends HandlerMethodArgumentResolver> resolvers) {
		this.resolvers.addResolvers(resolvers);
	}

	/**
	 * Return the configured argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getResolvers() {
		return this.resolvers.getResolvers();
	}

	/**
	 * Set the ParameterNameDiscoverer for resolving parameter names when needed
	 * (e.g. default request attribute name).
	 * <p>Default is a {@link DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer nameDiscoverer) {
		this.parameterNameDiscoverer = nameDiscoverer;
	}

	/**
	 * Return the configured parameter name discoverer.
	 */
	public ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * Configure a reactive adapter registry. This is needed for cases where the response is
	 * fully handled within the controller in combination with an async void return value.
	 * <p>By default this is a {@link ReactiveAdapterRegistry} with default settings.
	 */
	public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}

	/**
	 * Set the {@link MethodValidator} to perform method validation with if the
	 * controller method {@link #shouldValidateArguments()} or
	 * {@link #shouldValidateReturnValue()}.
	 * @since 6.1
	 */
	public void setMethodValidator(@Nullable MethodValidator methodValidator) {
		this.methodValidator = methodValidator;
	}


	/**
	 * Invoke the method for the given exchange.
	 * @param exchange the current exchange
	 * @param bindingContext the binding context to use
	 * @param providedArgs optional list of argument values to match by type
	 * @return a Mono with a {@link HandlerResult}
	 */
	@SuppressWarnings({"KotlinInternalInJava", "unchecked"})
	public Mono<HandlerResult> invoke(
			ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {

		return getMethodArgumentValues(exchange, bindingContext, providedArgs).flatMap(args -> {
			Class<?>[] groups = getValidationGroups();
			if (shouldValidateArguments() && this.methodValidator != null) {
				this.methodValidator.applyArgumentValidation(
						getBean(), getBridgedMethod(), getMethodParameters(), args, groups);
			}
			Object value;
			Method method = getBridgedMethod();
			boolean isSuspendingFunction = KotlinDetector.isSuspendingFunction(method);
			try {
				if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(method.getDeclaringClass())) {
					if (isSuspendingFunction) {
						value = CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
					}
					else {
						value = KotlinDelegate.invokeFunction(method, getBean(), args);
					}
				}
				else {
					value = method.invoke(getBean(), args);
				}
			}
			catch (IllegalArgumentException ex) {
				assertTargetBean(getBridgedMethod(), getBean(), args);
				String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
				return Mono.error(new IllegalStateException(formatInvokeError(text, args), ex));
			}
			catch (InvocationTargetException ex) {
				return Mono.error(ex.getTargetException());
			}
			catch (Throwable ex) {
				// Unlikely to ever get here, but it must be handled...
				return Mono.error(new IllegalStateException(formatInvokeError("Invocation failure", args), ex));
			}

			HttpStatusCode status = getResponseStatus();
			if (status != null) {
				exchange.getResponse().setStatusCode(status);
			}

			MethodParameter returnType = getReturnType();
			if (isResponseHandled(args, exchange)) {
				Class<?> parameterType = returnType.getParameterType();
				ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(parameterType);
				boolean asyncVoid = isAsyncVoidReturnType(returnType, adapter);
				if (value == null || asyncVoid) {
					return (asyncVoid ? Mono.from(adapter.toPublisher(value)) : Mono.empty());
				}
				if (isSuspendingFunction && parameterType == void.class) {
					return (Mono<HandlerResult>) value;
				}
			}

			HandlerResult result = new HandlerResult(this, value, returnType, bindingContext);
			return Mono.just(result);
		});
	}

	private Mono<Object[]> getMethodArgumentValues(
			ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {

		MethodParameter[] parameters = getMethodParameters();
		if (ObjectUtils.isEmpty(parameters)) {
			return EMPTY_ARGS;
		}

		List<Mono<Object>> argMonos = new ArrayList<>(parameters.length);
		for (MethodParameter parameter : parameters) {
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			Object providedArg = findProvidedArgument(parameter, providedArgs);
			if (providedArg != null) {
				argMonos.add(Mono.just(providedArg));
				continue;
			}
			if (!this.resolvers.supportsParameter(parameter)) {
				return Mono.error(new IllegalStateException(
						formatArgumentError(parameter, "No suitable resolver")));
			}
			try {
				argMonos.add(this.resolvers.resolveArgument(parameter, bindingContext, exchange)
						.defaultIfEmpty(NO_ARG_VALUE)
						.doOnError(ex -> logArgumentErrorIfNecessary(exchange, parameter, ex)));
			}
			catch (Exception ex) {
				logArgumentErrorIfNecessary(exchange, parameter, ex);
				argMonos.add(Mono.error(ex));
			}
		}
		return Mono.zip(argMonos, values ->
				Stream.of(values).map(value -> value != NO_ARG_VALUE ? value : null).toArray());
	}

	private void logArgumentErrorIfNecessary(ServerWebExchange exchange, MethodParameter parameter, Throwable ex) {
		// Leave stack trace for later, if error is not handled...
		String exMsg = ex.getMessage();
		if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + formatArgumentError(parameter, exMsg));
			}
		}
	}

	private Class<?>[] getValidationGroups() {
		return ((shouldValidateArguments() || shouldValidateReturnValue()) && this.methodValidator != null ?
				this.methodValidator.determineValidationGroups(getBean(), getBridgedMethod()) : EMPTY_GROUPS);
	}

	private static boolean isAsyncVoidReturnType(MethodParameter returnType, @Nullable ReactiveAdapter adapter) {
		if (adapter != null && adapter.supportsEmpty()) {
			if (adapter.isNoValue()) {
				return true;
			}
			Type parameterType = returnType.getGenericParameterType();
			if (parameterType instanceof ParameterizedType type) {
				if (type.getActualTypeArguments().length == 1) {
					return Void.class.equals(type.getActualTypeArguments()[0]);
				}
			}
		}
		return false;
	}

	private boolean isResponseHandled(Object[] args, ServerWebExchange exchange) {
		if (getResponseStatus() != null || exchange.isNotModified()) {
			return true;
		}
		for (Object arg : args) {
			if (arg instanceof ServerHttpResponse || arg instanceof ServerWebExchange) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		@Nullable
		@SuppressWarnings("deprecation")
		public static Object invokeFunction(Method method, Object target, Object[] args) {
			KFunction<?> function = Objects.requireNonNull(ReflectJvmMapping.getKotlinFunction(method));
			if (method.isAccessible() && !KCallablesJvm.isAccessible(function)) {
				KCallablesJvm.setAccessible(function, true);
			}
			Map<KParameter, Object> argMap = CollectionUtils.newHashMap(args.length + 1);
			int index = 0;
			for (KParameter parameter : function.getParameters()) {
				switch (parameter.getKind()) {
					case INSTANCE -> argMap.put(parameter, target);
					case VALUE -> {
						if (!parameter.isOptional() || args[index] != null) {
							argMap.put(parameter, args[index]);
						}
						index++;
					}
				}
			}
			return function.callBy(argMap);
		}
	}

}
