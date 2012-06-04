/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.portlet.mvc.annotation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ClientDataRequest;
import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.MimeResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.WindowState;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletWebRequest;
import org.springframework.web.portlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.View;

/**
 * Implementation of the {@link org.springframework.web.portlet.HandlerExceptionResolver} interface that handles
 * exceptions through the {@link ExceptionHandler} annotation.
 *
 * <p>This exception resolver is enabled by default in the {@link org.springframework.web.portlet.DispatcherPortlet}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class AnnotationMethodHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

	// dummy method placeholder
	private static final Method NO_METHOD_FOUND = ClassUtils.getMethodIfAvailable(System.class, "currentTimeMillis", (Class<?>[]) null);

	private WebArgumentResolver[] customArgumentResolvers;

	private final Map<Class<?>, Map<Class<? extends Throwable>, Method>> exceptionHandlerCache = new ConcurrentHashMap<Class<?>, Map<Class<? extends Throwable>, Method>>();

	/**
	 * Set a custom ArgumentResolvers to use for special method parameter types.
	 * <p>Such a custom ArgumentResolver will kick in first, having a chance to resolve
	 * an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolver(WebArgumentResolver argumentResolver) {
		this.customArgumentResolvers = new WebArgumentResolver[]{argumentResolver};
	}

	/**
	 * Set one or more custom ArgumentResolvers to use for special method parameter types.
	 * <p>Any such custom ArgumentResolver will kick in first, having a chance to resolve
	 * an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolvers(WebArgumentResolver[] argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}


	@Override
	protected ModelAndView doResolveException(
			PortletRequest request, MimeResponse response, Object handler, Exception ex) {

		if (handler != null) {
			Method handlerMethod = findBestExceptionHandlerMethod(handler, ex);
			if (handlerMethod != null) {
				NativeWebRequest webRequest = new PortletWebRequest(request, response);
				try {
					Object[] args = resolveHandlerArguments(handlerMethod, handler, webRequest, ex);
					if (logger.isDebugEnabled()) {
						logger.debug("Invoking request handler method: " + handlerMethod);
					}
					Object retVal = doInvokeMethod(handlerMethod, handler, args);
					return getModelAndView(retVal);
				}
				catch (Exception invocationEx) {
					logger.error("Invoking request method resulted in exception : " + handlerMethod, invocationEx);
				}
			}
		}
		return null;
	}

	/**
	 * Finds the handler method that matches the thrown exception best.
	 * @param handler the handler object
	 * @param thrownException the exception to be handled
	 * @return the best matching method; or <code>null</code> if none is found
	 */
	private Method findBestExceptionHandlerMethod(Object handler, final Exception thrownException) {
		final Class<?> handlerType = handler.getClass();
		final Class<? extends Throwable> thrownExceptionType = thrownException.getClass();
		Method handlerMethod = null;

		Map<Class<? extends Throwable>, Method> handlers = exceptionHandlerCache.get(handlerType);

		if (handlers != null) {
			handlerMethod = handlers.get(thrownExceptionType);
			if (handlerMethod != null) {
				return (handlerMethod == NO_METHOD_FOUND ? null : handlerMethod);
			}
		} else {
			handlers = new ConcurrentHashMap<Class<? extends Throwable>, Method>();
			exceptionHandlerCache.put(handlerType, handlers);
		}

		final Map<Class<? extends Throwable>, Method> resolverMethods = handlers;

		ReflectionUtils.doWithMethods(handlerType, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) {
				method = ClassUtils.getMostSpecificMethod(method, handlerType);
				List<Class<? extends Throwable>> handledExceptions = getHandledExceptions(method);
				for (Class<? extends Throwable> handledException : handledExceptions) {
					if (handledException.isAssignableFrom(thrownExceptionType)) {
						if (!resolverMethods.containsKey(handledException)) {
							resolverMethods.put(handledException, method);
						}
						else {
							Method oldMappedMethod = resolverMethods.get(handledException);
							if (!oldMappedMethod.equals(method)) {
								throw new IllegalStateException(
										"Ambiguous exception handler mapped for " + handledException + "]: {" +
												oldMappedMethod + ", " + method + "}.");
							}
						}
					}
				}
			}
		});

		handlerMethod = getBestMatchingMethod(resolverMethods, thrownException);
		handlers.put(thrownExceptionType, (handlerMethod == null ? NO_METHOD_FOUND : handlerMethod));
		return handlerMethod;
	}

	/**
	 * Returns all the exception classes handled by the given method.
	 * <p>Default implementation looks for exceptions in the {@linkplain ExceptionHandler#value() annotation},
	 * or - if that annotation element is empty - any exceptions listed in the method parameters if the
	 * method is annotated with {@code @ExceptionHandler}.
	 * @param method the method
	 * @return the handled exceptions
	 */
	@SuppressWarnings("unchecked")
	protected List<Class<? extends Throwable>> getHandledExceptions(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>();
		ExceptionHandler exceptionHandler = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
		if (exceptionHandler != null) {
			if (!ObjectUtils.isEmpty(exceptionHandler.value())) {
				result.addAll(Arrays.asList(exceptionHandler.value()));
			}
			else {
				for (Class<?> param : method.getParameterTypes()) {
					if (Throwable.class.isAssignableFrom(param)) {
						result.add((Class<? extends Throwable>) param);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Uses the {@link DepthComparator} to find the best matching method
	 * @return the best matching method or {@code null}.
	 */
	private Method getBestMatchingMethod(
			Map<Class<? extends Throwable>, Method> resolverMethods, Exception thrownException) {

		if (resolverMethods.isEmpty()) {
			return null;
		}
		Class<? extends Throwable> closestMatch =
				ExceptionDepthComparator.findClosestMatch(resolverMethods.keySet(), thrownException);
		Method method = resolverMethods.get(closestMatch);
		return ((method == null) || (NO_METHOD_FOUND == method)) ? null : method;
	}

	/**
	 * Resolves the arguments for the given method. Delegates to {@link #resolveCommonArgument}.
	 */
	private Object[] resolveHandlerArguments(Method handlerMethod, Object handler,
			NativeWebRequest webRequest, Exception thrownException) throws Exception {

		Class<?>[] paramTypes = handlerMethod.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		Class<?> handlerType = handler.getClass();
		for (int i = 0; i < args.length; i++) {
			MethodParameter methodParam = new MethodParameter(handlerMethod, i);
			GenericTypeResolver.resolveParameterType(methodParam, handlerType);
			Class<?> paramType = methodParam.getParameterType();
			Object argValue = resolveCommonArgument(methodParam, webRequest, thrownException);
			if (argValue != WebArgumentResolver.UNRESOLVED) {
				args[i] = argValue;
			}
			else {
				throw new IllegalStateException("Unsupported argument [" + paramType.getName() +
						"] for @ExceptionHandler method: " + handlerMethod);
			}
		}
		return args;
	}

	/**
	 * Resolves common method arguments. Delegates to registered
	 * {@link #setCustomArgumentResolver argumentResolvers} first,
	 * then checking {@link #resolveStandardArgument}.
	 * @param methodParameter the method parameter
	 * @param webRequest the request
	 * @param thrownException the exception thrown
	 * @return the argument value, or {@link org.springframework.web.bind.support.WebArgumentResolver#UNRESOLVED}
	 */
	protected Object resolveCommonArgument(MethodParameter methodParameter, NativeWebRequest webRequest,
			Exception thrownException) throws Exception {

		// Invoke custom argument resolvers if present...
		if (this.customArgumentResolvers != null) {
			for (WebArgumentResolver argumentResolver : this.customArgumentResolvers) {
				Object value = argumentResolver.resolveArgument(methodParameter, webRequest);
				if (value != WebArgumentResolver.UNRESOLVED) {
					return value;
				}
			}
		}

		// Resolution of standard parameter types...
		Class<?> paramType = methodParameter.getParameterType();
		Object value = resolveStandardArgument(paramType, webRequest, thrownException);
		if (value != WebArgumentResolver.UNRESOLVED && !ClassUtils.isAssignableValue(paramType, value)) {
			throw new IllegalStateException("Standard argument type [" + paramType.getName() +
					"] resolved to incompatible value of type [" + (value != null ? value.getClass() : null) +
					"]. Consider declaring the argument type in a less specific fashion.");
		}
		return value;
	}

	/**
	 * Resolves standard method arguments. The default implementation handles {@link NativeWebRequest},
	 * {@link ServletRequest}, {@link ServletResponse}, {@link HttpSession}, {@link Principal},
	 * {@link Locale}, request {@link InputStream}, request {@link Reader}, response {@link OutputStream},
	 * response {@link Writer}, and the given {@code thrownException}.
	 * @param parameterType the method parameter type
	 * @param webRequest the request
	 * @param thrownException the exception thrown
	 * @return the argument value, or {@link org.springframework.web.bind.support.WebArgumentResolver#UNRESOLVED}
	 */
	protected Object resolveStandardArgument(Class<?> parameterType, NativeWebRequest webRequest,
			Exception thrownException) throws Exception {

		if (parameterType.isInstance(thrownException)) {
			return thrownException;
		}
		else if (WebRequest.class.isAssignableFrom(parameterType)) {
			return webRequest;
		}

		PortletRequest request = webRequest.getNativeRequest(PortletRequest.class);
		PortletResponse response = webRequest.getNativeResponse(PortletResponse.class);

		if (PortletRequest.class.isAssignableFrom(parameterType)) {
			return request;
		}
		else if (PortletResponse.class.isAssignableFrom(parameterType)) {
			return response;
		}
		else if (PortletSession.class.isAssignableFrom(parameterType)) {
			return request.getPortletSession();
		}
		else if (PortletPreferences.class.isAssignableFrom(parameterType)) {
			return request.getPreferences();
		}
		else if (PortletMode.class.isAssignableFrom(parameterType)) {
			return request.getPortletMode();
		}
		else if (WindowState.class.isAssignableFrom(parameterType)) {
			return request.getWindowState();
		}
		else if (PortalContext.class.isAssignableFrom(parameterType)) {
			return request.getPortalContext();
		}
		else if (Principal.class.isAssignableFrom(parameterType)) {
			return request.getUserPrincipal();
		}
		else if (Locale.class.equals(parameterType)) {
			return request.getLocale();
		}
		else if (InputStream.class.isAssignableFrom(parameterType)) {
			if (!(request instanceof ClientDataRequest)) {
				throw new IllegalStateException("InputStream can only get obtained for Action/ResourceRequest");
			}
			return ((ClientDataRequest) request).getPortletInputStream();
		}
		else if (Reader.class.isAssignableFrom(parameterType)) {
			if (!(request instanceof ClientDataRequest)) {
				throw new IllegalStateException("Reader can only get obtained for Action/ResourceRequest");
			}
			return ((ClientDataRequest) request).getReader();
		}
		else if (OutputStream.class.isAssignableFrom(parameterType)) {
			if (!(response instanceof MimeResponse)) {
				throw new IllegalStateException("OutputStream can only get obtained for Render/ResourceResponse");
			}
			return ((MimeResponse) response).getPortletOutputStream();
		}
		else if (Writer.class.isAssignableFrom(parameterType)) {
			if (!(response instanceof MimeResponse)) {
				throw new IllegalStateException("Writer can only get obtained for Render/ResourceResponse");
			}
			return ((MimeResponse) response).getWriter();
		}
		else if (Event.class.equals(parameterType)) {
			if (!(request instanceof EventRequest)) {
				throw new IllegalStateException("Event can only get obtained from EventRequest");
			}
			return ((EventRequest) request).getEvent();
		}
		else {
			return WebArgumentResolver.UNRESOLVED;
		}
	}

	private Object doInvokeMethod(Method method, Object target, Object[] args) throws Exception {
		ReflectionUtils.makeAccessible(method);
		try {
			return method.invoke(target, args);
		}
		catch (InvocationTargetException ex) {
			ReflectionUtils.rethrowException(ex.getTargetException());
		}
		throw new IllegalStateException("Should never get here");
	}

	@SuppressWarnings("unchecked")
	private ModelAndView getModelAndView(Object returnValue) {
		if (returnValue instanceof ModelAndView) {
			return (ModelAndView) returnValue;
		}
		else if (returnValue instanceof Model) {
			return new ModelAndView().addAllObjects(((Model) returnValue).asMap());
		}
		else if (returnValue instanceof Map) {
			return new ModelAndView().addAllObjects((Map) returnValue);
		}
		else if (returnValue instanceof View) {
			return new ModelAndView(returnValue);
		}
		else if (returnValue instanceof String) {
			return new ModelAndView((String) returnValue);
		}
		else if (returnValue == null) {
			return new ModelAndView();
		}
		else {
			throw new IllegalArgumentException("Invalid handler method return value: " + returnValue);
		}
	}

}
