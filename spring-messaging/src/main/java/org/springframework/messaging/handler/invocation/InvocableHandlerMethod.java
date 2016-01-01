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

package org.springframework.messaging.handler.invocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.util.ReflectionUtils;

/**
 * Provides a method for invoking the handler method for a given message after resolving its
 * method argument values through registered {@link HandlerMethodArgumentResolver}s.
 *
 * <p>Use {@link #setMessageMethodArgumentResolvers(HandlerMethodArgumentResolver)}
 * to customize the list of argument resolvers.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private HandlerMethodArgumentResolver argumentResolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


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
	 * Construct a new handler method with the given bean instance, method name and parameters.
	 * @param bean the object bean
	 * @param methodName the method name
	 * @param parameterTypes the method parameter types
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		super(bean, methodName, parameterTypes);
	}


	/**
	 * Set {@link HandlerMethodArgumentResolver}s to use to use for resolving method argument values.
	 */
	public void setMessageMethodArgumentResolvers(HandlerMethodArgumentResolver argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	/**
	 * Set the ParameterNameDiscoverer for resolving parameter names when needed
	 * (e.g. default request attribute name).
	 * <p>Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}


	/**
	 * Invoke the method after resolving its argument values in the context of the given message.
	 * <p>Argument values are commonly resolved through {@link HandlerMethodArgumentResolver}s.
	 * The {@code providedArgs} parameter however may supply argument values to be used directly,
	 * i.e. without argument resolution.
	 * @param message the current message being processed
	 * @param providedArgs "given" arguments matched by type, not resolved
	 * @return the raw value returned by the invoked method
	 * @exception Exception raised if no suitable argument resolver can be found,
	 * or if the method raised an exception
	 */
	public Object invoke(Message<?> message, Object... providedArgs) throws Exception {
		Object[] args = getMethodArgumentValues(message, providedArgs);
		if (logger.isTraceEnabled()) {
			StringBuilder sb = new StringBuilder("Invoking [");
			sb.append(getBeanType().getSimpleName()).append(".");
			sb.append(getMethod().getName()).append("] method with arguments ");
			sb.append(Arrays.asList(args));
			logger.trace(sb.toString());
		}
		Object returnValue = doInvoke(args);
		if (logger.isTraceEnabled()) {
			logger.trace("Method [" + getMethod().getName() + "] returned [" + returnValue + "]");
		}
		return returnValue;
	}

	/**
	 * Get the method argument values for the current request.
	 */
	private Object[] getMethodArgumentValues(Message<?> message, Object... providedArgs) throws Exception {
		MethodParameter[] parameters = getMethodParameters();
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			GenericTypeResolver.resolveParameterType(parameter, getBean().getClass());
			args[i] = resolveProvidedArgument(parameter, providedArgs);
			if (args[i] != null) {
				continue;
			}
			if (this.argumentResolvers.supportsParameter(parameter)) {
				try {
					args[i] = this.argumentResolvers.resolveArgument(parameter, message);
					continue;
				}
				catch (Exception ex) {
					if (logger.isDebugEnabled()) {
						logger.debug(getArgumentResolutionErrorMessage("Error resolving argument", i), ex);
					}
					throw ex;
				}
			}
			if (args[i] == null) {
				String msg = getArgumentResolutionErrorMessage("No suitable resolver for argument", i);
				throw new IllegalStateException(msg);
			}
		}
		return args;
	}

	private String getArgumentResolutionErrorMessage(String message, int index) {
		MethodParameter param = getMethodParameters()[index];
		message += " [" + index + "] [type=" + param.getParameterType().getName() + "]";
		return getDetailedErrorMessage(message);
	}

	/**
	 * Adds HandlerMethod details such as the controller type and method
	 * signature to the given error message.
	 * @param message error message to append the HandlerMethod details to
	 */
	protected String getDetailedErrorMessage(String message) {
		StringBuilder sb = new StringBuilder(message).append("\n");
		sb.append("HandlerMethod details: \n");
		sb.append("Controller [").append(getBeanType().getName()).append("]\n");
		sb.append("Method [").append(getBridgedMethod().toGenericString()).append("]\n");
		return sb.toString();
	}

	/**
	 * Attempt to resolve a method parameter from the list of provided argument values.
	 */
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


	/**
	 * Invoke the handler method with the given argument values.
	 */
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

	/**
	 * Assert that the target bean class is an instance of the class where the given
	 * method is declared. In some cases the actual controller instance at request-
	 * processing time may be a JDK dynamic proxy (lazy initialization, prototype
	 * beans, and others). {@code @Controller}'s that require proxying should prefer
	 * class-based proxy mechanisms.
	 */
	private void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String msg = "The mapped controller method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual controller bean class '" +
					targetBeanClass.getName() + "'. If the controller requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(getInvocationErrorMessage(msg, args));
		}
	}

	private String getInvocationErrorMessage(String message, Object[] resolvedArgs) {
		StringBuilder sb = new StringBuilder(getDetailedErrorMessage(message));
		sb.append("Resolved arguments: \n");
		for (int i = 0; i < resolvedArgs.length; i++) {
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

	MethodParameter getAsyncReturnValueType(Object returnValue) {
		return new AsyncResultMethodParameter(returnValue);
	}


	private class AsyncResultMethodParameter extends HandlerMethodParameter {

		private final Object returnValue;

		private final ResolvableType returnType;

		public AsyncResultMethodParameter(Object returnValue) {
			super(-1);
			this.returnValue = returnValue;
			this.returnType = ResolvableType.forType(super.getGenericParameterType()).getGeneric(0);
		}

		@Override
		public Class<?> getParameterType() {
			if (this.returnValue != null) {
				return this.returnValue.getClass();
			}
			if (ResolvableType.NONE.equals(this.returnType)) {
				throw new IllegalArgumentException("Expected Future-like type with generic parameter");
			}
			return this.returnType.getRawClass();
		}

		@Override
		public Type getGenericParameterType() {
			return this.returnType.getType();
		}
	}

}
