/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.ui.ModelMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * Provides a way to invoke a handler method after resolving its method argument values through 
 * {@link HandlerMethodArgumentResolver}s in the context of the current request.
 * 
 * <p>Resolving argument values often requires a {@link WebDataBinder} for data binding and type conversion. 
 * Use {@link #setDataBinderFactory(WebDataBinderFactory)} to provide a factory for that. The list of argument 
 * resolvers can be set via {@link #setArgumentResolverContainer(HandlerMethodArgumentResolverContainer)}.
 *  
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private HandlerMethodArgumentResolverContainer argumentResolvers = new HandlerMethodArgumentResolverContainer();

	private WebDataBinderFactory dataBinderFactory;

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	/**
	 * Constructs a new invocable handler method with the given bean instance and method.
	 * @param bean the bean instance
	 * @param method the method
	 */
	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}

	/**
	 * Constructs a new invocable handler method with the given bean instance, method name and parameters.
	 * @param bean the object bean
	 * @param methodName the method name
	 * @param parameterTypes the method parameter types
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public InvocableHandlerMethod(
			Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		super(bean, methodName, parameterTypes);
	}

	/**
	 * Sets the {@link WebDataBinderFactory} to be passed to argument resolvers that require a 
	 * {@link WebDataBinder} to do type conversion or data binding on the method argument value.
	 * 
	 * @param dataBinderFactory the data binder factory.
	 */
	public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
		this.dataBinderFactory = dataBinderFactory;
	}

	/**
	 * Set {@link HandlerMethodArgumentResolver}s to use to use for resolving method argument values.
	 */
	public void setArgumentResolverContainer(HandlerMethodArgumentResolverContainer argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	/**
	 * Set the ParameterNameDiscoverer to use for resolving method parameter names if needed
	 * (e.g. for default attribute names).
	 * <p>Default is a {@link org.springframework.core.LocalVariableTableParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Invoke the method after resolving its argument values based on the given request.
	 * <p>Most argument values are resolved with the help of {@link HandlerMethodArgumentResolver}s 
	 * configured via {@link #setArgumentResolverContainer(HandlerMethodArgumentResolverContainer)}. 
	 * However, the {@code provideArgs} parameter can be used to supply argument values for use
	 * directly rather than relying on argument resolution - e.g. {@link WebDataBinder}, 
	 * {@link SessionStatus}, or the thrown exception in a HandlerExceptionResolver.
	 * @param request the current request
	 * @param model the model used throughout the current request
	 * @param providedArgs argument values to try to use, thus bypassing argument resolution
	 * @return the raw value returned by the invoked method
	 */
	public final Object invokeForRequest(NativeWebRequest request, ModelMap model, Object... providedArgs)
			throws Exception {

		Object[] args = getMethodArguments(request, model, providedArgs);

		if (logger.isTraceEnabled()) {
			StringBuilder builder = new StringBuilder("Invoking [");
			builder.append(this.getMethod().getName()).append("] method with arguments ");
			builder.append(Arrays.asList(args));
			logger.trace(builder.toString());
		}

		Object returnValue = invoke(args);

		if (logger.isTraceEnabled()) {
			logger.trace("Method [" + this.getMethod().getName() + "] returned [" + returnValue + "]");
		}

		return returnValue;
	}

	private Object[] getMethodArguments(NativeWebRequest request, ModelMap model, Object... providedArgs)
			throws Exception {
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
				args[i] = this.argumentResolvers.resolveArgument(parameter, model, request, dataBinderFactory);
			}
			else {
				throw new IllegalStateException("Cannot resolve argument index=" + parameter.getParameterIndex() + ""
						+ ", name=" + parameter.getParameterName() + ", type=" + parameter.getParameterType()
						+ " in method " + toString());
			}
		}
		return args;
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
	
	/**
	 * Invokes this handler method with the given argument values.
	 * @param args the argument values
	 * @return the result of the invocation
	 * @throws Exception when the method invocation results in an exception
	 */
	private Object invoke(Object... args) throws Exception {
		ReflectionUtils.makeAccessible(this.getBridgedMethod());
		try {
			return getBridgedMethod().invoke(getBean(), args);
		}
		catch (IllegalArgumentException ex) {
			handleIllegalArgumentException(ex, args);
			throw ex;
		}
		catch (InvocationTargetException ex) {
			handleInvocationTargetException(ex);
			throw new IllegalStateException(
					"Unexpected exception thrown by method - " + ex.getTargetException().getClass().getName() + ": " +
							ex.getTargetException().getMessage());
		}
	}

	private void handleIllegalArgumentException(IllegalArgumentException ex, Object... args) {
		StringBuilder builder = new StringBuilder(ex.getMessage());
		builder.append(" :: method=").append(getBridgedMethod().toGenericString());
		builder.append(" :: invoked with handler type=").append(getBeanType().getName());
		
		if (args != null &&  args.length > 0) {
			builder.append(" and argument types ");
			for (int i = 0; i < args.length; i++) {
				builder.append(" : arg[").append(i).append("] ").append(args[i].getClass());
			}
		}
		else {
			builder.append(" and 0 arguments");
		}
		
		throw new IllegalArgumentException(builder.toString(), ex);
	}
	
	private void handleInvocationTargetException(InvocationTargetException ex) throws Exception {
		Throwable targetException = ex.getTargetException();
		if (targetException instanceof RuntimeException) {
			throw (RuntimeException) targetException;
		}
		if (targetException instanceof Error) {
			throw (Error) targetException;
		}
		if (targetException instanceof Exception) {
			throw (Exception) targetException;
		}
	}

	/**
	 * Whether any of the registered {@link HandlerMethodArgumentResolver}s use the response argument.
	 * @see HandlerMethodProcessor#usesResponseArgument(MethodParameter)
	 */
	protected boolean usesResponseArgument() {
		MethodParameter[] methodParameters = getMethodParameters();
		for (MethodParameter methodParameter : methodParameters) {
			if (this.argumentResolvers.usesResponseArgument(methodParameter)) {
				return true;
			}
		}
		return false;
	}
	
}