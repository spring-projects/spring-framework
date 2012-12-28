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

package org.springframework.web.portlet.mvc.annotation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.ClientDataRequest;
import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.MimeResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.StateAwareResponse;
import javax.portlet.WindowState;
import javax.servlet.http.Cookie;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.support.BindingAwareModelMap;
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
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.NoHandlerFoundException;
import org.springframework.web.portlet.bind.MissingPortletRequestParameterException;
import org.springframework.web.portlet.bind.PortletRequestDataBinder;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;
import org.springframework.web.portlet.context.PortletWebRequest;
import org.springframework.web.portlet.handler.PortletContentGenerator;
import org.springframework.web.portlet.handler.PortletSessionRequiredException;
import org.springframework.web.portlet.util.PortletUtils;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

/**
 * Implementation of the {@link org.springframework.web.portlet.HandlerAdapter}
 * interface that maps handler methods based on portlet modes, action/render phases
 * and request parameters expressed through the {@link RequestMapping} annotation.
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
 * @see #setWebBindingInitializer
 * @see #setSessionAttributeStore
 */
public class AnnotationMethodHandlerAdapter extends PortletContentGenerator
		implements HandlerAdapter, Ordered, BeanFactoryAware {

	public static final String IMPLICIT_MODEL_SESSION_ATTRIBUTE =
			AnnotationMethodHandlerAdapter.class.getName() + ".IMPLICIT_MODEL";

	public static final String IMPLICIT_MODEL_RENDER_PARAMETER = "implicitModel";


	private WebBindingInitializer webBindingInitializer;

	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

	private int cacheSecondsForSessionAttributeHandlers = 0;

	private boolean synchronizeOnSession = false;

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private WebArgumentResolver[] customArgumentResolvers;

	private ModelAndViewResolver[] customModelAndViewResolvers;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private ConfigurableBeanFactory beanFactory;

	private BeanExpressionContext expressionContext;

	private final Map<Class<?>, PortletHandlerMethodResolver> methodResolverCache =
			new ConcurrentHashMap<Class<?>, PortletHandlerMethodResolver>(64);


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
	 * storing session attributes in the PortletSession, using the same
	 * attribute name as in the model.
	 */
	public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore must not be null");
		this.sessionAttributeStore = sessionAttributeStore;
	}

	/**
	 * Cache content produced by {@code @SessionAttributes} annotated handlers
	 * for the given number of seconds. Default is 0, preventing caching completely.
	 * <p>In contrast to the "cacheSeconds" property which will apply to all general
	 * handlers (but not to {@code @SessionAttributes} annotated handlers), this
	 * setting will apply to {@code @SessionAttributes} annotated handlers only.
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
	 * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
	 * safe reference to synchronize on for locking on the current session.
	 * <p>In many cases, the PortletSession reference itself is a safe mutex
	 * as well, since it will always be the same object reference for the
	 * same active logical session. However, this is not guaranteed across
	 * different servlet containers; the only 100% safe way is a session mutex.
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 * @see org.springframework.web.portlet.util.PortletUtils#getSessionMutex(javax.portlet.PortletSession)
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
	 * Set a custom WebArgumentResolver to use for special method parameter types.
	 * Such a custom WebArgumentResolver will kick in first, having a chance to
	 * resolve an argument value before the standard argument handling kicks in.
	 */
	public void setCustomArgumentResolver(WebArgumentResolver argumentResolver) {
		this.customArgumentResolvers = new WebArgumentResolver[] {argumentResolver};
	}

	/**
	 * Set one or more custom WebArgumentResolvers to use for special method
	 * parameter types. Any such custom WebArgumentResolver will kick in first,
	 * having a chance to resolve an argument value before the standard
	 * argument handling kicks in.
	 */
	public void setCustomArgumentResolvers(WebArgumentResolver[] argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}

	/**
	 * Set a custom ModelAndViewResolvers to use for special method return types.
	 * Such a custom ModelAndViewResolver will kick in first, having a chance to
	 * resolve an return value before the standard ModelAndView handling kicks in.
	 */
	public void setCustomModelAndViewResolver(ModelAndViewResolver customModelAndViewResolver) {
		this.customModelAndViewResolvers = new ModelAndViewResolver[]{customModelAndViewResolver};
	}

	/**
	 * Set one or more custom ModelAndViewResolvers to use for special method return types.
	 * Any such custom ModelAndViewResolver will kick in first, having a chance to
	 * resolve an return value before the standard ModelAndView handling kicks in.
	 */
	public void setCustomModelAndViewResolvers(ModelAndViewResolver[] customModelAndViewResolvers) {
		this.customModelAndViewResolvers = customModelAndViewResolvers;
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
	public void handleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception {
		Object returnValue = doHandle(request, response, handler);
		if (returnValue != null) {
			throw new IllegalStateException("Invalid action method return value: " + returnValue);
		}
	}

	@Override
	public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception {
		checkAndPrepare(request, response);
		return doHandle(request, response, handler);
	}

	@Override
	public ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler) throws Exception {
		checkAndPrepare(request, response);
		return doHandle(request, response, handler);
	}

	@Override
	public void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
		Object returnValue = doHandle(request, response, handler);
		if (returnValue != null) {
			throw new IllegalStateException("Invalid event method return value: " + returnValue);
		}
	}


	protected ModelAndView doHandle(PortletRequest request, PortletResponse response, Object handler) throws Exception {
		ExtendedModelMap implicitModel = null;

		if (response instanceof MimeResponse) {
			MimeResponse mimeResponse = (MimeResponse) response;
			// Detect implicit model from associated action phase.
			if (response instanceof RenderResponse) {
				PortletSession session = request.getPortletSession(false);
				if (session != null) {
					if (request.getParameter(IMPLICIT_MODEL_RENDER_PARAMETER) != null) {
						implicitModel = (ExtendedModelMap) session.getAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE);
					}
					else {
						session.removeAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE);
					}
				}
			}
			if (handler.getClass().getAnnotation(SessionAttributes.class) != null) {
				// Always prevent caching in case of session attribute management.
				checkAndPrepare(request, mimeResponse, this.cacheSecondsForSessionAttributeHandlers);
			}
			else {
				// Uses configured default cacheSeconds setting.
				checkAndPrepare(request, mimeResponse);
			}
		}

		if (implicitModel == null) {
			implicitModel = new BindingAwareModelMap();
		}

		// Execute invokeHandlerMethod in synchronized block if required.
		if (this.synchronizeOnSession) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					return invokeHandlerMethod(request, response, handler, implicitModel);
				}
			}
		}

		return invokeHandlerMethod(request, response, handler, implicitModel);
	}

	@SuppressWarnings("unchecked")
	private ModelAndView invokeHandlerMethod(
			PortletRequest request, PortletResponse response, Object handler, ExtendedModelMap implicitModel)
			throws Exception {

		PortletWebRequest webRequest = new PortletWebRequest(request, response);
		PortletHandlerMethodResolver methodResolver = getMethodResolver(handler);
		Method handlerMethod = methodResolver.resolveHandlerMethod(request);
		PortletHandlerMethodInvoker methodInvoker = new PortletHandlerMethodInvoker(methodResolver);

		Object result = methodInvoker.invokeHandlerMethod(handlerMethod, handler, webRequest, implicitModel);
		ModelAndView mav = methodInvoker.getModelAndView(handlerMethod, handler.getClass(), result, implicitModel,
				webRequest);
		methodInvoker.updateModelAttributes(
				handler, (mav != null ? mav.getModel() : null), implicitModel, webRequest);

		// Expose implicit model for subsequent render phase.
		if (response instanceof StateAwareResponse && !implicitModel.isEmpty()) {
			StateAwareResponse stateResponse = (StateAwareResponse) response;
			Map<?, ?> modelToStore = implicitModel;
			try {
				stateResponse.setRenderParameter(IMPLICIT_MODEL_RENDER_PARAMETER, Boolean.TRUE.toString());
				if (response instanceof EventResponse) {
					// Update the existing model, if any, when responding to an event -
					// whereas we're replacing the model in case of an action response.
					Map existingModel = (Map) request.getPortletSession().getAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE);
					if (existingModel != null) {
						existingModel.putAll(implicitModel);
						modelToStore = existingModel;
					}
				}
				request.getPortletSession().setAttribute(IMPLICIT_MODEL_SESSION_ATTRIBUTE, modelToStore);
			}
			catch (IllegalStateException ex) {
				// Probably sendRedirect called... no need to expose model to render phase.
			}
		}

		return mav;
	}

	/**
	 * Build a HandlerMethodResolver for the given handler type.
	 */
	private PortletHandlerMethodResolver getMethodResolver(Object handler) {
		Class handlerClass = ClassUtils.getUserClass(handler);
		PortletHandlerMethodResolver resolver = this.methodResolverCache.get(handlerClass);
		if (resolver == null) {
			synchronized (this.methodResolverCache) {
				resolver = this.methodResolverCache.get(handlerClass);
				if (resolver == null) {
					resolver = new PortletHandlerMethodResolver(handlerClass);
					this.methodResolverCache.put(handlerClass, resolver);
				}
			}
		}
		return resolver;
	}

	/**
	 * Template method for creating a new PortletRequestDataBinder instance.
	 * <p>The default implementation creates a standard PortletRequestDataBinder.
	 * This can be overridden for custom PortletRequestDataBinder subclasses.
	 * @param request current portlet request
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the objectName of the target object
	 * @return the PortletRequestDataBinder instance to use
	 * @throws Exception in case of invalid state or arguments
	 * @see PortletRequestDataBinder#bind(javax.portlet.PortletRequest)
	 */
	protected PortletRequestDataBinder createBinder(PortletRequest request, Object target, String objectName) throws Exception {
		return new PortletRequestDataBinder(target, objectName);
	}


	/**
	 * Portlet-specific subclass of {@link HandlerMethodResolver}.
	 */
	private static class PortletHandlerMethodResolver extends HandlerMethodResolver {

		private final Map<Method, RequestMappingInfo> mappings = new HashMap<Method, RequestMappingInfo>();

		public PortletHandlerMethodResolver(Class<?> handlerType) {
			init(handlerType);
		}

		@Override
		protected boolean isHandlerMethod(Method method) {
			if (this.mappings.containsKey(method)) {
				return true;
			}
			RequestMappingInfo mappingInfo = new RequestMappingInfo();
			ActionMapping actionMapping = AnnotationUtils.findAnnotation(method, ActionMapping.class);
			RenderMapping renderMapping = AnnotationUtils.findAnnotation(method, RenderMapping.class);
			ResourceMapping resourceMapping = AnnotationUtils.findAnnotation(method, ResourceMapping.class);
			EventMapping eventMapping = AnnotationUtils.findAnnotation(method, EventMapping.class);
			RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if (actionMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.ACTION_PHASE, actionMapping.value(), actionMapping.params());
			}
			if (renderMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.RENDER_PHASE, renderMapping.value(), renderMapping.params());
			}
			if (resourceMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.RESOURCE_PHASE, resourceMapping.value(), new String[0]);
			}
			if (eventMapping != null) {
				mappingInfo.initPhaseMapping(PortletRequest.EVENT_PHASE, eventMapping.value(), new String[0]);
			}
			if (requestMapping != null) {
				mappingInfo.initStandardMapping(requestMapping.value(), requestMapping.method(),
						requestMapping.params(), requestMapping.headers());
				if (mappingInfo.phase == null) {
					mappingInfo.phase = determineDefaultPhase(method);
				}
			}
			if (mappingInfo.phase != null) {
				this.mappings.put(method, mappingInfo);
				return true;
			}
			return false;
		}

		public Method resolveHandlerMethod(PortletRequest request) throws PortletException {
			Map<RequestMappingInfo, Method> targetHandlerMethods = new LinkedHashMap<RequestMappingInfo, Method>();
			for (Method handlerMethod : getHandlerMethods()) {
				RequestMappingInfo mappingInfo = this.mappings.get(handlerMethod);
				if (mappingInfo.match(request)) {
					Method oldMappedMethod = targetHandlerMethods.put(mappingInfo, handlerMethod);
					if (oldMappedMethod != null && oldMappedMethod != handlerMethod) {
						throw new IllegalStateException("Ambiguous handler methods mapped for portlet mode '" +
								request.getPortletMode() + "': {" + oldMappedMethod + ", " + handlerMethod +
								"}. If you intend to handle the same mode in multiple methods, then factor " +
								"them out into a dedicated handler class with that mode mapped at the type level!");
					}
				}
			}
			if (!targetHandlerMethods.isEmpty()) {
				if (targetHandlerMethods.size() == 1) {
					return targetHandlerMethods.values().iterator().next();
				}
				else {
					RequestMappingInfo bestMappingMatch = null;
					for (RequestMappingInfo mapping : targetHandlerMethods.keySet()) {
						if (bestMappingMatch == null) {
							bestMappingMatch = mapping;
						}
						else {
							if (mapping.isBetterMatchThan(bestMappingMatch)) {
								bestMappingMatch = mapping;
							}
						}
					}
					return targetHandlerMethods.get(bestMappingMatch);
				}
			}
			else {
				throw new NoHandlerFoundException("No matching handler method found for portlet request", request);
			}
		}

		private String determineDefaultPhase(Method handlerMethod) {
			if (!void.class.equals(handlerMethod.getReturnType())) {
				return PortletRequest.RENDER_PHASE;
			}
			for (Class<?> argType : handlerMethod.getParameterTypes()) {
				if (ActionRequest.class.isAssignableFrom(argType) || ActionResponse.class.isAssignableFrom(argType) ||
						InputStream.class.isAssignableFrom(argType) || Reader.class.isAssignableFrom(argType)) {
					return PortletRequest.ACTION_PHASE;
				}
				else if (RenderRequest.class.isAssignableFrom(argType) || RenderResponse.class.isAssignableFrom(argType) ||
						OutputStream.class.isAssignableFrom(argType) || Writer.class.isAssignableFrom(argType)) {
					return PortletRequest.RENDER_PHASE;
				}
				else if (ResourceRequest.class.isAssignableFrom(argType) || ResourceResponse.class.isAssignableFrom(argType)) {
					return PortletRequest.RESOURCE_PHASE;
				}
				else if (EventRequest.class.isAssignableFrom(argType) || EventResponse.class.isAssignableFrom(argType)) {
					return PortletRequest.EVENT_PHASE;
				}
			}
			return "";
		}
	}


	/**
	 * Portlet-specific subclass of {@link HandlerMethodInvoker}.
	 */
	private class PortletHandlerMethodInvoker extends HandlerMethodInvoker {

		public PortletHandlerMethodInvoker(HandlerMethodResolver resolver) {
			super(resolver, webBindingInitializer, sessionAttributeStore,
					parameterNameDiscoverer, customArgumentResolvers, null);
		}

		@Override
		protected void raiseMissingParameterException(String paramName, Class paramType) throws Exception {
			throw new MissingPortletRequestParameterException(paramName, paramType.getSimpleName());
		}

		@Override
		protected void raiseSessionRequiredException(String message) throws Exception {
			throw new PortletSessionRequiredException(message);
		}

		@Override
		protected WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
				throws Exception {

			return AnnotationMethodHandlerAdapter.this.createBinder(
					webRequest.getNativeRequest(PortletRequest.class), target, objectName);
		}

		@Override
		protected void doBind(WebDataBinder binder, NativeWebRequest webRequest) throws Exception {
			PortletRequestDataBinder portletBinder = (PortletRequestDataBinder) binder;
			portletBinder.bind(webRequest.getNativeRequest(PortletRequest.class));
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
		protected Object resolveCookieValue(String cookieName, Class paramType, NativeWebRequest webRequest)
				throws Exception {

			PortletRequest portletRequest = webRequest.getNativeRequest(PortletRequest.class);
			Cookie cookieValue = PortletUtils.getCookie(portletRequest, cookieName);
			if (Cookie.class.isAssignableFrom(paramType)) {
				return cookieValue;
			}
			else if (cookieValue != null) {
				return cookieValue.getValue();
			}
			else {
				return null;
			}
		}

		@Override
		protected Object resolveStandardArgument(Class<?> parameterType, NativeWebRequest webRequest)
				throws Exception {

			PortletRequest request = webRequest.getNativeRequest(PortletRequest.class);
			PortletResponse response = webRequest.getNativeResponse(PortletResponse.class);

			if (PortletRequest.class.isAssignableFrom(parameterType) ||
					MultipartRequest.class.isAssignableFrom(parameterType)) {
				Object nativeRequest = webRequest.getNativeRequest(parameterType);
				if (nativeRequest == null) {
					throw new IllegalStateException(
							"Current request is not of type [" + parameterType.getName() + "]: " + request);
				}
				return nativeRequest;
			}
			else if (PortletResponse.class.isAssignableFrom(parameterType)) {
				Object nativeResponse = webRequest.getNativeResponse(parameterType);
				if (nativeResponse == null) {
					throw new IllegalStateException(
							"Current response is not of type [" + parameterType.getName() + "]: " + response);
				}
				return nativeResponse;
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
			return super.resolveStandardArgument(parameterType, webRequest);
		}

		@SuppressWarnings("unchecked")
		public ModelAndView getModelAndView(Method handlerMethod, Class handlerType, Object returnValue, ExtendedModelMap implicitModel,
				PortletWebRequest webRequest) {
			// Invoke custom resolvers if present...
			if (customModelAndViewResolvers != null) {
				for (ModelAndViewResolver mavResolver : customModelAndViewResolvers) {
					org.springframework.web.servlet.ModelAndView smav =
							mavResolver.resolveModelAndView(handlerMethod, handlerType, returnValue, implicitModel, webRequest);
					if (smav != ModelAndViewResolver.UNRESOLVED) {
						return (smav.isReference() ?
								new ModelAndView(smav.getViewName(), smav.getModelMap()) :
								new ModelAndView(smav.getView(), smav.getModelMap()));
					}
				}
			}

			if (returnValue instanceof ModelAndView) {
				ModelAndView mav = (ModelAndView) returnValue;
				mav.getModelMap().mergeAttributes(implicitModel);
				return mav;
			}
			else if (returnValue instanceof org.springframework.web.servlet.ModelAndView) {
				org.springframework.web.servlet.ModelAndView smav = (org.springframework.web.servlet.ModelAndView) returnValue;
				ModelAndView mav = (smav.isReference() ?
						new ModelAndView(smav.getViewName(), smav.getModelMap()) :
						new ModelAndView(smav.getView(), smav.getModelMap()));
				mav.getModelMap().mergeAttributes(implicitModel);
				return mav;
			}
			else if (returnValue instanceof Model) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects(((Model) returnValue).asMap());
			}
			else if (returnValue instanceof View) {
				return new ModelAndView(returnValue).addAllObjects(implicitModel);
			}
			else if (handlerMethod.isAnnotationPresent(ModelAttribute.class)) {
				addReturnValueAsModelAttribute(handlerMethod, handlerType, returnValue, implicitModel);
				return new ModelAndView().addAllObjects(implicitModel);
			}
			else if (returnValue instanceof Map) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects((Map) returnValue);
			}
			else if (returnValue instanceof String) {
				return new ModelAndView((String) returnValue).addAllObjects(implicitModel);
			}
			else if (returnValue == null) {
				// Either returned null or was 'void' return.
				return null;
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
	}


	/**
	 * Holder for request mapping metadata. Allows for finding a best matching candidate.
	 */
	private static class RequestMappingInfo {

		public final Set<PortletMode> modes = new HashSet<PortletMode>();

		public String phase;

		public String value;

		public final Set<String> methods = new HashSet<String>();

		public String[] params = new String[0];

		public String[] headers = new String[0];

		public void initStandardMapping(String[] modes, RequestMethod[] methods, String[] params, String[] headers) {
			for (String mode : modes) {
				this.modes.add(new PortletMode(mode));
			}
			for (RequestMethod method : methods) {
				this.methods.add(method.name());
			}
			this.params = StringUtils.mergeStringArrays(this.params, params);
			this.headers = StringUtils.mergeStringArrays(this.headers, headers);
		}

		public void initPhaseMapping(String phase, String value, String[] params) {
			if (this.phase != null) {
				throw new IllegalStateException(
						"Invalid mapping - more than one phase specified: '" + this.phase + "', '" + phase + "'");
			}
			this.phase = phase;
			this.value = value;
			this.params = StringUtils.mergeStringArrays(this.params, params);
		}

		public boolean match(PortletRequest request) {
			if (!this.modes.isEmpty() && !this.modes.contains(request.getPortletMode())) {
				return false;
			}
			if (StringUtils.hasLength(this.phase) &&
					!this.phase.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE))) {
				return false;
			}
			if (StringUtils.hasLength(this.value)) {
				if (this.phase.equals(PortletRequest.ACTION_PHASE) &&
						!this.value.equals(request.getParameter(ActionRequest.ACTION_NAME))) {
					return false;
				}
				else if (this.phase.equals(PortletRequest.RENDER_PHASE) &&
						!(new WindowState(this.value)).equals(request.getWindowState())) {
					return false;
				}
				else if (this.phase.equals(PortletRequest.RESOURCE_PHASE) &&
						!this.value.equals(((ResourceRequest) request).getResourceID())) {
					return false;
				}
				else if (this.phase.equals(PortletRequest.EVENT_PHASE)) {
					Event event = ((EventRequest) request).getEvent();
					if (!this.value.equals(event.getName()) && !this.value.equals(event.getQName().toString())) {
						return false;
					}
				}
			}
			return PortletAnnotationMappingUtils.checkRequestMethod(this.methods, request) &&
					PortletAnnotationMappingUtils.checkParameters(this.params, request) &&
					PortletAnnotationMappingUtils.checkHeaders(this.headers, request);
		}

		public boolean isBetterMatchThan(RequestMappingInfo other) {
			return ((!this.modes.isEmpty() && other.modes.isEmpty()) ||
					(StringUtils.hasLength(this.phase) && !StringUtils.hasLength(other.phase)) ||
					(StringUtils.hasLength(this.value) && !StringUtils.hasLength(other.value)) ||
					(!this.methods.isEmpty() && other.methods.isEmpty()) ||
					this.params.length > other.params.length);
		}

		@Override
		public boolean equals(Object obj) {
			RequestMappingInfo other = (RequestMappingInfo) obj;
			return (this.modes.equals(other.modes) &&
					ObjectUtils.nullSafeEquals(this.phase, other.phase) &&
					ObjectUtils.nullSafeEquals(this.value, other.value) &&
					this.methods.equals(other.methods) &&
					Arrays.equals(this.params, other.params) &&
					Arrays.equals(this.headers, other.headers));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.modes) * 29 + this.phase.hashCode());
		}
	}

}
