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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Extension of {@link MethodOverride} that represents an arbitrary
 * override of a method by the IoC container.
 *
 * <p>Any non-final method can be overridden, irrespective of its
 * parameters and return types.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public class ReplaceOverride extends MethodOverride {

	private final String methodReplacerBeanName;

	private final List<String> typeIdentifiers = new ArrayList<>();


	/**
	 * Construct a new ReplaceOverride.
	 * @param methodName the name of the method to override
	 * @param methodReplacerBeanName the bean name of the {@link MethodReplacer}
	 */
	public ReplaceOverride(String methodName, String methodReplacerBeanName) {
		super(methodName);
		Assert.notNull(methodReplacerBeanName, "Method replacer bean name must not be null");
		this.methodReplacerBeanName = methodReplacerBeanName;
	}

	/**
	 * Construct a new ReplaceOverride.
	 * @param methodName the name of the method to override
	 * @param methodReplacerBeanName the bean name of the {@link MethodReplacer}
	 * @param typeIdentifiers a list of type identifiers for parameter types
	 * @since 6.2.9
	 */
	public ReplaceOverride(String methodName, String methodReplacerBeanName, List<String> typeIdentifiers) {
		super(methodName);
		Assert.notNull(methodReplacerBeanName, "Method replacer bean name must not be null");
		this.methodReplacerBeanName = methodReplacerBeanName;
		this.typeIdentifiers.addAll(typeIdentifiers);
	}


	/**
	 * Return the name of the bean implementing MethodReplacer.
	 */
	public String getMethodReplacerBeanName() {
		return this.methodReplacerBeanName;
	}

	/**
	 * Add a fragment of a class string, like "Exception"
	 * or "java.lang.Exc", to identify a parameter type.
	 * @param identifier a substring of the fully qualified class name
	 */
	public void addTypeIdentifier(String identifier) {
		this.typeIdentifiers.add(identifier);
	}

	/**
	 * Return the list of registered type identifiers (fragments of a class string).
	 * @since 6.2.9
	 * @see #addTypeIdentifier
	 */
	public List<String> getTypeIdentifiers() {
		return Collections.unmodifiableList(this.typeIdentifiers);
	}


	@Override
	public boolean matches(Method method) {
		if (!method.getName().equals(getMethodName())) {
			return false;
		}
		if (!isOverloaded()) {
			// Not overloaded: don't worry about arg type matching...
			return true;
		}
		// If we get here, we need to insist on precise argument matching...
		if (this.typeIdentifiers.size() != method.getParameterCount()) {
			return false;
		}
		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < this.typeIdentifiers.size(); i++) {
			String identifier = this.typeIdentifiers.get(i);
			if (!parameterTypes[i].getName().contains(identifier)) {
				return false;
			}
		}
		return true;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (other instanceof ReplaceOverride that && super.equals(that) &&
				this.methodReplacerBeanName.equals(that.methodReplacerBeanName) &&
				this.typeIdentifiers.equals(that.typeIdentifiers));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.methodReplacerBeanName, this.typeIdentifiers);
	}

	@Override
	public String toString() {
		return "ReplaceOverride for method '" + getMethodName() + "'";
	}

}
