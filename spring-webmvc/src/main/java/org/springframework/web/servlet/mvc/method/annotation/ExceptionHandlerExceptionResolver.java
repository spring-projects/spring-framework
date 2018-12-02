/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.method.annotation.MapMethodProcessor;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link AbstractHandlerMethodExceptionResolver} that resolves exceptions
 * through {@code @ExceptionHandler} methods.
 *
 * <p>Support for custom argument and return value types can be added via
 * {@link #setCustomArgumentResolvers} and {@link #setCustomReturnValueHandlers}.
 * Or alternatively to re-configure all argument and return value types use
 * {@link #setArgumentResolvers} and {@link #setReturnValueHandlers(List)}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ExceptionHandlerExceptionResolver extends AbstractHandlerMethodExceptionResolver
		implements ApplicationContextAware, InitializingBean {

	@Nullable
	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	@Nullable
	private HandlerMethodArgumentResolverComposite argumentResolvers;

	@Nullable
	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

	@Nullable
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	private List<HttpMessageConverter<?>> messageConverters;

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private final List<Object> responseBodyAdvice = new ArrayList<>();

	@Nullable
	private ApplicationContext applicationContext;

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);

	private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>();

	public ExceptionHandlerExceptionResolver() {
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);  // see SPR-7316

        // 初始化 messageConverters
		this.messageConverters = new ArrayList<>();
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		this.messageConverters.add(stringHttpMessageConverter);
		try {
			this.messageConverters.add(new SourceHttpMessageConverter<>());
		} catch (Error err) {
			// Ignore when no TransformerFactory implementation is available
		}
		this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
	}

	/**
	 * Provide resolvers for custom argument types. Custom resolvers are ordered
	 * after built-in ones. To override the built-in support for argument
	 * resolution use {@link #setArgumentResolvers} instead.
	 */
	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.customArgumentResolvers= argumentResolvers;
	}

	/**
	 * Return the custom argument resolvers, or {@code null}.
	 */
	@Nullable
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * Configure the complete list of supported argument types thus overriding
	 * the resolvers that would otherwise be configured by default.
	 */
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers = null;
		} else {
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
			this.argumentResolvers.addResolvers(argumentResolvers);
		}
	}

	/**
	 * Return the configured argument resolvers, or possibly {@code null} if
	 * not initialized yet via {@link #afterPropertiesSet()}.
	 */
	@Nullable
	public HandlerMethodArgumentResolverComposite getArgumentResolvers() {
		return this.argumentResolvers;
	}

	/**
	 * Provide handlers for custom return value types. Custom handlers are
	 * ordered after built-in ones. To override the built-in support for
	 * return value handling use {@link #setReturnValueHandlers}.
	 */
	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.customReturnValueHandlers = returnValueHandlers;
	}

	/**
	 * Return the custom return value handlers, or {@code null}.
	 */
	@Nullable
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * Configure the complete list of supported return value types thus
	 * overriding handlers that would otherwise be configured by default.
	 */
	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			this.returnValueHandlers = null;
		} else {
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
			this.returnValueHandlers.addHandlers(returnValueHandlers);
		}
	}

	/**
	 * Return the configured handlers, or possibly {@code null} if not
	 * initialized yet via {@link #afterPropertiesSet()}.
	 */
	@Nullable
	public HandlerMethodReturnValueHandlerComposite getReturnValueHandlers() {
		return this.returnValueHandlers;
	}

	/**
	 * Set the message body converters to use.
	 * <p>These converters are used to convert from and to HTTP requests and responses.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * Return the configured message body converters.
	 */
	public List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Set the {@link ContentNegotiationManager} to use to determine requested media types.
	 * If not set, the default constructor is used.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * Return the configured {@link ContentNegotiationManager}.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * Add one or more components to be invoked after the execution of a controller
	 * method annotated with {@code @ResponseBody} or returning {@code ResponseEntity}
	 * but before the body is written to the response with the selected
	 * {@code HttpMessageConverter}.
	 */
	public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
		this.responseBodyAdvice.clear();
		if (responseBodyAdvice != null) {
			this.responseBodyAdvice.addAll(responseBodyAdvice);
		}
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() {
		// Do this first, it may add ResponseBodyAdvice beans
        // 初始化 exceptionHandlerAdviceCache、responseBodyAdvice
		initExceptionHandlerAdviceCache();

		// 初始化 argumentResolvers 参数
		if (this.argumentResolvers == null) {
			List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
			this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
		}
		// 初始化 returnValueHandlers 参数
		if (this.returnValueHandlers == null) {
			List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
			this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
		}
	}

	private void initExceptionHandlerAdviceCache() {
		if (getApplicationContext() == null) {
			return;
		}

		// 扫描 @ControllerAdvice 注解的 Bean 们，并将进行排序
		List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
		AnnotationAwareOrderComparator.sort(adviceBeans);

		// 遍历 ControllerAdviceBean 数组
		for (ControllerAdviceBean adviceBean : adviceBeans) {
			Class<?> beanType = adviceBean.getBeanType();
			if (beanType == null) {
				throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
			}
			// 扫描该 ControllerAdviceBean 对应的类型
			ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
			// 有 @ExceptionHandler 注解，则添加到 exceptionHandlerAdviceCache 中
			if (resolver.hasExceptionMappings()) {
				this.exceptionHandlerAdviceCache.put(adviceBean, resolver);
			}

			// 如果该 beanType 类型是 ResponseBodyAdvice 子类，则添加到 responseBodyAdvice 中
			if (ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
				this.responseBodyAdvice.add(adviceBean);
			}
		}

		// 打印日志
		if (logger.isDebugEnabled()) {
			int handlerSize = this.exceptionHandlerAdviceCache.size();
			int adviceSize = this.responseBodyAdvice.size();
			if (handlerSize == 0 && adviceSize == 0) {
				logger.debug("ControllerAdvice beans: none");
			} else {
				logger.debug("ControllerAdvice beans: " +
						handlerSize + " @ExceptionHandler, " + adviceSize + " ResponseBodyAdvice");
			}
		}
	}

	/**
	 * Return an unmodifiable Map with the {@link ControllerAdvice @ControllerAdvice}
	 * beans discovered in the ApplicationContext. The returned map will be empty if
	 * the method is invoked before the bean has been initialized via
	 * {@link #afterPropertiesSet()}.
	 */
	public Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> getExceptionHandlerAdviceCache() {
		return Collections.unmodifiableMap(this.exceptionHandlerAdviceCache);
	}

	/**
	 * Return the list of argument resolvers to use including built-in resolvers
	 * and custom resolvers provided via {@link #setCustomArgumentResolvers}.
	 */
	protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Annotation-based argument resolution
		resolvers.add(new SessionAttributeMethodArgumentResolver());
		resolvers.add(new RequestAttributeMethodArgumentResolver());

		// Type-based argument resolution
		resolvers.add(new ServletRequestMethodArgumentResolver());
		resolvers.add(new ServletResponseMethodArgumentResolver());
		resolvers.add(new RedirectAttributesMethodArgumentResolver());
		resolvers.add(new ModelMethodProcessor());

		// Custom arguments
		if (getCustomArgumentResolvers() != null) {
			resolvers.addAll(getCustomArgumentResolvers());
		}

		return resolvers;
	}

	/**
	 * Return the list of return value handlers to use including built-in and
	 * custom handlers provided via {@link #setReturnValueHandlers}.
	 */
	protected List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();

		// Single-purpose return value types
		handlers.add(new ModelAndViewMethodReturnValueHandler());
		handlers.add(new ModelMethodProcessor());
		handlers.add(new ViewMethodReturnValueHandler());
		handlers.add(new HttpEntityMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// Annotation-based return value types
		handlers.add(new ModelAttributeMethodProcessor(false));
		handlers.add(new RequestResponseBodyMethodProcessor(
				getMessageConverters(), this.contentNegotiationManager, this.responseBodyAdvice));

		// Multi-purpose return value types
		handlers.add(new ViewNameMethodReturnValueHandler());
		handlers.add(new MapMethodProcessor());

		// Custom return value types
		if (getCustomReturnValueHandlers() != null) {
			handlers.addAll(getCustomReturnValueHandlers());
		}

		// Catch-all
		handlers.add(new ModelAttributeMethodProcessor(true));

		return handlers;
	}

	/**
	 * Find an {@code @ExceptionHandler} method and invoke it to handle the raised exception.
	 */
	@Override
	@Nullable
	protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request,
			HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception exception) {

	    // 获得异常对应的 ServletInvocableHandlerMethod 对象
		ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception);
		if (exceptionHandlerMethod == null) {
			return null;
		}

		// 设置 ServletInvocableHandlerMethod 对象的相关属性
		if (this.argumentResolvers != null) {
			exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		}
		if (this.returnValueHandlers != null) {
			exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
		}

		// 创建 ServletWebRequest 对象
		ServletWebRequest webRequest = new ServletWebRequest(request, response);
		// 创建 ModelAndViewContainer 对象
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Using @ExceptionHandler " + exceptionHandlerMethod);
			}
			// 执行 ServletInvocableHandlerMethod 的调用
			Throwable cause = exception.getCause();
			if (cause != null) {
				// Expose cause as provided argument as well
				exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, exception, cause, handlerMethod);
			} else {
				// Otherwise, just the given exception as-is
				exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, exception, handlerMethod);
			}
		} catch (Throwable invocationEx) {
		    // 发生异常，则直接返回
			// Any other than the original exception is unintended here,
			// probably an accident (e.g. failed assertion or the like).
			if (invocationEx != exception && logger.isWarnEnabled()) {
				logger.warn("Failure in @ExceptionHandler " + exceptionHandlerMethod, invocationEx);
			}
			// Continue with default processing of the original exception...
			return null;
		}

		// 如果 mavContainer 已处理，则返回“空”的 ModelAndView 对象。
		if (mavContainer.isRequestHandled()) {
			return new ModelAndView();
        // 如果 mavContainer 未处，则基于 `mavContainer` 生成 ModelAndView 对象
		} else {
			ModelMap model = mavContainer.getModel();
			HttpStatus status = mavContainer.getStatus();
			// 创建 ModelAndView 对象，并设置相关属性
			ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model, status);
			mav.setViewName(mavContainer.getViewName());
			if (!mavContainer.isViewReference()) {
				mav.setView((View) mavContainer.getView());
			}
			// TODO 1004 flashMapManager
			if (model instanceof RedirectAttributes) {
				Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
				RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
			}
			return mav;
		}
	}

	/**
	 * Find an {@code @ExceptionHandler} method for the given exception. The default
	 * implementation searches methods in the class hierarchy of the controller first
	 * and if not found, it continues searching for additional {@code @ExceptionHandler}
	 * methods assuming some {@linkplain ControllerAdvice @ControllerAdvice}
	 * Spring-managed beans were detected.
	 * @param handlerMethod the method where the exception was raised (may be {@code null})
	 * @param exception the raised exception
	 * @return a method to handle the exception, or {@code null} if none
	 */
	@Nullable
	protected ServletInvocableHandlerMethod getExceptionHandlerMethod(
			@Nullable HandlerMethod handlerMethod, Exception exception) {
	    // 处理器的类型
		Class<?> handlerType = null;

		// 首先，如果 handlerMethod 非空，则先获得 Controller 对应的 @ExceptionHandler 处理器对应的方法
		if (handlerMethod != null) {
			// Local exception handler methods on the controller class itself.
			// To be invoked through the proxy, even in case of an interface-based proxy.
            // 获得 handlerType
			handlerType = handlerMethod.getBeanType();
			// 获得 handlerType 对应的 ExceptionHandlerMethodResolver 对象
			ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(handlerType);
			if (resolver == null) {
				resolver = new ExceptionHandlerMethodResolver(handlerType);
				this.exceptionHandlerCache.put(handlerType, resolver);
			}
			// 获得异常对应的 Method 方法
			Method method = resolver.resolveMethod(exception);
			// 如果获得到 Method 方法，则创建 ServletInvocableHandlerMethod 对象，并返回
			if (method != null) {
				return new ServletInvocableHandlerMethod(handlerMethod.getBean(), method);
			}
			// For advice applicability check below (involving base packages, assignable types
			// and annotation presence), use target class instead of interface-based proxy.
            // 获得 handlerType 的原始类。因为，此处有可能是代理对象
			if (Proxy.isProxyClass(handlerType)) {
				handlerType = AopUtils.getTargetClass(handlerMethod.getBean());
			}
		}

		// 其次，使用 ControllerAdvice 对应的 @ExceptionHandler 处理器对应的方法
		for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
			ControllerAdviceBean advice = entry.getKey();
			// 如果 ControllerAdvice 支持当前的 handlerType
			if (advice.isApplicableToBeanType(handlerType)) {
                // 获得 handlerType 对应的 ExceptionHandlerMethodResolver 对象
				ExceptionHandlerMethodResolver resolver = entry.getValue();
                // 获得异常对应的 Method 方法
                Method method = resolver.resolveMethod(exception);
                // 如果获得到 Method 方法，则创建 ServletInvocableHandlerMethod 对象，并返回
                if (method != null) {
					return new ServletInvocableHandlerMethod(advice.resolveBean(), method);
				}
			}
		}

		// 最差，获取不到
		return null;
	}

}
