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
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.ClassUtils;

/**
 * Abstract implementation of {@link TransactionAttributeSource} that caches
 * attributes for methods and implements a merge policy for transaction attributes
 * (see {@link Transactional} annotation) with following priorities (high to low):
 * <ol>
 * <li>specific method;
 * <li>declaring class of the specific method;
 * <li>target class;
 * <li>method in the declaring class/interface;
 * <li>declaring class/interface.
 * </ol>
 *
 * <p>The merge policy means that all transaction attributes which are not
 * explicitly set [1] on a specific definition place (see above) will be inherited
 * from the place with the next lower priority.
 *
 * <p>On the contrary, the previous default {@link AbstractFallbackTransactionAttributeSource} implemented a fallback policy,
 * where all attributes were read from the first found definition place (essentially in the above order), and all others were ignored.
 *
 * <p>See analysis in <a href="https://github.com/spring-projects/spring-framework/issues/24291">Inherited @Transactional methods use wrong TransactionManager</a>.
 *
 * <p>[1] If the value of an attribute is equal to its default value, the current implementation
 * cannot distinguish, whether this value has been set explicitly or implicitly,
 * and considers such attribute as "not explicitly set". Therefore it's currently impossible to override a non-default value with a default value.

 * <p>This implementation caches attributes by method after they are first used.
 * If it is ever desirable to allow dynamic changing of transaction attributes
 * (which is very unlikely), caching could be made configurable. Caching is
 * desirable because of the cost of evaluating rollback rules.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public abstract class AbstractMergeTransactionAttributeSource implements TransactionAttributeSource {

	/**
	 * Canonical value held in cache to indicate no transaction attribute was
	 * found for this method, and we don't need to look again.
	 */
	@SuppressWarnings("serial")
	private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
		@Override
		public String toString() {
			return "null";
		}
	};


	/**
	 * Logger available to subclasses.
	 * <p>As this base class is not marked Serializable, the logger will be recreated
	 * after serialization - provided that the concrete subclass is Serializable.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Cache of TransactionAttributes, keyed by method on a specific target class.
	 * <p>As this base class is not marked Serializable, the cache will be recreated
	 * after serialization - provided that the concrete subclass is Serializable.
	 */
	private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);


	/**
	 * Determine the transaction attribute for this method invocation.
	 * <p>Defaults to the class's transaction attribute if no method attribute is found.
	 * @param method the method for the current invocation (never {@code null})
	 * @param targetClass the target class for this invocation (may be {@code null})
	 * @return a TransactionAttribute for this method, or {@code null} if the method
	 * is not transactional
	 */
	@Override
	@Nullable
	public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		// First, see if we have a cached value.
		Object cacheKey = getCacheKey(method, targetClass);
		TransactionAttribute cached = this.attributeCache.get(cacheKey);
		if (cached != null) {
			// Value will either be canonical value indicating there is no transaction attribute,
			// or an actual transaction attribute.
			if (cached == NULL_TRANSACTION_ATTRIBUTE) {
				return null;
			}
			else {
				return cached;
			}
		}
		else {
			// We need to work it out.
			TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
			// Put it in the cache.
			if (txAttr == null) {
				this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
			}
			else {
				String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
				if (txAttr instanceof DefaultTransactionAttribute) {
					((DefaultTransactionAttribute) txAttr).setDescriptor(methodIdentification);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
				}
				this.attributeCache.put(cacheKey, txAttr);
			}
			return txAttr;
		}
	}

	/**
	 * Determine a cache key for the given method and target class.
	 * <p>Must not produce same key for overloaded methods.
	 * Must produce same key for different instances of the same method.
	 * @param method the method (never {@code null})
	 * @param targetClass the target class (may be {@code null})
	 * @return the cache key (never {@code null})
	 */
	protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	/**
	 * Same signature as {@link #getTransactionAttribute}, but doesn't cache the result.
	 * {@link #getTransactionAttribute} is effectively a caching decorator for this method.
	 * <p>As of 4.1.8, this method can be overridden.
	 * @since 4.1.8
	 * @see #getTransactionAttribute
	 */
	@Nullable
	protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		// Don't allow no-public methods as required.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// The method may be on an interface, but we also need attributes from the target class.
		// If the target class is null, the method will be unchanged.
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

		// 1st priority is the specific method.
		TransactionAttribute txAttr = findTransactionAttribute(specificMethod);

		// 2nd priority is the declaring class of the specific method.
		Class<?> declaringClass = specificMethod.getDeclaringClass();
		boolean userLevelMethod = ClassUtils.isUserLevelMethod(method);
		if (userLevelMethod) {
			txAttr = merge(txAttr, findTransactionAttribute(declaringClass));
		}

		// 3rd priority is the target class
		if (targetClass != null && !targetClass.equals(declaringClass) && userLevelMethod) {
			txAttr = merge(txAttr, findTransactionAttribute(targetClass));
		}

		if (method != specificMethod) {
			// 4th priority is the method in the declaring class/interface.
			txAttr = merge(txAttr, findTransactionAttribute(method));

			// 5th priority is the declaring class/interface.
			txAttr = merge(txAttr, findTransactionAttribute(method.getDeclaringClass()));
		}

		return txAttr;
	}

	/**
	 * Set empty and default properties of "primary" object from "secondary" object.
	 * <p>Parameter objects should not be used after the call to this method,
	 * as they can be changed here or/and returned as a result.
	 */
	@Nullable
	private TransactionAttribute merge(@Nullable TransactionAttribute primaryObj, @Nullable TransactionAttribute secondaryObj) {
		if (primaryObj == null) {
			return secondaryObj;
		}
		if (secondaryObj == null) {
			return primaryObj;
		}

		if (primaryObj instanceof DefaultTransactionAttribute && secondaryObj instanceof DefaultTransactionAttribute) {
			DefaultTransactionAttribute primary = (DefaultTransactionAttribute) primaryObj;
			DefaultTransactionAttribute secondary = (DefaultTransactionAttribute) secondaryObj;

			if (primary.getQualifier() == null || primary.getQualifier().isEmpty()) {
				primary.setQualifier(secondary.getQualifier());
			}
			if (primary.getDescriptor() == null || primary.getDescriptor().isEmpty()) {
				primary.setDescriptor(secondary.getDescriptor());
			}
			if (primary.getName() == null || primary.getName().isEmpty()) {
				primary.setName(secondary.getName());
			}

			// The following properties have default values in DefaultTransactionDefinition;
			// we cannot distinguish here, whether these values have been set explicitly or implicitly;
			// but it seems to be logical to handle default values like empty values.
			if (primary.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED) {
				primary.setPropagationBehavior(secondary.getPropagationBehavior());
			}
			if (primary.getIsolationLevel() == TransactionDefinition.ISOLATION_DEFAULT) {
				primary.setIsolationLevel(secondary.getIsolationLevel());
			}
			if (primary.getTimeout() == TransactionDefinition.TIMEOUT_DEFAULT) {
				primary.setTimeout(secondary.getTimeout());
			}
			if (!primary.isReadOnly()) {
				primary.setReadOnly(secondary.isReadOnly());
			}
		}

		if (primaryObj instanceof RuleBasedTransactionAttribute && secondaryObj instanceof RuleBasedTransactionAttribute) {
			RuleBasedTransactionAttribute primary = (RuleBasedTransactionAttribute) primaryObj;
			RuleBasedTransactionAttribute secondary = (RuleBasedTransactionAttribute) secondaryObj;

			if (primary.getRollbackRules() == null || primary.getRollbackRules().isEmpty()) {
				primary.setRollbackRules(secondary.getRollbackRules());
			}
		}

		return primaryObj;
	}

	/**
	 * Subclasses need to implement this to return the transaction attribute for the
	 * given class, if any.
	 * @param clazz the class to retrieve the attribute for
	 * @return all transaction attribute associated with this class, or {@code null} if none
	 */
	@Nullable
	protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

	/**
	 * Subclasses need to implement this to return the transaction attribute for the
	 * given method, if any.
	 * @param method the method to retrieve the attribute for
	 * @return all transaction attribute associated with this method, or {@code null} if none
	 */
	@Nullable
	protected abstract TransactionAttribute findTransactionAttribute(Method method);

	/**
	 * Should only public methods be allowed to have transactional semantics?
	 * <p>The default implementation returns {@code false}.
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
