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

package org.springframework.reactive.web.dispatch.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.fn.tuple.Tuple;
import reactor.rx.Streams;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;


/**
 * @author Rossen Stoyanchev
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}


	public void setHandlerMethodArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		this.argumentResolvers.clear();
		this.argumentResolvers.addAll(resolvers);
	}


	public Publisher<Object> invokeForRequest(ReactiveServerHttpRequest request,
			Object... providedArgs) {

		List<Publisher<Object>> argPublishers = getMethodArguments(request, providedArgs);

		Publisher<Object[]> argValues = (!argPublishers.isEmpty() ?
				Streams.zip(argPublishers, this::unwrapOptionalArgValues) :
				Publishers.just(new Object[0]));

		return Publishers.map(argValues, args -> {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking [" + getBeanType().getSimpleName() + "." +
						getMethod().getName() + "] method with arguments " +
						Collections.singletonList(argPublishers));
			}
			Object returnValue = null;
			try {
				returnValue = doInvoke(args);
				if (logger.isTraceEnabled()) {
					logger.trace("Method [" + getMethod().getName() + "] returned " +
							"[" + returnValue + "]");
				}
			}
			catch (Exception ex) {
				// TODO: how to best handle error inside map? (also wrapping hides original ex)
				throw new IllegalStateException(ex);
			}
			return returnValue;
		});
	}

	private List<Publisher<Object>> getMethodArguments(ReactiveServerHttpRequest request,
			Object... providedArgs) {

		MethodParameter[] parameters = getMethodParameters();
		List<Publisher<Object>> valuePublishers = new ArrayList<>(parameters.length);
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			GenericTypeResolver.resolveParameterType(parameter, getBean().getClass());
			Object value = resolveProvidedArgument(parameter, providedArgs);
			if (value != null) {
				valuePublishers.add(Publishers.just(value));
				continue;
			}
			boolean resolved = false;
			for (HandlerMethodArgumentResolver resolver : this.argumentResolvers) {
				if (resolver.supportsParameter(parameter)) {
					try {
						valuePublishers.add(resolver.resolveArgument(parameter, request));
						resolved = true;
						break;
					}
					catch (Exception ex) {
						String msg = buildArgErrorMessage("Error resolving argument", i);
						valuePublishers.add(Publishers.error(new IllegalStateException(msg, ex)));
						break;
					}
				}
			}
			if (!resolved) {
				String msg = buildArgErrorMessage("No suitable resolver for argument", i);
				valuePublishers.add(Publishers.error(new IllegalStateException(msg)));
				break;
			}
		}
		return valuePublishers;
	}

	private String buildArgErrorMessage(String message, int index) {
		MethodParameter param = getMethodParameters()[index];
		message += " [" + index + "] [type=" + param.getParameterType().getName() + "]";
		return getDetailedErrorMessage(message);
	}

	protected String getDetailedErrorMessage(String message) {
		return message + "\n" + "HandlerMethod details: \n" +
				"Controller [" + getBeanType().getName() + "]\n" +
				"Method [" + getBridgedMethod().toGenericString() + "]\n";
	}

	private Object resolveProvidedArgument(MethodParameter parameter, Object... providedArgs) {
		if (providedArgs == null) {
			return null;
		}
		for (Object providedArg : providedArgs) {
			if (parameter.getParameterType().isInstance(providedArg)) {
				return providedArg;
			}
		}
		return null;
	}

	private void unwrapOptionalArgValues(Object[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Optional) {
				Optional optional = (Optional) args[i];
				args[i] = optional.isPresent() ? optional.get() : null;
			}
		}
	}

	private Object[] unwrapOptionalArgValues(Tuple tuple) {
		Object[] args = new Object[tuple.size()];
		for (int i = 0; i < tuple.size(); i++) {
			args[i] = tuple.get(i);
			if (args[i] instanceof Optional) {
				Optional optional = (Optional) args[i];
				args[i] = optional.isPresent() ? optional.get() : null;
			}
		}
		return args;
	}

	protected Object doInvoke(Object... args) throws Exception {
		ReflectionUtils.makeAccessible(getBridgedMethod());
		try {
			return getBridgedMethod().invoke(getBean(), args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(getBridgedMethod(), getBean(), args);
			throw new IllegalStateException(getInvocationErrorMessage(ex.getMessage(), args), ex);
		}
		catch (InvocationTargetException ex) {
			// Unwrap for HandlerExceptionResolvers ...
			Throwable targetException = ex.getTargetException();
			if (targetException instanceof RuntimeException) {
				throw (RuntimeException) targetException;
			}
			else if (targetException instanceof Error) {
				throw (Error) targetException;
			}
			else if (targetException instanceof Exception) {
				throw (Exception) targetException;
			}
			else {
				String msg = getInvocationErrorMessage("Failed to invoke controller method", args);
				throw new IllegalStateException(msg, targetException);
			}
		}
	}

	private void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String msg = "The mapped controller method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual controller bean instance '" +
					targetBeanClass.getName() + "'. If the controller requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(getInvocationErrorMessage(msg, args));
		}
	}

	private String getInvocationErrorMessage(String message, Object[] resolvedArgs) {
		StringBuilder sb = new StringBuilder(getDetailedErrorMessage(message));
		sb.append("Resolved arguments: \n");
		for (int i=0; i < resolvedArgs.length; i++) {
			sb.append("[").append(i).append("] ");
			if (resolvedArgs[i] == null) {
				sb.append("[null] \n");
			}
			else {
				sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
				sb.append("[value=").append(resolvedArgs[i]).append("]\n");
			}
		}
		return sb.toString();
	}

}
