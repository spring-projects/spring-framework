/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.handler.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Encapsulates information about a bean method consisting of a
 * {@linkplain #getMethod() method} and a {@linkplain #getBean() bean}. Provides
 * convenient access to method parameters, the method return value, method
 * annotations.
 *
 * <p>The class may be created with a bean instance or with a bean name (e.g. lazy
 * bean, prototype bean). Use {@link #createWithResolvedBean()} to obtain an
 * {@link HandlerMethod} instance with a bean instance initialized through the
 * bean factory.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HandlerMethod {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(HandlerMethod.class);

	private final Object bean;

	private final Method method;

	private final BeanFactory beanFactory;

	private final MethodParameter[] parameters;

	private final Method bridgedMethod;


	/**
	 * Create an instance from a bean instance and a method.
	 */
	public HandlerMethod(Object bean, Method method) {
		Assert.notNull(bean, "bean is required");
		Assert.notNull(method, "method is required");
		this.bean = bean;
		this.beanFactory = null;
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		this.parameters = initMethodParameters();
	}

	/**
	 * Create an instance from a bean instance, method name, and parameter types.
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Assert.notNull(bean, "bean is required");
		Assert.notNull(methodName, "method is required");
		this.bean = bean;
		this.beanFactory = null;
		this.method = bean.getClass().getMethod(methodName, parameterTypes);
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		this.parameters = initMethodParameters();
	}

	/**
	 * Create an instance from a bean name, a method, and a {@code BeanFactory}.
	 * The method {@link #createWithResolvedBean()} may be used later to
	 * re-create the {@code HandlerMethod} with an initialized the bean.
	 */
	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
		Assert.hasText(beanName, "beanName is required");
		Assert.notNull(beanFactory, "beanFactory is required");
		Assert.notNull(method, "method is required");
		Assert.isTrue(beanFactory.containsBean(beanName),
				"Bean factory [" + beanFactory + "] does not contain bean [" + beanName + "]");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		this.parameters = initMethodParameters();
	}

	/**
	 * Copy constructor for use in sub-classes.
	 */
	protected HandlerMethod(HandlerMethod handlerMethod) {
		Assert.notNull(handlerMethod, "HandlerMethod is required");
		this.bean = handlerMethod.bean;
		this.beanFactory = handlerMethod.beanFactory;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
	}

	/**
	 * Re-create HandlerMethod with the resolved handler.
	 */
	private HandlerMethod(HandlerMethod handlerMethod, Object handler) {
		Assert.notNull(handlerMethod, "handlerMethod is required");
		Assert.notNull(handler, "handler is required");
		this.bean = handler;
		this.beanFactory = handlerMethod.beanFactory;
		this.method = handlerMethod.method;
		this.bridgedMethod = handlerMethod.bridgedMethod;
		this.parameters = handlerMethod.parameters;
	}


	private MethodParameter[] initMethodParameters() {
		int count = this.bridgedMethod.getParameterTypes().length;
		MethodParameter[] result = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			result[i] = new HandlerMethodParameter(i);
		}
		return result;
	}

	/**
	 * Returns the bean for this handler method.
	 */
	public Object getBean() {
		return this.bean;
	}

	/**
	 * Returns the method for this handler method.
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Returns the type of the handler for this handler method.
	 * Note that if the bean type is a CGLIB-generated class, the original, user-defined class is returned.
	 */
	public Class<?> getBeanType() {
		Class<?> clazz = (this.bean instanceof String ?
				this.beanFactory.getType((String) this.bean) : this.bean.getClass());
		return ClassUtils.getUserClass(clazz);
	}

	/**
	 * If the bean method is a bridge method, this method returns the bridged (user-defined) method.
	 * Otherwise it returns the same method as {@link #getMethod()}.
	 */
	protected Method getBridgedMethod() {
		return this.bridgedMethod;
	}

	/**
	 * Returns the method parameters for this handler method.
	 */
	public MethodParameter[] getMethodParameters() {
		return this.parameters;
	}

	/**
	 * Return the HandlerMethod return type.
	 */
	public MethodParameter getReturnType() {
		return new HandlerMethodParameter(-1);
	}

	/**
	 * Return the actual return value type.
	 */
	public MethodParameter getReturnValueType(Object returnValue) {
		return new ReturnValueMethodParameter(returnValue);
	}

	/**
	 * Returns {@code true} if the method return type is void, {@code false} otherwise.
	 */
	public boolean isVoid() {
		return Void.TYPE.equals(getReturnType().getParameterType());
	}

	/**
	 * Returns a single annotation on the underlying method traversing its super methods if no
	 * annotation can be found on the given method itself.
	 * @param annotationType the type of annotation to introspect the method for.
	 * @return the annotation, or {@code null} if none found
	 */
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotationUtils.findAnnotation(this.method, annotationType);
	}

	/**
	 * If the provided instance contains a bean name rather than an object instance, the bean name is resolved
	 * before a {@link HandlerMethod} is created and returned.
	 */
	public HandlerMethod createWithResolvedBean() {
		Object handler = this.bean;
		if (this.bean instanceof String) {
			String beanName = (String) this.bean;
			handler = this.beanFactory.getBean(beanName);
		}
		return new HandlerMethod(this, handler);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof HandlerMethod) {
			HandlerMethod other = (HandlerMethod) obj;
			return this.bean.equals(other.bean) && this.method.equals(other.method);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.bean.hashCode() * 31 + this.method.hashCode();
	}

	@Override
	public String toString() {
		return this.method.toGenericString();
	}


	/**
	 * A MethodParameter with HandlerMethod-specific behavior.
	 */
	private class HandlerMethodParameter extends MethodParameter {

		public HandlerMethodParameter(int index) {
			super(HandlerMethod.this.bridgedMethod, index);
		}

		@Override
		public Class<?> getContainingClass() {
			return HandlerMethod.this.getBeanType();
		}

		@Override
		public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.getMethodAnnotation(annotationType);
		}
	}


	/**
	 * A MethodParameter for a HandlerMethod return type based on an actual return value.
	 */
	private class ReturnValueMethodParameter extends HandlerMethodParameter {

		private final Object returnValue;

		public ReturnValueMethodParameter(Object returnValue) {
			super(-1);
			this.returnValue = returnValue;
		}

		@Override
		public Class<?> getParameterType() {
			return (this.returnValue != null ? this.returnValue.getClass() : super.getParameterType());
		}
	}

}
