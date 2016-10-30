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
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;

/**
 * Extension of HandlerMethod that can invoke the target method after resolving
 * its method arguments.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private static final Mono<Object[]> NO_ARGS = Mono.just(new Object[0]);

	private static final Object NO_VALUE = new Object();


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
	 * @param bindingContext the binding context to use
	 * @param providedArgs optional list of argument values to check by type
	 * (via {@code instanceof}) for resolving method arguments.
	 * @return Publisher that produces a single HandlerResult or an error signal;
	 * never throws an exception
	 */
	public Mono<HandlerResult> invokeForRequest(ServerWebExchange exchange,
			BindingContext bindingContext, Object... providedArgs) {

		return resolveArguments(exchange, bindingContext, providedArgs).then(args -> {
			try {
				Object value = doInvoke(args);
				ModelMap model = bindingContext.getModel();
				HandlerResult handlerResult = new HandlerResult(this, value, getReturnType(), model);
				return Mono.just(handlerResult);
			}
			catch (InvocationTargetException ex) {
				return Mono.error(ex.getTargetException());
			}
			catch (Throwable ex) {
				String msg = getInvocationErrorMessage(args);
				return Mono.error(new IllegalStateException(msg));
			}
		});
	}

	private Mono<Object[]> resolveArguments(ServerWebExchange exchange,
			BindingContext bindingContext, Object... providedArgs) {

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
							return resolver.resolveArgument(param, bindingContext, exchange)
									.defaultIfEmpty(NO_VALUE)
									.doOnError(cause -> {
										if(logger.isDebugEnabled()) {
											logger.debug(getDetailedErrorMessage("Error resolving ", param), cause);
										}
									})
									.log("reactor.unresolved");
						}
						catch (Exception ex) {
							throw getArgError("Error resolving ", param, ex);
						}
					})
					.collect(Collectors.toList());

			// Create Mono with array of resolved values...
			return Mono.when(monos,
					args -> Stream.of(args).map(o -> o != NO_VALUE ? o : null).toArray());
		}
		catch (Throwable ex) {
			return Mono.error(ex);
		}
	}

	private IllegalStateException getArgError(String message, MethodParameter param, Throwable cause) {
		return new IllegalStateException(getDetailedErrorMessage(message, param), cause);
	}

	private String getDetailedErrorMessage(String message, MethodParameter param) {
		StringBuilder sb = new StringBuilder(message);
		sb.append("argument [").append(param.getParameterIndex()).append("] ");
		sb.append("of type [").append(param.getParameterType().getName()).append("] ");
		sb.append("on method [").append(getBridgedMethod().toGenericString()).append("]");
		return sb.toString();
	}

	private Object doInvoke(Object[] args) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Invoking '" + ClassUtils.getQualifiedMethodName(getMethod(), getBeanType()) +
					"' with arguments " + Arrays.toString(args));
		}
		ReflectionUtils.makeAccessible(getBridgedMethod());
		Object returnValue = getBridgedMethod().invoke(getBean(), args);
		if (logger.isTraceEnabled()) {
			logger.trace("Method [" + ClassUtils.getQualifiedMethodName(getMethod(), getBeanType()) +
					"] returned [" + returnValue + "]");
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
