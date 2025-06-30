/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.util.ObjectUtils;

/**
 * Represents an override of a method that looks up an object in the same IoC context,
 * either by bean name or by bean type (based on the declared method return type).
 *
 * <p>Methods eligible for lookup override may declare arguments in which case the
 * given arguments are passed to the bean retrieval operation.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.beans.factory.BeanFactory#getBean(String)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBeanProvider(ResolvableType)
 */
public class LookupOverride extends MethodOverride {

	private final @Nullable String beanName;

	private @Nullable Method method;


	/**
	 * Construct a new {@code LookupOverride}.
	 * @param methodName the name of the method to override
	 * @param beanName the name of the bean in the current {@code BeanFactory} that the
	 * overridden method should return (may be {@code null} for type-based bean retrieval)
	 */
	public LookupOverride(String methodName, @Nullable String beanName) {
		super(methodName);
		this.beanName = beanName;
	}

	/**
	 * Construct a new {@code LookupOverride}.
	 * @param method the method declaration to override
	 * @param beanName the name of the bean in the current {@code BeanFactory} that the
	 * overridden method should return (may be {@code null} for type-based bean retrieval)
	 */
	public LookupOverride(Method method, @Nullable String beanName) {
		super(method.getName());
		this.method = method;
		this.beanName = beanName;
	}


	/**
	 * Return the name of the bean that should be returned by this {@code LookupOverride}.
	 */
	public @Nullable String getBeanName() {
		return this.beanName;
	}

	/**
	 * Match the specified method by {@link Method} reference or method name.
	 * <p>For backwards compatibility reasons, in a scenario with overloaded
	 * non-abstract methods of the given name, only the no-arg variant of a
	 * method will be turned into a container-driven lookup method.
	 * <p>In case of a provided {@link Method}, only straight matches will
	 * be considered, usually demarcated by the {@code @Lookup} annotation.
	 */
	@Override
	public boolean matches(Method method) {
		if (this.method != null) {
			return method.equals(this.method);
		}
		else {
			return (method.getName().equals(getMethodName()) && (!isOverloaded() ||
					Modifier.isAbstract(method.getModifiers()) || method.getParameterCount() == 0));
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (other instanceof LookupOverride that && super.equals(other) &&
				ObjectUtils.nullSafeEquals(this.method, that.method) &&
				ObjectUtils.nullSafeEquals(this.beanName, that.beanName));
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.beanName);
	}

	@Override
	public String toString() {
		return "LookupOverride for method '" + getMethodName() + "'";
	}

}
