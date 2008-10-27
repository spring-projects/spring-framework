/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.support.HandlerMethodInvoker;
import org.springframework.web.bind.annotation.support.HandlerMethodResolver;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.multiaction.InternalPathMethodNameResolver;
import org.springframework.web.servlet.mvc.multiaction.MethodNameResolver;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * Implementation of the {@link org.springframework.web.servlet.HandlerAdapter}
 * interface that maps handler methods based on HTTP paths, HTTP methods and
 * request parameters expressed through the {@link RequestMapping} annotation.
 *
 * <p>Supports request parameter binding through the {@link RequestParam} annotation.
 * Also supports the {@link ModelAttribute} annotation for exposing model attribute
 * values to the view, as well as {@link InitBinder} for binder initialization methods
 * and {@link SessionAttributes} for automatic session management of specific attributes.
 *
 * <p>This adapter can be customized through various bean properties.
 * A common use case is to apply shared binder initialization logic through
 * a custom {@link #setWebBindingInitializer WebBindingInitializer}.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 2.5
 * @see #setPathMatcher
 * @see #setMethodNameResolver
 * @see #setWebBindingInitializer
 * @see #setSessionAttributeStore
 */
public class AnnotationMethodHandlerAdapter extends WebContentGenerator implements HandlerAdapter {

	/**
	 * Log category to use when no mapped handler is found for a request.
	 * @see #pageNotFoundLogger
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";


	/**
	 * Additional logger to use when no mapped handler is found for a request.
	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private MethodNameResolver methodNameResolver = new InternalPathMethodNameResolver();

	private WebBindingInitializer webBindingInitializer;

	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

	private int cacheSecondsForSessionAttributeHandlers = 0;

	private boolean synchronizeOnSession = false;

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private WebArgumentResolver[] customArgumentResolvers;

	private final Map<Class<?>, ServletHandlerMethodResolver> methodResolverCache =
			new ConcurrentHashMap<Class<?>, ServletHandlerMethodResolver>();


	public AnnotationMethodHandlerAdapter() {
		// no restriction of HTTP methods by default
		super(false);
	}


	/**
	 * Set if URL lookup should always use the full path within the current servlet
	 * context. Else, the path within the current servlet mapping is used if applicable
	 * (that is, in the case of a ".../*" servlet mapping in web.xml).
	 * <p>Default is "false".
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Set if context path and request URI should be URL-decoded. Both are returned
	 * <i>undecoded</i> by the Servlet API, in contrast to the servlet path.
	 * <p>Uses either the request encoding or the default encoding according
	 * to the Servlet spec (ISO-8859-1).
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple HandlerMappings
	 * and HandlerAdapters.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Set the MethodNameResolver to use for resolving default handler methods
	 * (carrying an empty <code>@RequestMapping</code> annotation).
	 * <p>Will only kick in when the handler method cannot be resolved uniquely
	 * through the annotation metadata already.
	 */
	public void setMethodNameResolver(MethodNameResolver methodNameResolver) {
		this.methodNameResolver = methodNameResolver;
	}

	/**
	 * Specify a WebBindingInitializer which will apply pre-configured
	 * configuration to every DataBinder that this controller uses.
	 */
	public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * Specify the strategy to store session attributes with.
	 * <p>Default is {@link org.springframework.web.bind.support.DefaultSessionAttributeStore},
	 * storing session attributes in the HttpSession, using the same
	 * attribute name as in the model.
	 */
	public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore must not be null");
		this.sessionAttributeStore = sessionAttributeStore;
	}

	/**
	 * Cache content produced by <code>@SessionAttributes</code> annotated handlers
	 * for the given number of seconds. Default is 0, preventing caching completely.
	 * <p>In contrast to the "cacheSeconds" property which will apply to all general
	 * handlers (but not to <code>@SessionAttributes</code> annotated handlers), this
	 * setting will apply to <code>@SessionAttributes</code> annotated handlers only.
	 * @see #setCacheSeconds
	 * @see org.springframework.web.bind.annotation.SessionAttributes
	 */
	public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
		this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
	}

	/**
	 * Set if controller execution should be synchronized on the session,
	 * to serialize parallel invocations from the same client.
	 * <p>More specifically, the execution of each handler method will get
	 * synchronized if this flag is "true". The best available session mutex
	 * will be used for the synchronization; ideally, this will be a mutex
	 * exposed by HttpSessionMutexListener.
	 * <p>The session mutex is guaranteed to be the same object during
	 * the entire lifetime of the session, available under the key defined
	 * by the <code>SESSION_MUTEX_ATTRIBUTE</code> constant. It serves as a
	 * safe reference to synchronize on for locking on the current session.
	 * <p>In many cases, the HttpSession reference itself is a safe mutex
	 * as well, since it will always be the same object reference for the
	 * same active logical session. However, this is not guaranteed across
	 * different servlet containers; the only 100% safe way is a session mutex.
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 * @see org.springframework.web.util.WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
	 */
	public void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * Set the ParameterNameDiscoverer to use for resolving method parameter
	 * names if needed (e.g. for default attribute names).
	 * <p>Default is a {@link org.springframework.core.LocalVariableTableParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Set a custom ArgumentResolvers to use for special method parameter types.
	 * Such a custom ArgumentResolver will kick in first, having a chance to
	 * resolve an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolver(WebArgumentResolver argumentResolver) {
		this.customArgumentResolvers = new WebArgumentResolver[] {argumentResolver};
	}

	/**
	 * Set one or more custom ArgumentResolvers to use for special method
	 * parameter types. Any such custom ArgumentResolver will kick in first,
	 * having a chance to resolve an argument value before the standard
	 * argument handling kicks in.
	 */
	public void setCustomArgumentResolvers(WebArgumentResolver[] argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}


	public boolean supports(Object handler) {
		return getMethodResolver(handler).hasHandlerMethods();
	}

	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		if (handler.getClass().getAnnotation(SessionAttributes.class) != null) {
			// Always prevent caching in case of session attribute management.
			checkAndPrepare(request, response, this.cacheSecondsForSessionAttributeHandlers, true);
			// Prepare cached set of session attributes names.
		}
		else {
			// Uses configured default cacheSeconds setting.
			checkAndPrepare(request, response, true);
		}

		// Execute invokeHandlerMethod in synchronized block if required.
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					return invokeHandlerMethod(request, response, handler);
				}
			}
		}

		return invokeHandlerMethod(request, response, handler);
	}

	protected ModelAndView invokeHandlerMethod(
			HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		try {
			ServletHandlerMethodResolver methodResolver = getMethodResolver(handler);
			Method handlerMethod = methodResolver.resolveHandlerMethod(request);
			ServletHandlerMethodInvoker methodInvoker = new ServletHandlerMethodInvoker(methodResolver);
			ServletWebRequest webRequest = new ServletWebRequest(request, response);
			ExtendedModelMap implicitModel = new BindingAwareModelMap();

			Object result = methodInvoker.invokeHandlerMethod(handlerMethod, handler, webRequest, implicitModel);
			ModelAndView mav =
					methodInvoker.getModelAndView(handlerMethod, handler.getClass(), result, implicitModel, webRequest);
			methodInvoker.updateModelAttributes(
					handler, (mav != null ? mav.getModel() : null), implicitModel, webRequest);
			return mav;
		}
		catch (NoSuchRequestHandlingMethodException ex) {
			return handleNoSuchRequestHandlingMethod(ex, request, response);
		}
	}

	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1;
	}


	/**
	 * Handle the case where no request handler method was found.
	 * <p>The default implementation logs a warning and sends an HTTP 404 error.
	 * Alternatively, a fallback view could be chosen, or the
	 * NoSuchRequestHandlingMethodException could be rethrown as-is.
	 * @param ex the NoSuchRequestHandlingMethodException to be handled
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return a ModelAndView to render, or <code>null</code> if handled directly
	 * @throws Exception an Exception that should be thrown as result of the servlet request
	 */
	protected ModelAndView handleNoSuchRequestHandlingMethod(
			NoSuchRequestHandlingMethodException ex, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		pageNotFoundLogger.warn(ex.getMessage());
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		return null;
	}

	/**
	 * Template method for creating a new ServletRequestDataBinder instance.
	 * <p>The default implementation creates a standard ServletRequestDataBinder.
	 * This can be overridden for custom ServletRequestDataBinder subclasses.
	 * @param request current HTTP request
	 * @param target the target object to bind onto (or <code>null</code>
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the objectName of the target object
	 * @return the ServletRequestDataBinder instance to use
	 * @throws Exception in case of invalid state or arguments
	 * @see ServletRequestDataBinder#bind(javax.servlet.ServletRequest)
	 * @see ServletRequestDataBinder#convertIfNecessary(Object, Class, MethodParameter)
	 */
	protected ServletRequestDataBinder createBinder(
			HttpServletRequest request, Object target, String objectName) throws Exception {

		return new ServletRequestDataBinder(target, objectName);
	}

	/**
	 * Build a HandlerMethodResolver for the given handler type.
	 */
	private ServletHandlerMethodResolver getMethodResolver(Object handler) {
		Class handlerClass = ClassUtils.getUserClass(handler);
		ServletHandlerMethodResolver resolver = this.methodResolverCache.get(handlerClass);
		if (resolver == null) {
			resolver = new ServletHandlerMethodResolver(handlerClass);
			this.methodResolverCache.put(handlerClass, resolver);
		}
		return resolver;
	}


	private class ServletHandlerMethodResolver extends HandlerMethodResolver {

		public ServletHandlerMethodResolver(Class<?> handlerType) {
			super(handlerType);
		}

		public Method resolveHandlerMethod(HttpServletRequest request) throws ServletException {
			String lookupPath = urlPathHelper.getLookupPathForRequest(request);
			Map<RequestMappingInfo, Method> targetHandlerMethods = new LinkedHashMap<RequestMappingInfo, Method>();
			Map<RequestMappingInfo, String> targetPathMatches = new LinkedHashMap<RequestMappingInfo, String>();
			String resolvedMethodName = null;
			for (Method handlerMethod : getHandlerMethods()) {
				RequestMappingInfo mappingInfo = new RequestMappingInfo();
				RequestMapping mapping = AnnotationUtils.findAnnotation(handlerMethod, RequestMapping.class);
				mappingInfo.paths = mapping.value();
				if (!hasTypeLevelMapping() || !Arrays.equals(mapping.method(), getTypeLevelMapping().method())) {
					mappingInfo.methods = mapping.method();
				}
				if (!hasTypeLevelMapping() || !Arrays.equals(mapping.params(), getTypeLevelMapping().params())) {
					mappingInfo.params = mapping.params();
				}
				boolean match = false;
				if (mappingInfo.paths.length > 0) {
					for (String mappedPath : mappingInfo.paths) {
						if (isPathMatch(mappedPath, lookupPath)) {
							if (checkParameters(mappingInfo, request)) {
								match = true;
								targetPathMatches.put(mappingInfo, mappedPath);
							}
							else {
								break;
							}
						}
					}
				}
				else {
					// No paths specified: parameter match sufficient.
					match = checkParameters(mappingInfo, request);
					if (match && mappingInfo.methods.length == 0 && mappingInfo.params.length == 0 &&
							resolvedMethodName != null && !resolvedMethodName.equals(handlerMethod.getName())) {
						match = false;
					}
				}
				if (match) {
					Method oldMappedMethod = targetHandlerMethods.put(mappingInfo, handlerMethod);
					if (oldMappedMethod != null && oldMappedMethod != handlerMethod) {
						if (methodNameResolver != null && mappingInfo.paths.length == 0) {
							if (!oldMappedMethod.getName().equals(handlerMethod.getName())) {
								if (resolvedMethodName == null) {
									resolvedMethodName = methodNameResolver.getHandlerMethodName(request);
								}
								if (!resolvedMethodName.equals(oldMappedMethod.getName())) {
									oldMappedMethod = null;
								}
								if (!resolvedMethodName.equals(handlerMethod.getName())) {
									if (oldMappedMethod != null) {
										targetHandlerMethods.put(mappingInfo, oldMappedMethod);
										oldMappedMethod = null;
									}
									else {
										targetHandlerMethods.remove(mappingInfo);
									}
								}
							}
						}
						if (oldMappedMethod != null) {
							throw new IllegalStateException("Ambiguous handler methods mapped for HTTP path '" +
									lookupPath + "': {" + oldMappedMethod + ", " + handlerMethod +
									"}. If you intend to handle the same path in multiple methods, then factor " +
									"them out into a dedicated handler class with that path mapped at the type level!");
						}
					}
				}
			}
			if (targetHandlerMethods.size() == 1) {
				return targetHandlerMethods.values().iterator().next();
			}
			else if (!targetHandlerMethods.isEmpty()) {
				RequestMappingInfo bestMappingMatch = null;
				String bestPathMatch = null;
				for (RequestMappingInfo mapping : targetHandlerMethods.keySet()) {
					String mappedPath = targetPathMatches.get(mapping);
					if (bestMappingMatch == null) {
						bestMappingMatch = mapping;
						bestPathMatch = mappedPath;
					}
					else {
						if (isBetterPathMatch(mappedPath, bestPathMatch, lookupPath) ||
								(!isBetterPathMatch(bestPathMatch, mappedPath, lookupPath) &&
										(isBetterMethodMatch(mapping, bestMappingMatch) ||
										(!isBetterMethodMatch(bestMappingMatch, mapping) &&
												isBetterParamMatch(mapping, bestMappingMatch))))) {
							bestMappingMatch = mapping;
							bestPathMatch = mappedPath;
						}
					}
				}
				return targetHandlerMethods.get(bestMappingMatch);
			}
			else {
				throw new NoSuchRequestHandlingMethodException(lookupPath, request.getMethod(), request.getParameterMap());
			}
		}

		private boolean isPathMatch(String mappedPath, String lookupPath) {
			if (mappedPath.equals(lookupPath) || pathMatcher.match(mappedPath, lookupPath)) {
				return true;
			}
			boolean hasSuffix = (mappedPath.indexOf('.') != -1);
			if (!hasSuffix && pathMatcher.match(mappedPath + ".*", lookupPath)) {
				return true;
			}
			return (!mappedPath.startsWith("/") &&
					(lookupPath.endsWith(mappedPath) || pathMatcher.match("/**/" + mappedPath, lookupPath) ||
							(!hasSuffix && pathMatcher.match("/**/" + mappedPath + ".*", lookupPath))));
		}

		private boolean checkParameters(RequestMappingInfo mapping, HttpServletRequest request) {
			return ServletAnnotationMappingUtils.checkRequestMethod(mapping.methods, request) &&
					ServletAnnotationMappingUtils.checkParameters(mapping.params, request);
		}

		private boolean isBetterPathMatch(String mappedPath, String mappedPathToCompare, String lookupPath) {
			return (mappedPath != null &&
					(mappedPathToCompare == null || mappedPathToCompare.length() < mappedPath.length() ||
							(mappedPath.equals(lookupPath) && !mappedPathToCompare.equals(lookupPath))));
		}

		private boolean isBetterMethodMatch(RequestMappingInfo mapping, RequestMappingInfo mappingToCompare) {
			return (mappingToCompare.methods.length == 0 && mapping.methods.length > 0);
		}

		private boolean isBetterParamMatch(RequestMappingInfo mapping, RequestMappingInfo mappingToCompare) {
			return (mappingToCompare.params.length < mapping.params.length);
		}
	}


	private class ServletHandlerMethodInvoker extends HandlerMethodInvoker {

		private boolean responseArgumentUsed = false;

		public ServletHandlerMethodInvoker(HandlerMethodResolver resolver) {
			super(resolver, webBindingInitializer, sessionAttributeStore,
					parameterNameDiscoverer, customArgumentResolvers);
		}

		@Override
		protected void raiseMissingParameterException(String paramName, Class paramType) throws Exception {
			throw new MissingServletRequestParameterException(paramName, paramType.getName());
		}

		@Override
		protected void raiseSessionRequiredException(String message) throws Exception {
			throw new HttpSessionRequiredException(message);
		}

		@Override
		protected WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
				throws Exception {

			return AnnotationMethodHandlerAdapter.this.createBinder(
					(HttpServletRequest) webRequest.getNativeRequest(), target, objectName);
		}

		@Override
		protected void doBind(NativeWebRequest webRequest, WebDataBinder binder, boolean failOnErrors)
				throws Exception {

			ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
			servletBinder.bind((ServletRequest) webRequest.getNativeRequest());
			if (failOnErrors) {
				servletBinder.closeNoCatch();
			}
		}

		@Override
		protected Object resolveStandardArgument(Class parameterType, NativeWebRequest webRequest)
				throws Exception {

			HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
			HttpServletResponse response = (HttpServletResponse) webRequest.getNativeResponse();

			if (ServletRequest.class.isAssignableFrom(parameterType)) {
				return request;
			}
			else if (ServletResponse.class.isAssignableFrom(parameterType)) {
				this.responseArgumentUsed = true;
				return response;
			}
			else if (HttpSession.class.isAssignableFrom(parameterType)) {
				return request.getSession();
			}
			else if (Principal.class.isAssignableFrom(parameterType)) {
				return request.getUserPrincipal();
			}
			else if (Locale.class.equals(parameterType)) {
				return RequestContextUtils.getLocale(request);
			}
			else if (InputStream.class.isAssignableFrom(parameterType)) {
				return request.getInputStream();
			}
			else if (Reader.class.isAssignableFrom(parameterType)) {
				return request.getReader();
			}
			else if (OutputStream.class.isAssignableFrom(parameterType)) {
				this.responseArgumentUsed = true;
				return response.getOutputStream();
			}
			else if (Writer.class.isAssignableFrom(parameterType)) {
				this.responseArgumentUsed = true;
				return response.getWriter();
			}
			return super.resolveStandardArgument(parameterType, webRequest);
		}

		@SuppressWarnings("unchecked")
		public ModelAndView getModelAndView(Method handlerMethod, Class handlerType, Object returnValue,
				ExtendedModelMap implicitModel, ServletWebRequest webRequest) {

			if (returnValue instanceof ModelAndView) {
				ModelAndView mav = (ModelAndView) returnValue;
				mav.getModelMap().mergeAttributes(implicitModel);
				return mav;
			}
			else if (returnValue instanceof Model) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects(((Model) returnValue).asMap());
			}
			else if (returnValue instanceof Map) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects((Map) returnValue);
			}
			else if (returnValue instanceof View) {
				return new ModelAndView((View) returnValue).addAllObjects(implicitModel);
			}
			else if (returnValue instanceof String) {
				return new ModelAndView((String) returnValue).addAllObjects(implicitModel);
			}
			else if (returnValue == null) {
				// Either returned null or was 'void' return.
				if (this.responseArgumentUsed || webRequest.isNotModified()) {
					return null;
				}
				else {
					// Assuming view name translation...
					return new ModelAndView().addAllObjects(implicitModel);
				}
			}
			else if (!BeanUtils.isSimpleProperty(returnValue.getClass())) {
				// Assume a single model attribute...
				ModelAttribute attr = AnnotationUtils.findAnnotation(handlerMethod, ModelAttribute.class);
				String attrName = (attr != null ? attr.value() : "");
				ModelAndView mav = new ModelAndView().addAllObjects(implicitModel);
				if ("".equals(attrName)) {
					Class resolvedType = GenericTypeResolver.resolveReturnType(handlerMethod, handlerType);
					attrName = Conventions.getVariableNameForReturnType(handlerMethod, resolvedType, returnValue);
				}
				return mav.addObject(attrName, returnValue);
			}
			else {
				throw new IllegalArgumentException("Invalid handler method return value: " + returnValue);
			}
		}
	}


	private static class RequestMappingInfo {

		public String[] paths = new String[0];

		public RequestMethod[] methods = new RequestMethod[0];

		public String[] params = new String[0];

		public boolean equals(Object obj) {
			RequestMappingInfo other = (RequestMappingInfo) obj;
			return (Arrays.equals(this.paths, other.paths) && Arrays.equals(this.methods, other.methods) &&
					Arrays.equals(this.params, other.params));
		}

		public int hashCode() {
			return (Arrays.hashCode(this.paths) * 29 + Arrays.hashCode(this.methods) * 31 +
					Arrays.hashCode(this.params));
		}
	}

}
