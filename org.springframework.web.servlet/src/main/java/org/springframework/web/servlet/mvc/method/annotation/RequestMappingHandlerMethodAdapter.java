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
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
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
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
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
import org.springframework.web.servlet.mvc.method.annotation.support.ServletWebArgumentResolverAdapter;
import org.springframework.web.servlet.mvc.method.annotation.support.ViewMethodReturnValueHandler;
import org.springframework.web.util.WebUtils;

/**
 * An {@link AbstractHandlerMethodAdapter} variant with support for {@link RequestMapping} handler methods.
 *
 * <p>Processing a {@link RequestMapping} method typically involves the invocation of {@link ModelAttribute}
 * methods for contributing attributes to the model and {@link InitBinder} methods for initializing
 * {@link WebDataBinder} instances for data binding and type conversion purposes.
 *
 * <p>{@link InvocableHandlerMethod} is the key contributor that helps with the invocation of handler
 * methods of all types resolving their arguments through registered {@link HandlerMethodArgumentResolver}s.
 * {@link ServletInvocableHandlerMethod} on the other hand adds handling of the return value for {@link RequestMapping}
 * methods through registered {@link HandlerMethodReturnValueHandler}s resulting in a {@link ModelAndView}.
 *
 * <p>{@link ModelFactory} is another contributor that assists with the invocation of all {@link ModelAttribute}
 * methods to populate a model while {@link ServletRequestDataBinderFactory} assists with the invocation of
 * {@link InitBinder} methods for initializing data binder instances when needed.
 *
 * <p>This class is the central point that assembles all of mentioned contributors and invokes the actual
 * {@link RequestMapping} handler method through a {@link ServletInvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see InvocableHandlerMethod
 * @see ServletInvocableHandlerMethod
 * @see HandlerMethodArgumentResolver
 * @see HandlerMethodReturnValueHandler
 */
public class RequestMappingHandlerMethodAdapter extends AbstractHandlerMethodAdapter implements BeanFactoryAware,
		InitializingBean {

	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;
	
	private List<ModelAndViewResolver> modelAndViewResolvers;

	private List<HttpMessageConverter<?>> messageConverters;

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
		
		messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(stringHttpMessageConverter);
		messageConverters.add(new SourceHttpMessageConverter<Source>());
		messageConverters.add(new XmlAwareFormHttpMessageConverter());
	}

	/**
	 * Set one or more custom argument resolvers to use with {@link RequestMapping}, {@link ModelAttribute}, and
	 * {@link InitBinder} methods. Custom argument resolvers are given a chance to resolve argument values
	 * ahead of the standard argument resolvers registered by default.
	 * <p>An existing {@link WebArgumentResolver} can either adapted with {@link ServletWebArgumentResolverAdapter}
	 * or preferably converted to a {@link HandlerMethodArgumentResolver} instead.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}
	
	/**
	 * Set the argument resolvers to use with {@link RequestMapping} and {@link ModelAttribute} methods.
	 * This is an optional property providing full control over all argument resolvers in contrast to
	 * {@link #setCustomArgumentResolvers(List)}, which does not override default registrations.
	 * @param argumentResolvers argument resolvers for {@link RequestMapping} and {@link ModelAttribute} methods
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers != null) {
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.argumentResolvers.addResolvers(argumentResolvers);
		}
	}
	
	/**
	 * Set the argument resolvers to use with {@link InitBinder} methods. This is an optional property
	 * providing full control over all argument resolvers for {@link InitBinder} methods in contrast to
	 * {@link #setCustomArgumentResolvers(List)}, which does not override default registrations.
	 * @param argumentResolvers argument resolvers for {@link InitBinder} methods
	 */
	public void setInitBinderArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers != null) {
			this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.initBinderArgumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * Set custom return value handlers to use to handle the return values of {@link RequestMapping} methods.
	 * Custom return value handlers are given a chance to handle a return value before the standard
	 * return value handlers registered by default.
	 * @param returnValueHandlers custom return value handlers for {@link RequestMapping} methods
	 */
	public void setCustomReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.customReturnValueHandlers = returnValueHandlers;
	}

	/**
	 * Set the {@link HandlerMethodReturnValueHandler}s to use to use with {@link RequestMapping} methods.
	 * This is an optional property providing full control over all return value handlers in contrast to
	 * {@link #setCustomReturnValueHandlers(List)}, which does not override default registrations.
	 * @param returnValueHandlers the return value handlers for {@link RequestMapping} methods
	 */
	public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers != null) {
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			this.returnValueHandlers.addHandlers(returnValueHandlers);
		}
	}

	/**
	 * Set custom {@link ModelAndViewResolver}s to use to handle the return values of {@link RequestMapping} methods.
	 * <p>Custom {@link ModelAndViewResolver}s are provided for backward compatibility and are invoked at the very,
	 * from the {@link DefaultMethodReturnValueHandler}, after all standard {@link HandlerMethodReturnValueHandler}s
	 * have been given a chance. This is because {@link ModelAndViewResolver}s do not have a method to indicate
	 * if they support a given return type or not. For this reason it is recommended to use
	 * {@link HandlerMethodReturnValueHandler} and {@link #setCustomReturnValueHandlers(List)} instead.
	 */
	public void setModelAndViewResolvers(List<ModelAndViewResolver> modelAndViewResolvers) {
		this.modelAndViewResolvers = modelAndViewResolvers;
	}

	/**
	 * Set the message body converters to use.
	 * <p>These converters are used to convert from and to HTTP requests and responses.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * Return the message body converters that this adapter has been configured with.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return messageConverters;
	}

	/**
	 * Set a WebBindingInitializer to apply configure every DataBinder instance this controller uses.
	 */
	public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * Return the WebBindingInitializer which applies pre-configured configuration to {@link DataBinder} instances.
	 */
	public WebBindingInitializer getWebBindingInitializer() {
		return webBindingInitializer;
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
	
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	public void afterPropertiesSet() throws Exception {
		initArgumentResolvers();
		initReturnValueHandlers();
		initInitBinderArgumentResolvers();
	}

	private void initArgumentResolvers() {
		if (argumentResolvers == null) {
			argumentResolvers = new HandlerMethodArgumentResolverComposite();
		}
		
		// Annotation-based resolvers
		argumentResolvers.addResolver(new RequestParamMethodArgumentResolver(beanFactory, false));
		argumentResolvers.addResolver(new RequestParamMapMethodArgumentResolver());
		argumentResolvers.addResolver(new PathVariableMethodArgumentResolver());
		argumentResolvers.addResolver(new ServletModelAttributeMethodProcessor(false));
		argumentResolvers.addResolver(new RequestResponseBodyMethodProcessor(messageConverters));
		argumentResolvers.addResolver(new RequestHeaderMethodArgumentResolver(beanFactory));
		argumentResolvers.addResolver(new RequestHeaderMapMethodArgumentResolver());
		argumentResolvers.addResolver(new ServletCookieValueMethodArgumentResolver(beanFactory));
		argumentResolvers.addResolver(new ExpressionValueMethodArgumentResolver(beanFactory));
		
		// Custom resolvers
		argumentResolvers.addResolvers(customArgumentResolvers);

		// Type-based resolvers
		argumentResolvers.addResolver(new ServletRequestMethodArgumentResolver());
		argumentResolvers.addResolver(new ServletResponseMethodArgumentResolver());
		argumentResolvers.addResolver(new HttpEntityMethodProcessor(messageConverters));
		argumentResolvers.addResolver(new ModelMethodProcessor());
		argumentResolvers.addResolver(new ErrorsMethodArgumentResolver());
		
		// Default-mode resolution
		argumentResolvers.addResolver(new RequestParamMethodArgumentResolver(beanFactory, true));
		argumentResolvers.addResolver(new ServletModelAttributeMethodProcessor(true));
	}

	private void initInitBinderArgumentResolvers() {
		if (initBinderArgumentResolvers == null) {
			initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
		}

		// Annotation-based resolvers
		initBinderArgumentResolvers.addResolver(new RequestParamMethodArgumentResolver(beanFactory, false));
		initBinderArgumentResolvers.addResolver(new RequestParamMapMethodArgumentResolver());
		initBinderArgumentResolvers.addResolver(new PathVariableMethodArgumentResolver());
		initBinderArgumentResolvers.addResolver(new ExpressionValueMethodArgumentResolver(beanFactory));

		// Custom resolvers
		argumentResolvers.addResolvers(customArgumentResolvers);

		// Type-based resolvers
		initBinderArgumentResolvers.addResolver(new ServletRequestMethodArgumentResolver());
		initBinderArgumentResolvers.addResolver(new ServletResponseMethodArgumentResolver());
		
		// Default-mode resolution
		initBinderArgumentResolvers.addResolver(new RequestParamMethodArgumentResolver(beanFactory, true));
	}
	
	private void initReturnValueHandlers() {
		if (returnValueHandlers == null) {
			returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
		}
		
		// Annotation-based handlers
		returnValueHandlers.addHandler(new RequestResponseBodyMethodProcessor(messageConverters));
		returnValueHandlers.addHandler(new ModelAttributeMethodProcessor(false));
		
		// Custom return value handlers
		returnValueHandlers.addHandlers(customReturnValueHandlers);
		
		// Type-based handlers
		returnValueHandlers.addHandler(new ModelAndViewMethodReturnValueHandler());
		returnValueHandlers.addHandler(new ModelMethodProcessor());
		returnValueHandlers.addHandler(new ViewMethodReturnValueHandler());
		returnValueHandlers.addHandler(new HttpEntityMethodProcessor(messageConverters));
		
		// Default handler
		returnValueHandlers.addHandler(new DefaultMethodReturnValueHandler(modelAndViewResolvers));
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

	/**
	 * Whether the given handler type defines any handler-specific session attributes via {@link SessionAttributes}.
	 * Also initializes the sessionAttributesHandlerCache for the given handler type.
	 */
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

	/**
	 * Invoke the {@link RequestMapping} handler method preparing a {@link ModelAndView} if view resolution is required.
	 */
	private ModelAndView invokeHandlerMethod(HttpServletRequest request,
											 HttpServletResponse response,
											 HandlerMethod handlerMethod) throws Exception {
		
		WebDataBinderFactory binderFactory = createDataBinderFactory(handlerMethod);
		ModelFactory modelFactory = createModelFactory(handlerMethod, binderFactory);
		ServletInvocableHandlerMethod requestMethod = createRequestMappingMethod(handlerMethod, binderFactory);

		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		SessionStatus sessionStatus = new SimpleSessionStatus();
		
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		
		modelFactory.initModel(webRequest, mavContainer, requestMethod);
		
		requestMethod.invokeAndHandle(webRequest, mavContainer, sessionStatus);

		modelFactory.updateModel(webRequest, mavContainer, sessionStatus);
		
		if (!mavContainer.isResolveView()) {
			return null;
		}
		else {
			ModelAndView mav = new ModelAndView().addAllObjects(mavContainer.getModel());
			mav.setViewName(mavContainer.getViewName());
			if (mavContainer.getView() != null) {
				mav.setView((View) mavContainer.getView());
			}
			return mav;				
		}
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

		return new ServletRequestDataBinderFactory(initBinderMethods, this.webBindingInitializer);
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