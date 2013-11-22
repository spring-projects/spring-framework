/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.support.HandlerMethodInvoker;
import org.springframework.web.bind.annotation.support.HandlerMethodResolver;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
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
 * Implementation of the {@link org.springframework.web.servlet.HandlerAdapter} interface
 * that maps handler methods based on HTTP paths, HTTP methods and request parameters
 * expressed through the {@link RequestMapping} annotation.
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
 *
 * @deprecated in Spring 3.2 in favor of
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter RequestMappingHandlerAdapter}
 */
@Deprecated
public class AnnotationMethodHandlerAdapter extends WebContentGenerator
		implements HandlerAdapter, Ordered, BeanFactoryAware {

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

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private WebArgumentResolver[] customArgumentResolvers;

	private ModelAndViewResolver[] customModelAndViewResolvers;

	private HttpMessageConverter<?>[] messageConverters;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private ConfigurableBeanFactory beanFactory;

	private BeanExpressionContext expressionContext;

	private final Map<Class<?>, ServletHandlerMethodResolver> methodResolverCache =
			new ConcurrentHashMap<Class<?>, ServletHandlerMethodResolver>(64);

	private final Map<Class<?>, Boolean> sessionAnnotatedClassesCache = new ConcurrentHashMap<Class<?>, Boolean>(64);


	public AnnotationMethodHandlerAdapter() {
		// no restriction of HTTP methods by default
		super(false);

		// See SPR-7316
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);
		this.messageConverters = new HttpMessageConverter<?>[] {
			new ByteArrayHttpMessageConverter(), stringHttpMessageConverter,
			new SourceHttpMessageConverter<Source>(),
			new org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter() };
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
	 * or to share common UrlPathHelper settings across multiple HandlerMappings and HandlerAdapters.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths against registered URL patterns.
	 * <p>Default is {@link org.springframework.util.AntPathMatcher}.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Set the MethodNameResolver to use for resolving default handler methods
	 * (carrying an empty {@code @RequestMapping} annotation).
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
	 * storing session attributes in the HttpSession, using the same attribute name as in the model.
	 */
	public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore must not be null");
		this.sessionAttributeStore = sessionAttributeStore;
	}

	/**
	 * Cache content produced by {@code @SessionAttributes} annotated handlers
	 * for the given number of seconds. Default is 0, preventing caching completely.
	 * <p>In contrast to the "cacheSeconds" property which will apply to all general handlers
	 * (but not to {@code @SessionAttributes} annotated handlers), this setting will
	 * apply to {@code @SessionAttributes} annotated handlers only.
	 * @see #setCacheSeconds
	 * @see org.springframework.web.bind.annotation.SessionAttributes
	 */
	public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
		this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
	}

	/**
	 * Set if controller execution should be synchronized on the session,
	 * to serialize parallel invocations from the same client.
	 * <p>More specifically, the execution of the {@code handleRequestInternal}
	 * method will get synchronized if this flag is "true". The best available
	 * session mutex will be used for the synchronization; ideally, this will
	 * be a mutex exposed by HttpSessionMutexListener.
	 * <p>The session mutex is guaranteed to be the same object during
	 * the entire lifetime of the session, available under the key defined
	 * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
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
	 * Set the ParameterNameDiscoverer to use for resolving method parameter names if needed
	 * (e.g. for default attribute names).
	 * <p>Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Set a custom WebArgumentResolvers to use for special method parameter types.
	 * <p>Such a custom WebArgumentResolver will kick in first, having a chance to resolve
	 * an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolver(WebArgumentResolver argumentResolver) {
		this.customArgumentResolvers = new WebArgumentResolver[] {argumentResolver};
	}

	/**
	 * Set one or more custom WebArgumentResolvers to use for special method parameter types.
	 * <p>Any such custom WebArgumentResolver will kick in first, having a chance to resolve
	 * an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolvers(WebArgumentResolver[] argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * Set a custom ModelAndViewResolvers to use for special method return types.
	 * <p>Such a custom ModelAndViewResolver will kick in first, having a chance to resolve
	 * a return value before the standard ModelAndView handling kicks in.
	 */
	public void setCustomModelAndViewResolver(ModelAndViewResolver customModelAndViewResolver) {
		this.customModelAndViewResolvers = new ModelAndViewResolver[] {customModelAndViewResolver};
	}

	/**
	 * Set one or more custom ModelAndViewResolvers to use for special method return types.
	 * <p>Any such custom ModelAndViewResolver will kick in first, having a chance to resolve
	 * a return value before the standard ModelAndView handling kicks in.
	 */
	public void setCustomModelAndViewResolvers(ModelAndViewResolver[] customModelAndViewResolvers) {
		this.customModelAndViewResolvers = customModelAndViewResolvers;
	}

	/**
	 * Set the message body converters to use.
	 * <p>These converters are used to convert from and to HTTP requests and responses.
	 */
	public void setMessageConverters(HttpMessageConverter<?>[] messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * Return the message body converters that this adapter has been configured with.
	 */
	public HttpMessageConverter<?>[] getMessageConverters() {
		return messageConverters;
	}

	/**
	 * Specify the order value for this HandlerAdapter bean.
	 * <p>Default value is {@code Integer.MAX_VALUE}, meaning that it's non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
			this.expressionContext = new BeanExpressionContext(this.beanFactory, new RequestScope());
		}
	}


	@Override
	public boolean supports(Object handler) {
		return getMethodResolver(handler).hasHandlerMethods();
	}

	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		Class<?> clazz = ClassUtils.getUserClass(handler);
		Boolean annotatedWithSessionAttributes = this.sessionAnnotatedClassesCache.get(clazz);
		if (annotatedWithSessionAttributes == null) {
			annotatedWithSessionAttributes = (AnnotationUtils.findAnnotation(clazz, SessionAttributes.class) != null);
			this.sessionAnnotatedClassesCache.put(clazz, annotatedWithSessionAttributes);
		}

		if (annotatedWithSessionAttributes) {
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

	protected ModelAndView invokeHandlerMethod(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		ServletHandlerMethodResolver methodResolver = getMethodResolver(handler);
		Method handlerMethod = methodResolver.resolveHandlerMethod(request);
		ServletHandlerMethodInvoker methodInvoker = new ServletHandlerMethodInvoker(methodResolver);
		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		ExtendedModelMap implicitModel = new BindingAwareModelMap();

		Object result = methodInvoker.invokeHandlerMethod(handlerMethod, handler, webRequest, implicitModel);
		ModelAndView mav =
				methodInvoker.getModelAndView(handlerMethod, handler.getClass(), result, implicitModel, webRequest);
		methodInvoker.updateModelAttributes(handler, (mav != null ? mav.getModel() : null), implicitModel, webRequest);
		return mav;
	}

	/**
	 * This method always returns -1 since an annotated controller can have many methods,
	 * each requiring separate lastModified calculations. Instead, an
	 * {@link RequestMapping}-annotated method can calculate the lastModified value, call
	 * {@link org.springframework.web.context.request.WebRequest#checkNotModified(long)}
	 * to check it, and return {@code null} if that returns {@code true}.
	 * @see org.springframework.web.context.request.WebRequest#checkNotModified(long)
	 */
	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1;
	}


	/**
	 * Build a HandlerMethodResolver for the given handler type.
	 */
	private ServletHandlerMethodResolver getMethodResolver(Object handler) {
		Class<?> handlerClass = ClassUtils.getUserClass(handler);
		ServletHandlerMethodResolver resolver = this.methodResolverCache.get(handlerClass);
		if (resolver == null) {
			synchronized (this.methodResolverCache) {
				resolver = this.methodResolverCache.get(handlerClass);
				if (resolver == null) {
					resolver = new ServletHandlerMethodResolver(handlerClass);
					this.methodResolverCache.put(handlerClass, resolver);
				}
			}
		}
		return resolver;
	}


	/**
	 * Template method for creating a new ServletRequestDataBinder instance.
	 * <p>The default implementation creates a standard ServletRequestDataBinder.
	 * This can be overridden for custom ServletRequestDataBinder subclasses.
	 * @param request current HTTP request
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the objectName of the target object
	 * @return the ServletRequestDataBinder instance to use
	 * @throws Exception in case of invalid state or arguments
	 * @see ServletRequestDataBinder#bind(javax.servlet.ServletRequest)
	 * @see ServletRequestDataBinder#convertIfNecessary(Object, Class, org.springframework.core.MethodParameter)
	 */
	protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object target, String objectName) throws Exception {
		return new ServletRequestDataBinder(target, objectName);
	}

	/**
	 * Template method for creating a new HttpInputMessage instance.
	 * <p>The default implementation creates a standard {@link ServletServerHttpRequest}.
	 * This can be overridden for custom {@code HttpInputMessage} implementations
	 * @param servletRequest current HTTP request
	 * @return the HttpInputMessage instance to use
	 * @throws Exception in case of errors
	 */
	protected HttpInputMessage createHttpInputMessage(HttpServletRequest servletRequest) throws Exception {
		return new ServletServerHttpRequest(servletRequest);
	}

	/**
	 * Template method for creating a new HttpOuputMessage instance.
	 * <p>The default implementation creates a standard {@link ServletServerHttpResponse}.
	 * This can be overridden for custom {@code HttpOutputMessage} implementations
	 * @param servletResponse current HTTP response
	 * @return the HttpInputMessage instance to use
	 * @throws Exception in case of errors
	 */
	protected HttpOutputMessage createHttpOutputMessage(HttpServletResponse servletResponse) throws Exception {
		return new ServletServerHttpResponse(servletResponse);
	}


	/**
	 * Servlet-specific subclass of {@link HandlerMethodResolver}.
	 */
	private class ServletHandlerMethodResolver extends HandlerMethodResolver {

		private final Map<Method, RequestMappingInfo> mappings = new HashMap<Method, RequestMappingInfo>();

		private ServletHandlerMethodResolver(Class<?> handlerType) {
			init(handlerType);
		}

		@Override
		protected boolean isHandlerMethod(Method method) {
			if (this.mappings.containsKey(method)) {
				return true;
			}
			RequestMapping mapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if (mapping != null) {
				String[] patterns = mapping.value();
				RequestMethod[] methods = new RequestMethod[0];
				String[] params = new String[0];
				String[] headers = new String[0];
				if (!hasTypeLevelMapping() || !Arrays.equals(mapping.method(), getTypeLevelMapping().method())) {
					methods = mapping.method();
				}
				if (!hasTypeLevelMapping() || !Arrays.equals(mapping.params(), getTypeLevelMapping().params())) {
					params = mapping.params();
				}
				if (!hasTypeLevelMapping() || !Arrays.equals(mapping.headers(), getTypeLevelMapping().headers())) {
					headers = mapping.headers();
				}
				RequestMappingInfo mappingInfo = new RequestMappingInfo(patterns, methods, params, headers);
				this.mappings.put(method, mappingInfo);
				return true;
			}
			return false;
		}

		public Method resolveHandlerMethod(HttpServletRequest request) throws ServletException {
			String lookupPath = urlPathHelper.getLookupPathForRequest(request);
			Comparator<String> pathComparator = pathMatcher.getPatternComparator(lookupPath);
			Map<RequestSpecificMappingInfo, Method> targetHandlerMethods = new LinkedHashMap<RequestSpecificMappingInfo, Method>();
			Set<String> allowedMethods = new LinkedHashSet<String>(7);
			String resolvedMethodName = null;
			for (Method handlerMethod : getHandlerMethods()) {
				RequestSpecificMappingInfo mappingInfo = new RequestSpecificMappingInfo(this.mappings.get(handlerMethod));
				boolean match = false;
				if (mappingInfo.hasPatterns()) {
					for (String pattern : mappingInfo.getPatterns()) {
						if (!hasTypeLevelMapping() && !pattern.startsWith("/")) {
							pattern = "/" + pattern;
						}
						String combinedPattern = getCombinedPattern(pattern, lookupPath, request);
						if (combinedPattern != null) {
							if (mappingInfo.matches(request)) {
								match = true;
								mappingInfo.addMatchedPattern(combinedPattern);
							}
							else {
								if (!mappingInfo.matchesRequestMethod(request)) {
									allowedMethods.addAll(mappingInfo.methodNames());
								}
								break;
							}
						}
					}
					mappingInfo.sortMatchedPatterns(pathComparator);
				}
				else if (useTypeLevelMapping(request)) {
					String[] typeLevelPatterns = getTypeLevelMapping().value();
					for (String typeLevelPattern : typeLevelPatterns) {
						if (!typeLevelPattern.startsWith("/")) {
							typeLevelPattern = "/" + typeLevelPattern;
						}
						boolean useSuffixPattern = useSuffixPattern(request);
						if (getMatchingPattern(typeLevelPattern, lookupPath, useSuffixPattern) != null) {
							if (mappingInfo.matches(request)) {
								match = true;
								mappingInfo.addMatchedPattern(typeLevelPattern);
							}
							else {
								if (!mappingInfo.matchesRequestMethod(request)) {
									allowedMethods.addAll(mappingInfo.methodNames());
								}
								break;
							}
						}
					}
					mappingInfo.sortMatchedPatterns(pathComparator);
				}
				else {
					// No paths specified: parameter match sufficient.
					match = mappingInfo.matches(request);
					if (match && mappingInfo.getMethodCount() == 0 && mappingInfo.getParamCount() == 0 &&
							resolvedMethodName != null && !resolvedMethodName.equals(handlerMethod.getName())) {
						match = false;
					}
					else {
						if (!mappingInfo.matchesRequestMethod(request)) {
							allowedMethods.addAll(mappingInfo.methodNames());
						}
					}
				}
				if (match) {
					Method oldMappedMethod = targetHandlerMethods.put(mappingInfo, handlerMethod);
					if (oldMappedMethod != null && oldMappedMethod != handlerMethod) {
						if (methodNameResolver != null && !mappingInfo.hasPatterns()) {
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
							throw new IllegalStateException(
									"Ambiguous handler methods mapped for HTTP path '" + lookupPath + "': {" +
									oldMappedMethod + ", " + handlerMethod +
									"}. If you intend to handle the same path in multiple methods, then factor " +
									"them out into a dedicated handler class with that path mapped at the type level!");
						}
					}
				}
			}
			if (!targetHandlerMethods.isEmpty()) {
				List<RequestSpecificMappingInfo> matches = new ArrayList<RequestSpecificMappingInfo>(targetHandlerMethods.keySet());
				RequestSpecificMappingInfoComparator requestMappingInfoComparator =
						new RequestSpecificMappingInfoComparator(pathComparator, request);
				Collections.sort(matches, requestMappingInfoComparator);
				RequestSpecificMappingInfo bestMappingMatch = matches.get(0);
				String bestMatchedPath = bestMappingMatch.bestMatchedPattern();
				if (bestMatchedPath != null) {
					extractHandlerMethodUriTemplates(bestMatchedPath, lookupPath, request);
				}
				return targetHandlerMethods.get(bestMappingMatch);
			}
			else {
				if (!allowedMethods.isEmpty()) {
					throw new HttpRequestMethodNotSupportedException(request.getMethod(), StringUtils.toStringArray(allowedMethods));
				}
				throw new NoSuchRequestHandlingMethodException(lookupPath, request.getMethod(), request.getParameterMap());
			}
		}

		private boolean useTypeLevelMapping(HttpServletRequest request) {
			if (!hasTypeLevelMapping() || ObjectUtils.isEmpty(getTypeLevelMapping().value())) {
				return false;
			}
			Object value = request.getAttribute(HandlerMapping.INTROSPECT_TYPE_LEVEL_MAPPING);
			return (value != null) ? (Boolean) value : Boolean.TRUE;
		}

		private boolean useSuffixPattern(HttpServletRequest request) {
			Object value = request.getAttribute(DefaultAnnotationHandlerMapping.USE_DEFAULT_SUFFIX_PATTERN);
			return (value != null) ? (Boolean) value : Boolean.TRUE;
		}

		/**
		 * Determines the combined pattern for the given methodLevelPattern and path.
		 * <p>Uses the following algorithm:
		 * <ol>
		 * <li>If there is a type-level mapping with path information, it is {@linkplain
		 * PathMatcher#combine(String, String) combined} with the method-level pattern.</li>
		 * <li>If there is a {@linkplain HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE best matching pattern}
		 * in the request, it is combined with the method-level pattern.</li>
		 * <li>Otherwise, the method-level pattern is returned.</li>
		 * </ol>
		 */
		private String getCombinedPattern(String methodLevelPattern, String lookupPath, HttpServletRequest request) {
			boolean useSuffixPattern = useSuffixPattern(request);
			if (useTypeLevelMapping(request)) {
				String[] typeLevelPatterns = getTypeLevelMapping().value();
				for (String typeLevelPattern : typeLevelPatterns) {
					if (!typeLevelPattern.startsWith("/")) {
						typeLevelPattern = "/" + typeLevelPattern;
					}
					String combinedPattern = pathMatcher.combine(typeLevelPattern, methodLevelPattern);
					String matchingPattern = getMatchingPattern(combinedPattern, lookupPath, useSuffixPattern);
					if (matchingPattern != null) {
						return matchingPattern;
					}
				}
				return null;
			}
			String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
			if (StringUtils.hasText(bestMatchingPattern) && bestMatchingPattern.endsWith("*")) {
				String combinedPattern = pathMatcher.combine(bestMatchingPattern, methodLevelPattern);
				String matchingPattern = getMatchingPattern(combinedPattern, lookupPath, useSuffixPattern);
				if (matchingPattern != null && !matchingPattern.equals(bestMatchingPattern)) {
					return matchingPattern;
				}
			}
			return getMatchingPattern(methodLevelPattern, lookupPath, useSuffixPattern);
		}

		private String getMatchingPattern(String pattern, String lookupPath, boolean useSuffixPattern) {
			if (pattern.equals(lookupPath)) {
				return pattern;
			}
			boolean hasSuffix = pattern.indexOf('.') != -1;
			if (useSuffixPattern && !hasSuffix) {
				String patternWithSuffix = pattern + ".*";
				if (pathMatcher.match(patternWithSuffix, lookupPath)) {
					return patternWithSuffix;
				}
			}
			if (pathMatcher.match(pattern, lookupPath)) {
				return pattern;
			}
			boolean endsWithSlash = pattern.endsWith("/");
			if (useSuffixPattern && !endsWithSlash) {
				String patternWithSlash = pattern + "/";
				if (pathMatcher.match(patternWithSlash, lookupPath)) {
					return patternWithSlash;
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private void extractHandlerMethodUriTemplates(String mappedPattern, String lookupPath, HttpServletRequest request) {
			Map<String, String> variables =
					(Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			int patternVariableCount = StringUtils.countOccurrencesOf(mappedPattern, "{");
			if ((variables == null || patternVariableCount != variables.size()) && pathMatcher.match(mappedPattern, lookupPath)) {
				variables = pathMatcher.extractUriTemplateVariables(mappedPattern, lookupPath);
				Map<String, String> decodedVariables = urlPathHelper.decodePathVariables(request, variables);
				request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodedVariables);
			}
		}
	}


	/**
	 * Servlet-specific subclass of {@link HandlerMethodInvoker}.
	 */
	private class ServletHandlerMethodInvoker extends HandlerMethodInvoker {

		private boolean responseArgumentUsed = false;

		private ServletHandlerMethodInvoker(HandlerMethodResolver resolver) {
			super(resolver, webBindingInitializer, sessionAttributeStore, parameterNameDiscoverer,
					customArgumentResolvers, messageConverters);
		}

		@Override
		protected void raiseMissingParameterException(String paramName, Class<?> paramType) throws Exception {
			throw new MissingServletRequestParameterException(paramName, paramType.getSimpleName());
		}

		@Override
		protected void raiseSessionRequiredException(String message) throws Exception {
			throw new HttpSessionRequiredException(message);
		}

		@Override
		protected WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
				throws Exception {

			return AnnotationMethodHandlerAdapter.this.createBinder(
					webRequest.getNativeRequest(HttpServletRequest.class), target, objectName);
		}

		@Override
		protected void doBind(WebDataBinder binder, NativeWebRequest webRequest) throws Exception {
			ServletRequestDataBinder servletBinder = (ServletRequestDataBinder) binder;
			servletBinder.bind(webRequest.getNativeRequest(ServletRequest.class));
		}

		@Override
		protected HttpInputMessage createHttpInputMessage(NativeWebRequest webRequest) throws Exception {
			HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
			return AnnotationMethodHandlerAdapter.this.createHttpInputMessage(servletRequest);
		}

		@Override
		protected HttpOutputMessage createHttpOutputMessage(NativeWebRequest webRequest) throws Exception {
			HttpServletResponse servletResponse = (HttpServletResponse) webRequest.getNativeResponse();
			return AnnotationMethodHandlerAdapter.this.createHttpOutputMessage(servletResponse);
		}

		@Override
		protected Object resolveDefaultValue(String value) {
			if (beanFactory == null) {
				return value;
			}
			String placeholdersResolved = beanFactory.resolveEmbeddedValue(value);
			BeanExpressionResolver exprResolver = beanFactory.getBeanExpressionResolver();
			if (exprResolver == null) {
				return value;
			}
			return exprResolver.evaluate(placeholdersResolved, expressionContext);
		}

		@Override
		protected Object resolveCookieValue(String cookieName, Class<?> paramType, NativeWebRequest webRequest)
				throws Exception {

			HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
			Cookie cookieValue = WebUtils.getCookie(servletRequest, cookieName);
			if (Cookie.class.isAssignableFrom(paramType)) {
				return cookieValue;
			}
			else if (cookieValue != null) {
				return urlPathHelper.decodeRequestString(servletRequest, cookieValue.getValue());
			}
			else {
				return null;
			}
		}

		@Override
		@SuppressWarnings({"unchecked"})
		protected String resolvePathVariable(String pathVarName, Class<?> paramType, NativeWebRequest webRequest)
				throws Exception {

			HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
			Map<String, String> uriTemplateVariables =
					(Map<String, String>) servletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			if (uriTemplateVariables == null || !uriTemplateVariables.containsKey(pathVarName)) {
				throw new IllegalStateException(
						"Could not find @PathVariable [" + pathVarName + "] in @RequestMapping");
			}
			return uriTemplateVariables.get(pathVarName);
		}

		@Override
		protected Object resolveStandardArgument(Class<?> parameterType, NativeWebRequest webRequest) throws Exception {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);

			if (ServletRequest.class.isAssignableFrom(parameterType) ||
					MultipartRequest.class.isAssignableFrom(parameterType)) {
				Object nativeRequest = webRequest.getNativeRequest(parameterType);
				if (nativeRequest == null) {
					throw new IllegalStateException(
							"Current request is not of type [" + parameterType.getName() + "]: " + request);
				}
				return nativeRequest;
			}
			else if (ServletResponse.class.isAssignableFrom(parameterType)) {
				this.responseArgumentUsed = true;
				Object nativeResponse = webRequest.getNativeResponse(parameterType);
				if (nativeResponse == null) {
					throw new IllegalStateException(
							"Current response is not of type [" + parameterType.getName() + "]: " + response);
				}
				return nativeResponse;
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
		public ModelAndView getModelAndView(Method handlerMethod, Class<?> handlerType, Object returnValue,
				ExtendedModelMap implicitModel, ServletWebRequest webRequest) throws Exception {

			ResponseStatus responseStatusAnn = AnnotationUtils.findAnnotation(handlerMethod, ResponseStatus.class);
			if (responseStatusAnn != null) {
				HttpStatus responseStatus = responseStatusAnn.value();
				String reason = responseStatusAnn.reason();
				if (!StringUtils.hasText(reason)) {
					webRequest.getResponse().setStatus(responseStatus.value());
				}
				else {
					webRequest.getResponse().sendError(responseStatus.value(), reason);
				}

				// to be picked up by the RedirectView
				webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, responseStatus);

				responseArgumentUsed = true;
			}

			// Invoke custom resolvers if present...
			if (customModelAndViewResolvers != null) {
				for (ModelAndViewResolver mavResolver : customModelAndViewResolvers) {
					ModelAndView mav = mavResolver.resolveModelAndView(
							handlerMethod, handlerType, returnValue, implicitModel, webRequest);
					if (mav != ModelAndViewResolver.UNRESOLVED) {
						return mav;
					}
				}
			}

			if (returnValue instanceof HttpEntity) {
				handleHttpEntityResponse((HttpEntity<?>) returnValue, webRequest);
				return null;
			}
			else if (AnnotationUtils.findAnnotation(handlerMethod, ResponseBody.class) != null) {
				handleResponseBody(returnValue, webRequest);
				return null;
			}
			else if (returnValue instanceof ModelAndView) {
				ModelAndView mav = (ModelAndView) returnValue;
				mav.getModelMap().mergeAttributes(implicitModel);
				return mav;
			}
			else if (returnValue instanceof Model) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects(((Model) returnValue).asMap());
			}
			else if (returnValue instanceof View) {
				return new ModelAndView((View) returnValue).addAllObjects(implicitModel);
			}
			else if (AnnotationUtils.findAnnotation(handlerMethod, ModelAttribute.class) != null) {
				addReturnValueAsModelAttribute(handlerMethod, handlerType, returnValue, implicitModel);
				return new ModelAndView().addAllObjects(implicitModel);
			}
			else if (returnValue instanceof Map) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects((Map<String, ?>) returnValue);
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
				addReturnValueAsModelAttribute(handlerMethod, handlerType, returnValue, implicitModel);
				return new ModelAndView().addAllObjects(implicitModel);
			}
			else {
				throw new IllegalArgumentException("Invalid handler method return value: " + returnValue);
			}
		}

		private void handleResponseBody(Object returnValue, ServletWebRequest webRequest)
				throws Exception {
			if (returnValue == null) {
				return;
			}
			HttpInputMessage inputMessage = createHttpInputMessage(webRequest);
			HttpOutputMessage outputMessage = createHttpOutputMessage(webRequest);
			writeWithMessageConverters(returnValue, inputMessage, outputMessage);
		}

		private void handleHttpEntityResponse(HttpEntity<?> responseEntity, ServletWebRequest webRequest)
				throws Exception {
			if (responseEntity == null) {
				return;
			}
			HttpInputMessage inputMessage = createHttpInputMessage(webRequest);
			HttpOutputMessage outputMessage = createHttpOutputMessage(webRequest);
			if (responseEntity instanceof ResponseEntity && outputMessage instanceof ServerHttpResponse) {
				((ServerHttpResponse) outputMessage).setStatusCode(((ResponseEntity<?>) responseEntity).getStatusCode());
			}
			HttpHeaders entityHeaders = responseEntity.getHeaders();
			if (!entityHeaders.isEmpty()) {
				outputMessage.getHeaders().putAll(entityHeaders);
			}
			Object body = responseEntity.getBody();
			if (body != null) {
				writeWithMessageConverters(body, inputMessage, outputMessage);
			}
			else {
				// flush headers
				outputMessage.getBody();
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private void writeWithMessageConverters(Object returnValue,
				HttpInputMessage inputMessage, HttpOutputMessage outputMessage)
				throws IOException, HttpMediaTypeNotAcceptableException {
			List<MediaType> acceptedMediaTypes = inputMessage.getHeaders().getAccept();
			if (acceptedMediaTypes.isEmpty()) {
				acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
			}
			MediaType.sortByQualityValue(acceptedMediaTypes);
			Class<?> returnValueType = returnValue.getClass();
			List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
			if (getMessageConverters() != null) {
				for (MediaType acceptedMediaType : acceptedMediaTypes) {
					for (HttpMessageConverter messageConverter : getMessageConverters()) {
						if (messageConverter.canWrite(returnValueType, acceptedMediaType)) {
							messageConverter.write(returnValue, acceptedMediaType, outputMessage);
							if (logger.isDebugEnabled()) {
								MediaType contentType = outputMessage.getHeaders().getContentType();
								if (contentType == null) {
									contentType = acceptedMediaType;
								}
								logger.debug("Written [" + returnValue + "] as \"" + contentType +
										"\" using [" + messageConverter + "]");
							}
							this.responseArgumentUsed = true;
							return;
						}
					}
				}
				for (HttpMessageConverter messageConverter : messageConverters) {
					allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
				}
			}
			throw new HttpMediaTypeNotAcceptableException(allSupportedMediaTypes);
		}

	}


	/**
	 * Holder for request mapping metadata.
	 */
	static class RequestMappingInfo {

		private final String[] patterns;

		private final RequestMethod[] methods;

		private final String[] params;

		private final String[] headers;

		RequestMappingInfo(String[] patterns, RequestMethod[] methods, String[] params, String[] headers) {
			this.patterns = patterns != null ? patterns : new String[0];
			this.methods = methods != null ? methods : new RequestMethod[0];
			this.params = params != null ? params : new String[0];
			this.headers = headers != null ? headers : new String[0];
		}

		public boolean hasPatterns() {
			return patterns.length > 0;
		}

		public String[] getPatterns() {
			return patterns;
		}

		public int getMethodCount() {
			return methods.length;
		}

		public int getParamCount() {
			return params.length;
		}

		public int getHeaderCount() {
			return headers.length;
		}

		public boolean matches(HttpServletRequest request) {
			return matchesRequestMethod(request) && matchesParameters(request) && matchesHeaders(request);
		}

		public boolean matchesHeaders(HttpServletRequest request) {
			return ServletAnnotationMappingUtils.checkHeaders(this.headers, request);
		}

		public boolean matchesParameters(HttpServletRequest request) {
			return ServletAnnotationMappingUtils.checkParameters(this.params, request);
		}

		public boolean matchesRequestMethod(HttpServletRequest request) {
			return ServletAnnotationMappingUtils.checkRequestMethod(this.methods, request);
		}

		public Set<String> methodNames() {
			Set<String> methodNames = new LinkedHashSet<String>(methods.length);
			for (RequestMethod method : methods) {
				methodNames.add(method.name());
			}
			return methodNames;
		}

		@Override
		public boolean equals(Object obj) {
			RequestMappingInfo other = (RequestMappingInfo) obj;
			return (Arrays.equals(this.patterns, other.patterns) && Arrays.equals(this.methods, other.methods) &&
					Arrays.equals(this.params, other.params) && Arrays.equals(this.headers, other.headers));
		}

		@Override
		public int hashCode() {
			return (Arrays.hashCode(this.patterns) * 23 + Arrays.hashCode(this.methods) * 29 +
					Arrays.hashCode(this.params) * 31 + Arrays.hashCode(this.headers));
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(Arrays.asList(patterns));
			if (methods.length > 0) {
				builder.append(',');
				builder.append(Arrays.asList(methods));
			}
			if (headers.length > 0) {
				builder.append(',');
				builder.append(Arrays.asList(headers));
			}
			if (params.length > 0) {
				builder.append(',');
				builder.append(Arrays.asList(params));
			}
			return builder.toString();
		}
	}


	/**
	 * Subclass of {@link RequestMappingInfo} that holds request-specific data.
	 */
	static class RequestSpecificMappingInfo extends RequestMappingInfo {

		private final List<String> matchedPatterns = new ArrayList<String>();

		RequestSpecificMappingInfo(String[] patterns, RequestMethod[] methods, String[] params, String[] headers) {
			super(patterns, methods, params, headers);
		}

		RequestSpecificMappingInfo(RequestMappingInfo other) {
			super(other.patterns, other.methods, other.params, other.headers);
		}

		public void addMatchedPattern(String matchedPattern) {
			matchedPatterns.add(matchedPattern);
		}

		public void sortMatchedPatterns(Comparator<String> pathComparator) {
			Collections.sort(matchedPatterns, pathComparator);
		}

		public String bestMatchedPattern() {
			return (!this.matchedPatterns.isEmpty() ? this.matchedPatterns.get(0) : null);
		}
	}


	/**
	 * Comparator capable of sorting {@link RequestSpecificMappingInfo}s (RHIs) so that
	 * sorting a list with this comparator will result in:
	 * <ul>
	 * <li>RHIs with {@linkplain AnnotationMethodHandlerAdapter.RequestSpecificMappingInfo#matchedPatterns better matched paths}
	 * take prescedence over those with a weaker match (as expressed by the {@linkplain PathMatcher#getPatternComparator(String)
	 * path pattern comparator}.) Typically, this means that patterns without wild cards and uri templates
	 * will be ordered before those without.</li>
	 * <li>RHIs with one single {@linkplain RequestMappingInfo#methods request method} will be
	 * ordered before those without a method, or with more than one method.</li>
	 * <li>RHIs with more {@linkplain RequestMappingInfo#params request parameters} will be ordered
	 * before those with less parameters</li>
	 * </ol>
	 */
	static class RequestSpecificMappingInfoComparator implements Comparator<RequestSpecificMappingInfo> {

		private final Comparator<String> pathComparator;

		private final ServerHttpRequest request;

		RequestSpecificMappingInfoComparator(Comparator<String> pathComparator, HttpServletRequest request) {
			this.pathComparator = pathComparator;
			this.request = new ServletServerHttpRequest(request);
		}

		@Override
		public int compare(RequestSpecificMappingInfo info1, RequestSpecificMappingInfo info2) {
			int pathComparison = pathComparator.compare(info1.bestMatchedPattern(), info2.bestMatchedPattern());
			if (pathComparison != 0) {
				return pathComparison;
			}
			int info1ParamCount = info1.getParamCount();
			int info2ParamCount = info2.getParamCount();
			if (info1ParamCount != info2ParamCount) {
				return info2ParamCount - info1ParamCount;
			}
			int info1HeaderCount = info1.getHeaderCount();
			int info2HeaderCount = info2.getHeaderCount();
			if (info1HeaderCount != info2HeaderCount) {
				return info2HeaderCount - info1HeaderCount;
			}
			int acceptComparison = compareAcceptHeaders(info1, info2);
			if (acceptComparison != 0) {
				return acceptComparison;
			}
			int info1MethodCount = info1.getMethodCount();
			int info2MethodCount = info2.getMethodCount();
			if (info1MethodCount == 0 && info2MethodCount > 0) {
				return 1;
			}
			else if (info2MethodCount == 0 && info1MethodCount > 0) {
				return -1;
			}
			else if (info1MethodCount == 1 & info2MethodCount > 1) {
				return -1;
			}
			else if (info2MethodCount == 1 & info1MethodCount > 1) {
				return 1;
			}
			return 0;
		}

		private int compareAcceptHeaders(RequestMappingInfo info1, RequestMappingInfo info2) {
			List<MediaType> requestAccepts = request.getHeaders().getAccept();
			MediaType.sortByQualityValue(requestAccepts);

			List<MediaType> info1Accepts = getAcceptHeaderValue(info1);
			List<MediaType> info2Accepts = getAcceptHeaderValue(info2);

			for (MediaType requestAccept : requestAccepts) {
				int pos1 = indexOfIncluded(info1Accepts, requestAccept);
				int pos2 = indexOfIncluded(info2Accepts, requestAccept);
				if (pos1 != pos2) {
					return pos2 - pos1;
				}
			}
			return 0;
		}

		private int indexOfIncluded(List<MediaType> infoAccepts, MediaType requestAccept) {
			for (int i = 0; i < infoAccepts.size(); i++) {
				MediaType info1Accept = infoAccepts.get(i);
				if (requestAccept.includes(info1Accept)) {
					return i;
				}
			}
			return -1;
		}

		private List<MediaType> getAcceptHeaderValue(RequestMappingInfo info) {
			for (String header : info.headers) {
				int separator = header.indexOf('=');
				if (separator != -1) {
					String key = header.substring(0, separator);
					String value = header.substring(separator + 1);
					if ("Accept".equalsIgnoreCase(key)) {
						return MediaType.parseMediaTypes(value);
					}
				}
			}
			return Collections.emptyList();
		}
	}

}
