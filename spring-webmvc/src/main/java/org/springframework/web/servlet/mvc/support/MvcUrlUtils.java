/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import java.lang.reflect.Method;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.util.UriComponents;

/**
 * Utility methods to support the creation URLs to Spring MVC controllers and controller
 * methods.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MvcUrlUtils {

	private static Log logger = LogFactory.getLog(MvcUrlUtils.class);

	private final static ObjenesisStd OBJENESIS = new ObjenesisStd(true);


	/**
	 * Extract the type-level URL mapping or return an empty String. If multiple mappings
	 * are found, the first one is used.
	 */
	public static String getTypeLevelMapping(Class<?> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		RequestMapping annot = AnnotationUtils.findAnnotation(controllerType, RequestMapping.class);
		if ((annot == null) || ObjectUtils.isEmpty(annot.value())) {
			return "/";
		}
		if (annot.value().length > 1) {
			logger.warn("Multiple class level mappings on " + controllerType.getName() + ", using the first one");
		}
		return annot.value()[0];
	}

	/**
	 * Extract the mapping from the given controller method, including both type and
	 * method-level mappings. If multiple mappings are found, the first one is used.
	 */
	public static String getMethodMapping(Method method) {
		RequestMapping methodAnnot = AnnotationUtils.findAnnotation(method, RequestMapping.class);
		Assert.notNull(methodAnnot, "No mappings on " + method.toGenericString());
		PatternsRequestCondition condition = new PatternsRequestCondition(methodAnnot.value());

		RequestMapping typeAnnot = AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequestMapping.class);
		if (typeAnnot != null) {
			condition = new PatternsRequestCondition(typeAnnot.value()).combine(condition);
		}

		Set<String> patterns = condition.getPatterns();
		if (patterns.size() > 1) {
			logger.warn("Multiple mappings on " + method.toGenericString() + ", using the first one");
		}

		return (patterns.size() == 0) ? "/" : patterns.iterator().next();
	}

	/**
	 * Return a "mock" controller instance. When a controller method is invoked, the
	 * invoked method and argument values are remembered, and a "mock" value is returned
	 * so it can be used to help prepare a {@link UriComponents} through
	 * {@link MvcUrls#linkToMethodOn(Object)}.
	 * @param controllerType the type of controller to mock, must not be {@literal null}.
	 * @param typeLevelUriVariables URI variables to expand into the type-level mapping
	 * @return the created controller instance
	 */
	public static <T> T controller(Class<T> controllerType, Object... typeLevelUriVariables) {
		Assert.notNull(controllerType, "'type' must not be null");
		return initProxy(controllerType, new ControllerMethodInvocationInterceptor(typeLevelUriVariables));
	}

	@SuppressWarnings("unchecked")
	private static <T> T initProxy(Class<?> type, ControllerMethodInvocationInterceptor interceptor) {
		if (type.isInterface()) {
			ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
			factory.addInterface(type);
			factory.addInterface(ControllerMethodValues.class);
			factory.addAdvice(interceptor);
			return (T) factory.getProxy();
		}
		else {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(type);
			enhancer.setInterfaces(new Class<?>[] { ControllerMethodValues.class });
			enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

			Factory factory = (Factory) OBJENESIS.newInstance(enhancer.createClass());
			factory.setCallbacks(new Callback[] { interceptor });
			return (T) factory;
		}
	}


	private static class ControllerMethodInvocationInterceptor
			implements org.springframework.cglib.proxy.MethodInterceptor, MethodInterceptor {

		private static final Method getTypeLevelUriVariables =
				ReflectionUtils.findMethod(ControllerMethodValues.class, "getTypeLevelUriVariables");

		private static final Method getControllerMethod =
				ReflectionUtils.findMethod(ControllerMethodValues.class, "getControllerMethod");

		private static final Method getArgumentValues =
				ReflectionUtils.findMethod(ControllerMethodValues.class, "getArgumentValues");


		private final Object[] typeLevelUriVariables;

		private Method controllerMethod;

		private Object[] argumentValues;


		public ControllerMethodInvocationInterceptor(Object... typeLevelUriVariables) {
			this.typeLevelUriVariables = typeLevelUriVariables.clone();
		}


		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {

			if (getTypeLevelUriVariables.equals(method)) {
				return this.typeLevelUriVariables;
			}
			else if (getControllerMethod.equals(method)) {
				return this.controllerMethod;
			}
			else if (getArgumentValues.equals(method)) {
				return this.argumentValues;
			}
			else if (ReflectionUtils.isObjectMethod(method)) {
				return ReflectionUtils.invokeMethod(method, obj, args);
			}
			else {
				this.controllerMethod = method;
				this.argumentValues = args;

				Class<?> returnType = method.getReturnType();
				return void.class.equals(returnType) ? null : returnType.cast(initProxy(returnType, this));
			}
		}

		@Override
		public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
			return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
		}
	}

	/**
	 * Provides information about a controller method that can be used to prepare a URL
	 * including type-level URI template variables, a method reference, and argument
	 * values collected through the invocation of a "mock" controller.
	 * <p>
	 * Instances of this interface are returned from
	 * {@link MvcUrlUtils#controller(Class, Object...) controller(Class, Object...)} and
	 * are needed for {@link MvcUrls#linkToMethodOn(Object)}.
	 */
	public interface ControllerMethodValues {

		Object[] getTypeLevelUriVariables();

		Method getControllerMethod();

		Object[] getArgumentValues();

	}

}
