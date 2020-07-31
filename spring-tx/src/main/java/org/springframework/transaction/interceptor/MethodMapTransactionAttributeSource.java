/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringValueResolver;

/**
 * Simple {@link TransactionAttributeSource} implementation that
 * allows attributes to be stored per method in a {@link Map}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 24.04.2003
 * @see #isMatch
 * @see NameMatchTransactionAttributeSource
 */
public class MethodMapTransactionAttributeSource
		implements TransactionAttributeSource, EmbeddedValueResolverAware, BeanClassLoaderAware, InitializingBean {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Map from method name to attribute value. */
	@Nullable
	private Map<String, TransactionAttribute> methodMap;

	@Nullable
	private StringValueResolver embeddedValueResolver;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean eagerlyInitialized = false;

	private boolean initialized = false;

	/** Map from Method to TransactionAttribute. */
	private final Map<Method, TransactionAttribute> transactionAttributeMap = new HashMap<>();

	/** Map from Method to name pattern used for registration. */
	private final Map<Method, String> methodNameMap = new HashMap<>();


	/**
	 * Set a name/attribute map, consisting of "FQCN.method" method names
	 * (e.g. "com.mycompany.mycode.MyClass.myMethod") and
	 * {@link TransactionAttribute} instances (or Strings to be converted
	 * to {@code TransactionAttribute} instances).
	 * <p>Intended for configuration via setter injection, typically within
	 * a Spring bean factory. Relies on {@link #afterPropertiesSet()}
	 * being called afterwards.
	 * @param methodMap said {@link Map} from method name to attribute value
	 * @see TransactionAttribute
	 * @see TransactionAttributeEditor
	 */
	public void setMethodMap(Map<String, TransactionAttribute> methodMap) {
		this.methodMap = methodMap;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * Eagerly initializes the specified
	 * {@link #setMethodMap(java.util.Map) "methodMap"}, if any.
	 * @see #initMethodMap(java.util.Map)
	 */
	@Override
	public void afterPropertiesSet() {
		initMethodMap(this.methodMap);
		this.eagerlyInitialized = true;
		this.initialized = true;
	}

	/**
	 * Initialize the specified {@link #setMethodMap(java.util.Map) "methodMap"}, if any.
	 * @param methodMap a Map from method names to {@code TransactionAttribute} instances
	 * @see #setMethodMap
	 */
	protected void initMethodMap(@Nullable Map<String, TransactionAttribute> methodMap) {
		if (methodMap != null) {
			methodMap.forEach(this::addTransactionalMethod);
		}
	}


	/**
	 * Add an attribute for a transactional method.
	 * <p>Method names can end or start with "*" for matching multiple methods.
	 * @param name class and method name, separated by a dot
	 * @param attr attribute associated with the method
	 * @throws IllegalArgumentException in case of an invalid name
	 */
	public void addTransactionalMethod(String name, TransactionAttribute attr) {
		Assert.notNull(name, "Name must not be null");
		int lastDotIndex = name.lastIndexOf('.');
		if (lastDotIndex == -1) {
			throw new IllegalArgumentException("'" + name + "' is not a valid method name: format is FQN.methodName");
		}
		String className = name.substring(0, lastDotIndex);
		String methodName = name.substring(lastDotIndex + 1);
		Class<?> clazz = ClassUtils.resolveClassName(className, this.beanClassLoader);
		addTransactionalMethod(clazz, methodName, attr);
	}

	/**
	 * Add an attribute for a transactional method.
	 * Method names can end or start with "*" for matching multiple methods.
	 * @param clazz target interface or class
	 * @param mappedName mapped method name
	 * @param attr attribute associated with the method
	 */
	public void addTransactionalMethod(Class<?> clazz, String mappedName, TransactionAttribute attr) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(mappedName, "Mapped name must not be null");
		String name = clazz.getName() + '.'  + mappedName;

		Method[] methods = clazz.getDeclaredMethods();
		List<Method> matchingMethods = new ArrayList<>();
		for (Method method : methods) {
			if (isMatch(method.getName(), mappedName)) {
				matchingMethods.add(method);
			}
		}
		if (matchingMethods.isEmpty()) {
			throw new IllegalArgumentException(
					"Could not find method '" + mappedName + "' on class [" + clazz.getName() + "]");
		}

		// Register all matching methods
		for (Method method : matchingMethods) {
			String regMethodName = this.methodNameMap.get(method);
			if (regMethodName == null || (!regMethodName.equals(name) && regMethodName.length() <= name.length())) {
				// No already registered method name, or more specific
				// method name specification now -> (re-)register method.
				if (logger.isDebugEnabled() && regMethodName != null) {
					logger.debug("Replacing attribute for transactional method [" + method + "]: current name '" +
							name + "' is more specific than '" + regMethodName + "'");
				}
				this.methodNameMap.put(method, name);
				addTransactionalMethod(method, attr);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Keeping attribute for transactional method [" + method + "]: current name '" +
							name + "' is not more specific than '" + regMethodName + "'");
				}
			}
		}
	}

	/**
	 * Add an attribute for a transactional method.
	 * @param method the method
	 * @param attr attribute associated with the method
	 */
	public void addTransactionalMethod(Method method, TransactionAttribute attr) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(attr, "TransactionAttribute must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Adding transactional method [" + method + "] with attribute [" + attr + "]");
		}
		if (this.embeddedValueResolver != null && attr instanceof DefaultTransactionAttribute) {
			((DefaultTransactionAttribute) attr).resolveAttributeStrings(this.embeddedValueResolver);
		}
		this.transactionAttributeMap.put(method, attr);
	}

	/**
	 * Return if the given method name matches the mapped name.
	 * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*"
	 * matches, as well as direct equality.
	 * @param methodName the method name of the class
	 * @param mappedName the name in the descriptor
	 * @return if the names match
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	@Nullable
	public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		if (this.eagerlyInitialized) {
			return this.transactionAttributeMap.get(method);
		}
		else {
			synchronized (this.transactionAttributeMap) {
				if (!this.initialized) {
					initMethodMap(this.methodMap);
					this.initialized = true;
				}
				return this.transactionAttributeMap.get(method);
			}
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodMapTransactionAttributeSource)) {
			return false;
		}
		MethodMapTransactionAttributeSource otherTas = (MethodMapTransactionAttributeSource) other;
		return ObjectUtils.nullSafeEquals(this.methodMap, otherTas.methodMap);
	}

	@Override
	public int hashCode() {
		return MethodMapTransactionAttributeSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.methodMap;
	}

}
