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

package org.springframework.web.method;

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
 * Encapsulates information about a bean method consisting of a {@linkplain #getMethod() method} and a 
 * {@linkplain #getBean() bean}. Provides convenient access to method parameters, the method return value, 
 * method annotations.
 *
 * <p>The class may be created with a bean instance or with a bean name (e.g. lazy bean, prototype bean).
 * Use {@link #createWithResolvedBean()} to obtain an {@link HandlerMethod} instance with a bean instance
 * initialized through the bean factory.  
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HandlerMethod {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final Object bean;

	private final Method method;
	
	private final BeanFactory beanFactory;

	private MethodParameter[] parameters;

	private final Method bridgedMethod;

	/**
	 * Constructs a new handler method with the given bean instance and method.
	 * @param bean the object bean
	 * @param method the method
	 */
	public HandlerMethod(Object bean, Method method) {
		Assert.notNull(bean, "bean must not be null");
		Assert.notNull(method, "method must not be null");
		this.bean = bean;
		this.beanFactory = null;
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
	}

	/**
	 * Constructs a new handler method with the given bean instance, method name and parameters.
	 * @param bean the object bean
	 * @param methodName the method name
	 * @param parameterTypes the method parameter types
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Assert.notNull(bean, "bean must not be null");
		Assert.notNull(methodName, "method must not be null");
		this.bean = bean;
		this.beanFactory = null;
		this.method = bean.getClass().getMethod(methodName, parameterTypes);
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
	}

	/**
	 * Constructs a new handler method with the given bean name and method. The bean name will be lazily 
	 * initialized when {@link #createWithResolvedBean()} is called.
	 * @param beanName the bean name
	 * @param beanFactory the bean factory to use for bean initialization
	 * @param method the method for the bean
	 */
	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
		Assert.hasText(beanName, "'beanName' must not be null");
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.notNull(method, "'method' must not be null");
		Assert.isTrue(beanFactory.containsBean(beanName),
				"Bean factory [" + beanFactory + "] does not contain bean " + "with name [" + beanName + "]");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		this.method = method;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
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
		if (bean instanceof String) {
			String beanName = (String) bean;
			return beanFactory.getType(beanName);
		}
		else {
			return ClassUtils.getUserClass(bean.getClass());
		}
	}
	
	/**
	 * If the bean method is a bridge method, this method returns the bridged (user-defined) method.
	 * Otherwise it returns the same method as {@link #getMethod()}.
	 */
	protected Method getBridgedMethod() {
		return bridgedMethod;
	}

	/**
	 * Returns the method parameters for this handler method.
	 */
	public MethodParameter[] getMethodParameters() {
		if (this.parameters == null) {
			int parameterCount = this.bridgedMethod.getParameterTypes().length;
			MethodParameter[] p = new MethodParameter[parameterCount];
			for (int i = 0; i < parameterCount; i++) {
				p[i] = new HandlerMethodParameter(this.bridgedMethod, i);
			}
			this.parameters = p;
		}
		return parameters;
	}

	/**
	 * Returns the method return type, as {@code MethodParameter}.
	 */
	public MethodParameter getReturnType() {
		return new HandlerMethodParameter(this.bridgedMethod, -1);
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
		return new HandlerMethod(handler, method);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o != null && o instanceof HandlerMethod) {
			HandlerMethod other = (HandlerMethod) o;
			return this.bean.equals(other.bean) && this.method.equals(other.method);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * this.bean.hashCode() + this.method.hashCode();
	}

	@Override
	public String toString() {
		return method.toGenericString();
	}

	/**
	 * A {@link MethodParameter} that resolves method annotations even when the actual annotations
	 * are on a bridge method rather than on the current method. Annotations on super types are
	 * also returned via {@link AnnotationUtils#findAnnotation(Method, Class)}. 
	 */
	private class HandlerMethodParameter extends MethodParameter {
		
		public HandlerMethodParameter(Method method, int parameterIndex) {
			super(method, parameterIndex);
		}

		/**
		 * Return {@link HandlerMethod#getBeanType()} rather than the method's class, which could be
		 * important for the proper discovery of generic types.
		 */
		@Override
		public Class<?> getDeclaringClass() {
			return HandlerMethod.this.getBeanType();
		}

		/**
		 * Return the method annotation via {@link HandlerMethod#getMethodAnnotation(Class)}, which will find
		 * the annotation by traversing super-types and handling annotations on bridge methods correctly.
		 */
		@Override
		public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
			return HandlerMethod.this.getMethodAnnotation(annotationType);
		}
	}

}
