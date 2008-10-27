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

package org.springframework.web.portlet.mvc.annotation;

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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.UnavailableException;
import javax.portlet.WindowState;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.style.StylerUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.annotation.support.HandlerMethodInvoker;
import org.springframework.web.bind.annotation.support.HandlerMethodResolver;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.MissingPortletRequestParameterException;
import org.springframework.web.portlet.bind.PortletRequestDataBinder;
import org.springframework.web.portlet.context.PortletWebRequest;
import org.springframework.web.portlet.handler.PortletContentGenerator;
import org.springframework.web.portlet.handler.PortletSessionRequiredException;
import org.springframework.web.portlet.util.PortletUtils;
import org.springframework.web.servlet.View;

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
public class AnnotationMethodHandlerAdapter extends PortletContentGenerator implements HandlerAdapter {

	private static final String IMPLICIT_MODEL_ATTRIBUTE = "org.springframework.web.portlet.mvc.ImplicitModel";


	private WebBindingInitializer webBindingInitializer;

	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

	private int cacheSecondsForSessionAttributeHandlers = 0;

	private boolean synchronizeOnSession = false;

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	private WebArgumentResolver[] customArgumentResolvers;

	private final Map<Class<?>, PortletHandlerMethodResolver> methodResolverCache =
			new ConcurrentHashMap<Class<?>, PortletHandlerMethodResolver>();


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

	public void handleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception {
		Object returnValue = doHandle(request, response, handler);
		if (returnValue != null) {
			throw new IllegalStateException("Invalid action method return value: " + returnValue);
		}
	}

	public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception {
		checkAndPrepare(request, response);
		return doHandle(request, response, handler);
	}

	protected ModelAndView doHandle(PortletRequest request, PortletResponse response, Object handler) throws Exception {
		ExtendedModelMap implicitModel = null;

		if (request instanceof RenderRequest && response instanceof RenderResponse) {
			RenderRequest renderRequest = (RenderRequest) request;
			RenderResponse renderResponse = (RenderResponse) response;
			// Detect implicit model from associated action phase.
			if (renderRequest.getParameter(IMPLICIT_MODEL_ATTRIBUTE) != null) {
				PortletSession session = request.getPortletSession(false);
				if (session != null) {
					implicitModel = (ExtendedModelMap) session.getAttribute(IMPLICIT_MODEL_ATTRIBUTE);
				}
			}
			if (handler.getClass().getAnnotation(SessionAttributes.class) != null) {
				// Always prevent caching in case of session attribute management.
				checkAndPrepare(renderRequest, renderResponse, this.cacheSecondsForSessionAttributeHandlers);
			}
			else {
				// Uses configured default cacheSeconds setting.
				checkAndPrepare(renderRequest, renderResponse);
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

	private ModelAndView invokeHandlerMethod(
			PortletRequest request, PortletResponse response, Object handler, ExtendedModelMap implicitModel)
			throws Exception {

		PortletWebRequest webRequest = new PortletWebRequest(request, response);
		PortletHandlerMethodResolver methodResolver = getMethodResolver(handler);
		Method handlerMethod = methodResolver.resolveHandlerMethod(request, response);
		PortletHandlerMethodInvoker methodInvoker = new PortletHandlerMethodInvoker(methodResolver);

		Object result = methodInvoker.invokeHandlerMethod(handlerMethod, handler, webRequest, implicitModel);
		ModelAndView mav = methodInvoker.getModelAndView(handlerMethod, handler.getClass(), result, implicitModel);
		methodInvoker.updateModelAttributes(
				handler, (mav != null ? mav.getModel() : null), implicitModel, webRequest);

		// Expose implicit model for subsequent render phase.
		if (response instanceof ActionResponse && !implicitModel.isEmpty()) {
			ActionResponse actionResponse = (ActionResponse) response;
			try {
				actionResponse.setRenderParameter(IMPLICIT_MODEL_ATTRIBUTE, Boolean.TRUE.toString());
				request.getPortletSession().setAttribute(IMPLICIT_MODEL_ATTRIBUTE, implicitModel);
			}
			catch (IllegalStateException ex) {
				// Probably sendRedirect called... no need to expose model to render phase.
			}
		}

		return mav;
	}


	/**
	 * Template method for creating a new PortletRequestDataBinder instance.
	 * <p>The default implementation creates a standard PortletRequestDataBinder.
	 * This can be overridden for custom PortletRequestDataBinder subclasses.
	 * @param request current portlet request
	 * @param target the target object to bind onto (or <code>null</code>
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the objectName of the target object
	 * @return the PortletRequestDataBinder instance to use
	 * @throws Exception in case of invalid state or arguments
	 * @see PortletRequestDataBinder#bind(javax.portlet.PortletRequest)
	 * @see PortletRequestDataBinder#convertIfNecessary(Object, Class, MethodParameter)
	 */
	protected PortletRequestDataBinder createBinder(
			PortletRequest request, Object target, String objectName) throws Exception {

		return new PortletRequestDataBinder(target, objectName);
	}

	/**
	 * Build a HandlerMethodResolver for the given handler type.
	 */
	private PortletHandlerMethodResolver getMethodResolver(Object handler) {
		Class handlerClass = ClassUtils.getUserClass(handler);
		PortletHandlerMethodResolver resolver = this.methodResolverCache.get(handlerClass);
		if (resolver == null) {
			resolver = new PortletHandlerMethodResolver(handlerClass);
			this.methodResolverCache.put(handlerClass, resolver);
		}
		return resolver;
	}


	private static class PortletHandlerMethodResolver extends HandlerMethodResolver {

		public PortletHandlerMethodResolver(Class<?> handlerType) {
			super(handlerType);
		}

		public Method resolveHandlerMethod(PortletRequest request, PortletResponse response) throws PortletException {
			String lookupMode = request.getPortletMode().toString();
			Map<RequestMappingInfo, Method> targetHandlerMethods = new LinkedHashMap<RequestMappingInfo, Method>();
			for (Method handlerMethod : getHandlerMethods()) {
				RequestMapping mapping = AnnotationUtils.findAnnotation(handlerMethod, RequestMapping.class);
				RequestMappingInfo mappingInfo = new RequestMappingInfo();
				mappingInfo.modes = mapping.value();
				mappingInfo.params = mapping.params();
				mappingInfo.action = isActionMethod(handlerMethod);
				mappingInfo.render = isRenderMethod(handlerMethod);
				boolean match = false;
				if (mappingInfo.modes.length > 0) {
					for (String mappedMode : mappingInfo.modes) {
						if (mappedMode.equalsIgnoreCase(lookupMode)) {
							if (checkParameters(request, response, mappingInfo)) {
								match = true;
							}
							else {
								break;
							}
						}
					}
				}
				else {
					// No modes specified: parameter match sufficient.
					match = checkParameters(request, response, mappingInfo);
				}
				if (match) {
					Method oldMappedMethod = targetHandlerMethods.put(mappingInfo, handlerMethod);
					if (oldMappedMethod != null && oldMappedMethod != handlerMethod) {
						throw new IllegalStateException("Ambiguous handler methods mapped for portlet mode '" +
								lookupMode + "': {" + oldMappedMethod + ", " + handlerMethod +
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
							if ((bestMappingMatch.modes.length == 0 && mapping.modes.length > 0) ||
									bestMappingMatch.params.length < mapping.params.length) {
								bestMappingMatch = mapping;
							}
						}
					}
					return targetHandlerMethods.get(bestMappingMatch);
				}
			}
			else {
				throw new UnavailableException("No matching handler method found for portlet request: mode '" +
						request.getPortletMode() + "', type '" + (response instanceof ActionResponse ? "action" : "render") +
						"', parameters " + StylerUtils.style(request.getParameterMap()));
			}
		}

		private boolean checkParameters(PortletRequest request, PortletResponse response, RequestMappingInfo mapping) {
			if (response instanceof RenderResponse) {
				if (mapping.action) {
					return false;
				}
			}
			else if (response instanceof ActionResponse) {
				if (mapping.render) {
					return false;
				}
			}
			return PortletAnnotationMappingUtils.checkParameters(mapping.params, request);
		}

		private boolean isActionMethod(Method handlerMethod) {
			if (!void.class.equals(handlerMethod.getReturnType())) {
				return false;
			}
			for (Class<?> argType : handlerMethod.getParameterTypes()) {
				if (ActionRequest.class.isAssignableFrom(argType) || ActionResponse.class.isAssignableFrom(argType) ||
						InputStream.class.isAssignableFrom(argType) || Reader.class.isAssignableFrom(argType)) {
					return true;
				}
			}
			return false;
		}

		private boolean isRenderMethod(Method handlerMethod) {
			if (!void.class.equals(handlerMethod.getReturnType())) {
				return true;
			}
			for (Class<?> argType : handlerMethod.getParameterTypes()) {
				if (RenderRequest.class.isAssignableFrom(argType) || RenderResponse.class.isAssignableFrom(argType) ||
						OutputStream.class.isAssignableFrom(argType) || Writer.class.isAssignableFrom(argType)) {
					return true;
				}
			}
			return false;
		}
	}


	private class PortletHandlerMethodInvoker extends HandlerMethodInvoker {

		public PortletHandlerMethodInvoker(HandlerMethodResolver resolver) {
			super(resolver, webBindingInitializer, sessionAttributeStore,
					parameterNameDiscoverer, customArgumentResolvers);
		}

		@Override
		protected void raiseMissingParameterException(String paramName, Class paramType) throws Exception {
			throw new MissingPortletRequestParameterException(paramName, paramType.getName());
		}

		@Override
		protected void raiseSessionRequiredException(String message) throws Exception {
			throw new PortletSessionRequiredException(message);
		}

		@Override
		protected WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
				throws Exception {

			return AnnotationMethodHandlerAdapter.this.createBinder(
					(PortletRequest) webRequest.getNativeRequest(), target, objectName);
		}

		@Override
		protected void doBind(NativeWebRequest webRequest, WebDataBinder binder, boolean failOnErrors)
				throws Exception {

			PortletRequestDataBinder servletBinder = (PortletRequestDataBinder) binder;
			servletBinder.bind((PortletRequest) webRequest.getNativeRequest());
			if (failOnErrors) {
				servletBinder.closeNoCatch();
			}
		}

		@Override
		protected Object resolveStandardArgument(Class parameterType, NativeWebRequest webRequest)
				throws Exception {

			PortletRequest request = (PortletRequest) webRequest.getNativeRequest();
			PortletResponse response = (PortletResponse) webRequest.getNativeResponse();

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
				if (!(request instanceof ActionRequest)) {
					throw new IllegalStateException("InputStream can only get obtained for ActionRequest");
				}
				return ((ActionRequest) request).getPortletInputStream();
			}
			else if (Reader.class.isAssignableFrom(parameterType)) {
				if (!(request instanceof ActionRequest)) {
					throw new IllegalStateException("Reader can only get obtained for ActionRequest");
				}
				return ((ActionRequest) request).getReader();
			}
			else if (OutputStream.class.isAssignableFrom(parameterType)) {
				if (!(response instanceof RenderResponse)) {
					throw new IllegalStateException("OutputStream can only get obtained for RenderResponse");
				}
				return ((RenderResponse) response).getPortletOutputStream();
			}
			else if (Writer.class.isAssignableFrom(parameterType)) {
				if (!(response instanceof RenderResponse)) {
					throw new IllegalStateException("Writer can only get obtained for RenderResponse");
				}
				return ((RenderResponse) response).getWriter();
			}
			return super.resolveStandardArgument(parameterType, webRequest);
		}

		@SuppressWarnings("unchecked")
		public ModelAndView getModelAndView(
				Method handlerMethod, Class handlerType, Object returnValue, ExtendedModelMap implicitModel) {

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
			else if (returnValue instanceof Map) {
				return new ModelAndView().addAllObjects(implicitModel).addAllObjects((Map) returnValue);
			}
			else if (returnValue instanceof View) {
				return new ModelAndView(returnValue).addAllObjects(implicitModel);
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

		public String[] modes = new String[0];

		public String[] params = new String[0];

		private boolean action = false;

		private boolean render = false;

		public boolean equals(Object obj) {
			RequestMappingInfo other = (RequestMappingInfo) obj;
			return (this.action == other.action && this.render == other.render &&
					Arrays.equals(this.modes, other.modes) && Arrays.equals(this.params, other.params));
		}

		public int hashCode() {
			return (Arrays.hashCode(this.modes) * 29 + Arrays.hashCode(this.params));
		}
	}

}
