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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.Source;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.ui.ModelMap;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.SimpleSessionStatus;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.method.annotation.ModelFactory;
import org.springframework.web.method.annotation.SessionAttributesHandler;
import org.springframework.web.method.annotation.support.ErrorsMethodArgumentResolver;
import org.springframework.web.method.annotation.support.ExpressionValueMethodArgumentResolver;
import org.springframework.web.method.annotation.support.ModelAttributeMethodProcessor;
import org.springframework.web.method.annotation.support.ModelMethodProcessor;
import org.springframework.web.method.annotation.support.RequestHeaderMapMethodArgumentResolver;
import org.springframework.web.method.annotation.support.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.support.RequestParamMapMethodArgumentResolver;
import org.springframework.web.method.annotation.support.RequestParamMethodArgumentResolver;
import org.springframework.web.method.annotation.support.WebArgumentResolverAdapter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.method.annotation.support.DefaultMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.support.HttpEntityMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.support.ModelAndViewMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.support.PathVariableMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletCookieValueMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletModelAttributeMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletRequestMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletResponseMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ViewMethodReturnValueHandler;
import org.springframework.web.util.WebUtils;

/**
 * An extension of {@link AbstractHandlerMethodAdapter} with support for {@link RequestMapping} handler methods.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestMappingHandlerMethodAdapter extends AbstractHandlerMethodAdapter implements BeanFactoryAware,
		InitializingBean {

	private WebArgumentResolver[] customArgumentResolvers;

	private ModelAndViewResolver[] customModelAndViewResolvers;

	private HttpMessageConverter<?>[] messageConverters;

	private WebBindingInitializer webBindingInitializer;

	private int cacheSecondsForSessionAttributeHandlers = 0;

	private boolean synchronizeOnSession = false;

	private ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
	
	private ConfigurableBeanFactory beanFactory;

	private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();
	
	private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache =
		new ConcurrentHashMap<Class<?>, SessionAttributesHandler>();

	private final Map<Class<?>, Set<Method>> modelAttributeMethodCache = new ConcurrentHashMap<Class<?>, Set<Method>>();

	private final Map<Class<?>, Set<Method>> initBinderMethodCache = new ConcurrentHashMap<Class<?>, Set<Method>>();

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;
	
	private HandlerMethodArgumentResolverComposite argumentResolvers;

	private HandlerMethodArgumentResolverComposite initBinderArgumentResolvers;
	
	/**
	 * Create a {@link RequestMappingHandlerMethodAdapter} instance.
	 */
	public RequestMappingHandlerMethodAdapter() {
		
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false); // See SPR-7316
		
		this.messageConverters = new HttpMessageConverter[] { new ByteArrayHttpMessageConverter(),
				stringHttpMessageConverter, new SourceHttpMessageConverter<Source>(),
				new XmlAwareFormHttpMessageConverter() };
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
		this.sessionAttributeStore = sessionAttributeStore;
	}
	
	/**
	 * Cache content produced by <code>@SessionAttributes</code> annotated handlers
	 * for the given number of seconds. Default is 0, preventing caching completely.
	 * <p>In contrast to the "cacheSeconds" property which will apply to all general handlers
	 * (but not to <code>@SessionAttributes</code> annotated handlers), this setting will
	 * apply to <code>@SessionAttributes</code> annotated handlers only.
	 * @see #setCacheSeconds
	 * @see org.springframework.web.bind.annotation.SessionAttributes
	 */
	public void setCacheSecondsForSessionAttributeHandlers(int cacheSecondsForSessionAttributeHandlers) {
		this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
	}

	/**
	 * Set if controller execution should be synchronized on the session,
	 * to serialize parallel invocations from the same client.
	 * <p>More specifically, the execution of the <code>handleRequestInternal</code>
	 * method will get synchronized if this flag is "true". The best available
	 * session mutex will be used for the synchronization; ideally, this will
	 * be a mutex exposed by HttpSessionMutexListener.
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
	 * Set the ParameterNameDiscoverer to use for resolving method parameter names if needed
	 * (e.g. for default attribute names).
	 * <p>Default is a {@link org.springframework.core.LocalVariableTableParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}
	
	/**
	 * Set the {@link HandlerMethodArgumentResolver}s to use to resolve argument values for {@link RequestMapping} 
	 * and {@link ModelAttribute} methods. This is an optional property.
	 * @param argumentResolvers the argument resolvers to use
	 */
	public void setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolver[] argumentResolvers) {
		this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
		for (HandlerMethodArgumentResolver resolver : argumentResolvers) {
			this.argumentResolvers.registerArgumentResolver(resolver);
		}
	}

	/**
	 * Set the {@link HandlerMethodReturnValueHandler}s to use to handle the return values of 
	 * {@link RequestMapping} methods. This is an optional property.
	 * @param returnValueHandlers the return value handlers to use
	 */
	public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandler[] returnValueHandlers) {
		this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
		for (HandlerMethodReturnValueHandler handler : returnValueHandlers) {
			this.returnValueHandlers.registerReturnValueHandler(handler);
		}
	}

	/**
	 * Set the {@link HandlerMethodArgumentResolver}s to use to resolve argument values for {@link InitBinder} 
	 * methods. This is an optional property.
	 * @param argumentResolvers the argument resolvers to use
	 */
	public void setInitBinderMethodArgumentResolvers(HandlerMethodArgumentResolver[] argumentResolvers) {
		this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
		for (HandlerMethodArgumentResolver resolver : argumentResolvers) {
			this.initBinderArgumentResolvers.registerArgumentResolver(resolver);
		}
	}
	
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	public void afterPropertiesSet() throws Exception {
		initHandlerMethodArgumentResolvers();
		initHandlerMethodReturnValueHandlers();
		initBinderMethodArgumentResolvers();
	}

	private void initHandlerMethodArgumentResolvers() {
		if (argumentResolvers != null) {
			return;
		}
		argumentResolvers = new HandlerMethodArgumentResolverComposite();
		
		// Annotation-based resolvers
		argumentResolvers.registerArgumentResolver(new RequestParamMethodArgumentResolver(beanFactory, false));
		argumentResolvers.registerArgumentResolver(new RequestParamMapMethodArgumentResolver());
		argumentResolvers.registerArgumentResolver(new PathVariableMethodArgumentResolver(beanFactory));
		argumentResolvers.registerArgumentResolver(new ServletModelAttributeMethodProcessor(false));
		argumentResolvers.registerArgumentResolver(new RequestResponseBodyMethodProcessor(messageConverters));
		argumentResolvers.registerArgumentResolver(new RequestHeaderMethodArgumentResolver(beanFactory));
		argumentResolvers.registerArgumentResolver(new RequestHeaderMapMethodArgumentResolver());
		argumentResolvers.registerArgumentResolver(new ServletCookieValueMethodArgumentResolver(beanFactory));
		argumentResolvers.registerArgumentResolver(new ExpressionValueMethodArgumentResolver(beanFactory));

		if (customArgumentResolvers != null) {
			for (WebArgumentResolver customResolver : customArgumentResolvers) {
				argumentResolvers.registerArgumentResolver(new WebArgumentResolverAdapter(customResolver));
			}
		}
		
		// Type-based resolvers
		argumentResolvers.registerArgumentResolver(new ServletRequestMethodArgumentResolver());
		argumentResolvers.registerArgumentResolver(new ServletResponseMethodArgumentResolver());
		argumentResolvers.registerArgumentResolver(new HttpEntityMethodProcessor(messageConverters));
		argumentResolvers.registerArgumentResolver(new ModelMethodProcessor());
		argumentResolvers.registerArgumentResolver(new ErrorsMethodArgumentResolver());
		
		// Default-mode resolution
		argumentResolvers.registerArgumentResolver(new RequestParamMethodArgumentResolver(beanFactory, true));
		argumentResolvers.registerArgumentResolver(new ServletModelAttributeMethodProcessor(true));
	}	

	private void initBinderMethodArgumentResolvers() {
		if (initBinderArgumentResolvers != null) {
			return;
		}
		initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
		
		// Annotation-based resolvers
		initBinderArgumentResolvers.registerArgumentResolver(new RequestParamMethodArgumentResolver(beanFactory, false));
		initBinderArgumentResolvers.registerArgumentResolver(new RequestParamMapMethodArgumentResolver());
		initBinderArgumentResolvers.registerArgumentResolver(new PathVariableMethodArgumentResolver(beanFactory));
		initBinderArgumentResolvers.registerArgumentResolver(new ExpressionValueMethodArgumentResolver(beanFactory));

		if (customArgumentResolvers != null) {
			for (WebArgumentResolver customResolver : customArgumentResolvers) {
				initBinderArgumentResolvers.registerArgumentResolver(new WebArgumentResolverAdapter(customResolver));
			}
		}

		// Type-based resolvers
		initBinderArgumentResolvers.registerArgumentResolver(new ServletRequestMethodArgumentResolver());
		initBinderArgumentResolvers.registerArgumentResolver(new ServletResponseMethodArgumentResolver());
		
		// Default-mode resolution
		initBinderArgumentResolvers.registerArgumentResolver(new RequestParamMethodArgumentResolver(beanFactory, true));
	}

	private void initHandlerMethodReturnValueHandlers() {
		if (returnValueHandlers != null) {
			return;
		}
		returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
		
		// Annotation-based handlers
		returnValueHandlers.registerReturnValueHandler(new RequestResponseBodyMethodProcessor(messageConverters));
		returnValueHandlers.registerReturnValueHandler(new ModelAttributeMethodProcessor(false));
		
		// Type-based handlers
		returnValueHandlers.registerReturnValueHandler(new ModelAndViewMethodReturnValueHandler());
		returnValueHandlers.registerReturnValueHandler(new ModelMethodProcessor());
		returnValueHandlers.registerReturnValueHandler(new ViewMethodReturnValueHandler());
		returnValueHandlers.registerReturnValueHandler(new HttpEntityMethodProcessor(messageConverters));
		
		// Default handler
		returnValueHandlers.registerReturnValueHandler(new DefaultMethodReturnValueHandler(customModelAndViewResolvers));
	}

	@Override
	protected boolean supportsInternal(HandlerMethod handlerMethod) {
		return supportsMethodParameters(handlerMethod.getMethodParameters()) && 
			supportsReturnType(handlerMethod.getReturnType());
	}
	
	private boolean supportsMethodParameters(MethodParameter[] methodParameters) {
		for (MethodParameter methodParameter : methodParameters) {
			if (! this.argumentResolvers.supportsParameter(methodParameter)) {
				return false;
			}
		}
		return true;
	}

	private boolean supportsReturnType(MethodParameter methodReturnType) {
		return (this.returnValueHandlers.supportsReturnType(methodReturnType) ||
				Void.TYPE.equals(methodReturnType.getParameterType()));
	}

	@Override
	protected long getLastModifiedInternal(HttpServletRequest request, HandlerMethod handlerMethod) {
		return -1;
	}

	@Override
	protected final ModelAndView handleInternal(HttpServletRequest request,
												HttpServletResponse response,
												HandlerMethod handlerMethod) throws Exception {
		
		if (hasSessionAttributes(handlerMethod.getBeanType())) {
			// Always prevent caching in case of session attribute management.
			checkAndPrepare(request, response, this.cacheSecondsForSessionAttributeHandlers, true);
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
					return invokeHandlerMethod(request, response, handlerMethod);
				}
			}
		}
		
		return invokeHandlerMethod(request, response, handlerMethod);
	}

	private boolean hasSessionAttributes(Class<?> handlerType) {
		SessionAttributesHandler handler = null;
		synchronized(this.sessionAttributesHandlerCache) {
			handler = this.sessionAttributesHandlerCache.get(handlerType);
			if (handler == null) {
				handler = new SessionAttributesHandler(handlerType, sessionAttributeStore);
				this.sessionAttributesHandlerCache.put(handlerType, handler);
			}
		}
		return handler.hasSessionAttributes();
	}

	private ModelAndView invokeHandlerMethod(HttpServletRequest request, 
											 HttpServletResponse response, 
											 HandlerMethod handlerMethod) throws Exception {
		
		WebDataBinderFactory binderFactory = createDataBinderFactory(handlerMethod);
		ModelFactory modelFactory = createModelFactory(handlerMethod, binderFactory);
		ServletInvocableHandlerMethod requestMethod = createRequestMappingMethod(handlerMethod, binderFactory);

		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		SessionStatus sessionStatus = new SimpleSessionStatus();
		
		ModelMap implicitModel = modelFactory.createModel(webRequest, requestMethod);
		ModelAndView mav = requestMethod.invokeAndHandle(webRequest, implicitModel, sessionStatus);

		ModelMap actualModel = (mav != null) ? mav.getModelMap() : null;
		modelFactory.updateAttributes(webRequest, sessionStatus, actualModel, implicitModel);

		return mav;
	}

	private WebDataBinderFactory createDataBinderFactory(HandlerMethod handlerMethod) {
		List<InvocableHandlerMethod> initBinderMethods = new ArrayList<InvocableHandlerMethod>();

		Class<?> handlerType = handlerMethod.getBeanType();
		Set<Method> binderMethods = initBinderMethodCache.get(handlerType);
		if (binderMethods == null) {
			binderMethods = HandlerMethodSelector.selectMethods(handlerType, INIT_BINDER_METHODS);
			initBinderMethodCache.put(handlerType, binderMethods);
		}

		for (Method method : binderMethods) {
			Object bean = handlerMethod.getBean();
			InvocableHandlerMethod binderMethod = new InvocableHandlerMethod(bean, method);
			binderMethod.setHandlerMethodArgumentResolvers(this.initBinderArgumentResolvers);
			binderMethod.setDataBinderFactory(new DefaultDataBinderFactory(this.webBindingInitializer));
			binderMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);

			initBinderMethods.add(binderMethod);
		}

		return new ServletInitBinderMethodDataBinderFactory(initBinderMethods, this.webBindingInitializer);
	}

	private ModelFactory createModelFactory(HandlerMethod handlerMethod, WebDataBinderFactory binderFactory) {
		List<InvocableHandlerMethod> modelAttrMethods = new ArrayList<InvocableHandlerMethod>();

		Class<?> handlerType = handlerMethod.getBeanType();
		Set<Method> attributeMethods = modelAttributeMethodCache.get(handlerType);
		if (attributeMethods == null) {
			attributeMethods = HandlerMethodSelector.selectMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
			modelAttributeMethodCache.put(handlerType, attributeMethods);
		}

		for (Method method : attributeMethods) {
			InvocableHandlerMethod attrMethod = new InvocableHandlerMethod(handlerMethod.getBean(), method);
			attrMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
			attrMethod.setDataBinderFactory(binderFactory);
			attrMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
			modelAttrMethods.add(attrMethod);
		}

		return new ModelFactory(modelAttrMethods, binderFactory, sessionAttributesHandlerCache.get(handlerType));
	}

	private ServletInvocableHandlerMethod createRequestMappingMethod(HandlerMethod handlerMethod, 
															   		 WebDataBinderFactory binderFactory) {
		Method method = handlerMethod.getMethod();
		ServletInvocableHandlerMethod requestMethod = new ServletInvocableHandlerMethod(handlerMethod.getBean(), method);
		requestMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		requestMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
		requestMethod.setDataBinderFactory(binderFactory);
		requestMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
		return requestMethod;
	}

	/**
	 * MethodFilter that matches {@link InitBinder @InitBinder} methods.
	 */
	public static MethodFilter INIT_BINDER_METHODS = new MethodFilter() {

		public boolean matches(Method method) {
			return AnnotationUtils.findAnnotation(method, InitBinder.class) != null;
		}
	};

	/**
	 * MethodFilter that matches {@link ModelAttribute @ModelAttribute} methods.
	 */
	public static MethodFilter MODEL_ATTRIBUTE_METHODS = new MethodFilter() {

		public boolean matches(Method method) {
			return ((AnnotationUtils.findAnnotation(method, RequestMapping.class) == null) && 
					(AnnotationUtils.findAnnotation(method, ModelAttribute.class) != null));
		}
	};

}
