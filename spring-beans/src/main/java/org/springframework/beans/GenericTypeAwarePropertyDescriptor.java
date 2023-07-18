/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Extension of the standard JavaBeans {@link PropertyDescriptor} class,
 * overriding {@code getPropertyType()} such that a generically declared
 * type variable will be resolved against the containing bean class.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
final class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {

	private final Class<?> beanClass;

	@Nullable
	private final Method readMethod;

	@Nullable
	private final Method writeMethod;

	@Nullable
	private volatile Set<Method> ambiguousWriteMethods;

	@Nullable
	private MethodParameter writeMethodParameter;

	@Nullable
	private Class<?> propertyType;

	@Nullable
	private final Class<?> propertyEditorClass;


	public GenericTypeAwarePropertyDescriptor(Class<?> beanClass, String propertyName,
			@Nullable Method readMethod, @Nullable Method writeMethod,
			@Nullable Class<?> propertyEditorClass) throws IntrospectionException {

		super(propertyName, null, null);
		this.beanClass = beanClass;

		Method readMethodToUse = (readMethod != null ? BridgeMethodResolver.findBridgedMethod(readMethod) : null);
		Method writeMethodToUse = (writeMethod != null ? BridgeMethodResolver.findBridgedMethod(writeMethod) : null);
		if (writeMethodToUse == null && readMethodToUse != null) {
			// Fallback: Original JavaBeans introspection might not have found matching setter
			// method due to lack of bridge method resolution, in case of the getter using a
			// covariant return type whereas the setter is defined for the concrete property type.
			Method candidate = ClassUtils.getMethodIfAvailable(
					this.beanClass, "set" + StringUtils.capitalize(getName()), (Class<?>[]) null);
			if (candidate != null && candidate.getParameterCount() == 1) {
				writeMethodToUse = candidate;
			}
		}
		this.readMethod = readMethodToUse;
		this.writeMethod = writeMethodToUse;

		if (this.writeMethod != null) {
			if (this.readMethod == null) {
				// Write method not matched against read method: potentially ambiguous through
				// several overloaded variants, in which case an arbitrary winner has been chosen
				// by the JDK's JavaBeans Introspector...
				Set<Method> ambiguousCandidates = new HashSet<>();
				for (Method method : beanClass.getMethods()) {
					if (method.getName().equals(writeMethodToUse.getName()) &&
							!method.equals(writeMethodToUse) && !method.isBridge() &&
							method.getParameterCount() == writeMethodToUse.getParameterCount()) {
						ambiguousCandidates.add(method);
					}
				}
				if (!ambiguousCandidates.isEmpty()) {
					this.ambiguousWriteMethods = ambiguousCandidates;
				}
			}
			this.writeMethodParameter = new MethodParameter(this.writeMethod, 0).withContainingClass(this.beanClass);
		}

		if (this.readMethod != null) {
			this.propertyType = GenericTypeResolver.resolveReturnType(this.readMethod, this.beanClass);
		}
		else if (this.writeMethodParameter != null) {
			this.propertyType = this.writeMethodParameter.getParameterType();
		}

		this.propertyEditorClass = propertyEditorClass;
	}


	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	@Override
	@Nullable
	public Method getReadMethod() {
		return this.readMethod;
	}

	@Override
	@Nullable
	public Method getWriteMethod() {
		return this.writeMethod;
	}

	public Method getWriteMethodForActualAccess() {
		Assert.state(this.writeMethod != null, "No write method available");
		Set<Method> ambiguousCandidates = this.ambiguousWriteMethods;
		if (ambiguousCandidates != null) {
			this.ambiguousWriteMethods = null;
			LogFactory.getLog(GenericTypeAwarePropertyDescriptor.class).debug("Non-unique JavaBean property '" +
					getName() + "' being accessed! Ambiguous write methods found next to actually used [" +
					this.writeMethod + "]: " + ambiguousCandidates);
		}
		return this.writeMethod;
	}

	public MethodParameter getWriteMethodParameter() {
		Assert.state(this.writeMethodParameter != null, "No write method available");
		return this.writeMethodParameter;
	}

	@Override
	@Nullable
	public Class<?> getPropertyType() {
		return this.propertyType;
	}

	@Override
	@Nullable
	public Class<?> getPropertyEditorClass() {
		return this.propertyEditorClass;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof GenericTypeAwarePropertyDescriptor that &&
				getBeanClass().equals(that.getBeanClass()) &&
				PropertyDescriptorUtils.equals(this, that)));
	}

	@Override
	public int hashCode() {
		int hashCode = getBeanClass().hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getReadMethod());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getWriteMethod());
		return hashCode;
	}

}
