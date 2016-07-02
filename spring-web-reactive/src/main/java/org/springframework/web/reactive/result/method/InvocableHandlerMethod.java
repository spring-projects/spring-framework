/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;


/**
 * @author Rossen Stoyanchev
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Mono<Object[]> NO_ARGS = Mono.just(new Object[0]);

	private final static Object NO_VALUE = new Object();


	private List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}


	public void setHandlerMethodArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.resolvers.clear();
		this.resolvers.addAll(resolvers);
	}

	@Override
	protected Method getBridgedMethod() {
		return super.getBridgedMethod();
	}


	/**
	 * Invoke the method and return a Publisher for the return value.
	 * @param exchange the current exchange
	 * @param model the model for request handling
	 * @param providedArgs optional list of argument values to check by type
	 * (via {@code instanceof}) for resolving method arguments.
	 * @return Publisher that produces a single HandlerResult or an error signal;
	 * never throws an exception
	 */
	public Mono<HandlerResult> invokeForRequest(ServerWebExchange exchange, ModelMap model,
			Object... providedArgs) {

		return resolveArguments(exchange, model, providedArgs).then(args -> {
			try {
				Object value = doInvoke(args);
				HandlerResult handlerResult = new HandlerResult(this, value, getReturnType(), model);
				return Mono.just(handlerResult);
			}
			catch (InvocationTargetException ex) {
				return Mono.error(ex.getTargetException());
			}
			catch (Throwable ex) {
				String s = getInvocationErrorMessage(args);
				return Mono.error(new IllegalStateException(s));
			}
		});
	}

	private Mono<Object[]> resolveArguments(ServerWebExchange exchange, ModelMap model, Object... providedArgs) {
		if (ObjectUtils.isEmpty(getMethodParameters())) {
			return NO_ARGS;
		}
		try {
			List<Mono<Object>> monos = Stream.of(getMethodParameters())
					.map(param -> {
						param.initParameterNameDiscovery(this.parameterNameDiscoverer);
						GenericTypeResolver.resolveParameterType(param, getBean().getClass());
						if (!ObjectUtils.isEmpty(providedArgs)) {
							for (Object providedArg : providedArgs) {
								if (param.getParameterType().isInstance(providedArg)) {
									return Mono.just(providedArg).log("reactor.resolved");
								}
							}
						}
						HandlerMethodArgumentResolver resolver = this.resolvers.stream()
								.filter(r -> r.supportsParameter(param))
								.findFirst()
								.orElseThrow(() -> getArgError("No resolver for ", param, null));
						try {
							return resolver.resolveArgument(param, model, exchange)
									.defaultIfEmpty(NO_VALUE)
									.otherwise(ex -> Mono.error(getArgError("Error resolving ", param, ex)))
									.log("reactor.unresolved");
						}
						catch (Exception ex) {
							throw getArgError("Error resolving ", param, ex);
						}
					})
					.collect(Collectors.toList());

			return Mono.when(monos).log("reactor.unresolved").map(args ->
					Stream.of(args).map(o -> o != NO_VALUE ? o : null).toArray());
		}
		catch (Throwable ex) {
			return Mono.error(ex);
		}
	}

	private IllegalStateException getArgError(String message, MethodParameter param, Throwable cause) {
		return new IllegalStateException(message +
				"argument [" + param.getParameterIndex() + "] " +
				"of type [" + param.getParameterType().getName() + "] " +
				"on method [" + getBridgedMethod().toGenericString() + "]", cause);
	}

	private Object doInvoke(Object[] args) throws Exception {
		if (logger.isTraceEnabled()) {
			String target = getBeanType().getSimpleName() + "." + getMethod().getName();
			logger.trace("Invoking [" + target + "] method with arguments " + Arrays.toString(args));
		}
		ReflectionUtils.makeAccessible(getBridgedMethod());
		Object returnValue = getBridgedMethod().invoke(getBean(), args);
		if (logger.isTraceEnabled()) {
			String target = getBeanType().getSimpleName() + "." + getMethod().getName();
			logger.trace("Method [" + target + "] returned [" + returnValue + "]");
		}
		return returnValue;
	}

	private String getInvocationErrorMessage(Object[] args) {
		String argumentDetails = IntStream.range(0, args.length)
				.mapToObj(i -> (args[i] != null ?
						"[" + i + "][type=" + args[i].getClass().getName() + "][value=" + args[i] + "]" :
						"[" + i + "][null]"))
				.collect(Collectors.joining(",", " ", " "));
		return "Failed to invoke controller with resolved arguments:" + argumentDetails +
				"on method [" + getBridgedMethod().toGenericString() + "]";
	}

}
