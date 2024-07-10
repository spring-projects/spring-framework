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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import kotlin.Unit;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KType;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.springframework.core.CoroutinesUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Contract;
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
 * <p>By default, the method invocation happens on the thread from which the
 * {@code Mono} was subscribed to, or in some cases the thread that emitted one
 * of the resolved arguments (e.g. when the request body needs to be decoded).
 * To ensure a predictable thread for the underlying method's invocation,
 * a {@link Scheduler} can optionally be provided via
 * {@link #setInvocationScheduler(Scheduler)}.
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

	private Class<?>[] validationGroups = EMPTY_GROUPS;

	@Nullable
	private Scheduler invocationScheduler;


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
		this.validationGroups = (methodValidator != null ?
				methodValidator.determineValidationGroups(getBean(), getBridgedMethod()) : EMPTY_GROUPS);
	}

	/**
	 * Set the {@link Scheduler} on which to perform the method invocation.
	 * @since 6.1.6
	 */
	public void setInvocationScheduler(@Nullable Scheduler invocationScheduler) {
		this.invocationScheduler = invocationScheduler;
	}

	/**
	 * Invoke the method for the given exchange.
	 * @param exchange the current exchange
	 * @param bindingContext the binding context to use
	 * @param providedArgs optional list of argument values to match by type
	 * @return a Mono with a {@link HandlerResult}
	 */
	@SuppressWarnings({"unchecked", "NullAway"})
	public Mono<HandlerResult> invoke(
			ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {

		return getMethodArgumentValuesOnScheduler(exchange, bindingContext, providedArgs).flatMap(args -> {
			if (shouldValidateArguments() && this.methodValidator != null) {
				this.methodValidator.applyArgumentValidation(
						getBean(), getBridgedMethod(), getMethodParameters(), args, this.validationGroups);
			}
			Object value;
			Method method = getBridgedMethod();
			boolean isSuspendingFunction = KotlinDetector.isSuspendingFunction(method);
			try {
				if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(method.getDeclaringClass())) {
					value = KotlinDelegate.invokeFunction(method, getBean(), args, isSuspendingFunction, exchange);
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

	private Mono<Object[]> getMethodArgumentValuesOnScheduler(
			ServerWebExchange exchange, BindingContext bindingContext, Object... providedArgs) {
		Mono<Object[]> argumentValuesMono = getMethodArgumentValues(exchange, bindingContext, providedArgs);
		return this.invocationScheduler != null ? argumentValuesMono.publishOn(this.invocationScheduler) : argumentValuesMono;
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

	@Contract("_, null -> false")
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

		// Copy of CoWebFilter.COROUTINE_CONTEXT_ATTRIBUTE value to avoid compilation errors in Eclipse
		private static final String COROUTINE_CONTEXT_ATTRIBUTE = "org.springframework.web.server.CoWebFilter.context";

		@Nullable
		@SuppressWarnings({"deprecation", "DataFlowIssue"})
		public static Object invokeFunction(Method method, Object target, Object[] args, boolean isSuspendingFunction,
				ServerWebExchange exchange) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

			if (isSuspendingFunction) {
				Object coroutineContext = exchange.getAttribute(COROUTINE_CONTEXT_ATTRIBUTE);
				if (coroutineContext == null) {
					return CoroutinesUtils.invokeSuspendingFunction(method, target, args);
				}
				else {
					return CoroutinesUtils.invokeSuspendingFunction((CoroutineContext) coroutineContext, method, target, args);
				}
			}
			else {
				KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
				// For property accessors
				if (function == null) {
					return method.invoke(target, args);
				}
				if (!KCallablesJvm.isAccessible(function)) {
					KCallablesJvm.setAccessible(function, true);
				}
				Map<KParameter, Object> argMap = CollectionUtils.newHashMap(args.length + 1);
				int index = 0;
				for (KParameter parameter : function.getParameters()) {
					switch (parameter.getKind()) {
						case INSTANCE -> argMap.put(parameter, target);
						case VALUE, EXTENSION_RECEIVER -> {
							Object arg = args[index];
							if (!(parameter.isOptional() && arg == null)) {
								KType type = parameter.getType();
								if (!(type.isMarkedNullable() && arg == null) && type.getClassifier() instanceof KClass<?> kClass
										&& KotlinDetector.isInlineClass(JvmClassMappingKt.getJavaClass(kClass))) {
									KFunction<?> constructor = KClasses.getPrimaryConstructor(kClass);
									if (!KCallablesJvm.isAccessible(constructor)) {
										KCallablesJvm.setAccessible(constructor, true);
									}
									arg = constructor.call(arg);
								}
								argMap.put(parameter, arg);
							}
							index++;
						}
					}
				}
				Object result = function.callBy(argMap);
				if (result != null && KotlinDetector.isInlineClass(result.getClass())) {
					return result.getClass().getDeclaredMethod("unbox-impl").invoke(result);
				}
				return (result == Unit.INSTANCE ? null : result);
			}
		}
	}

}
