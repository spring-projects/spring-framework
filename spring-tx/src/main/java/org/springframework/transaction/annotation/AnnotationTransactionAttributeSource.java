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

package org.springframework.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of the
 * {@link org.springframework.transaction.interceptor.TransactionAttributeSource}
 * interface for working with transaction metadata from annotations.
 *
 * <p>This class reads Spring's {@link Transactional @Transactional} annotation and
 * exposes corresponding transaction attributes to Spring's transaction infrastructure.
 * Also supports JTA's {@link jakarta.transaction.Transactional} and EJB's
 * {@link jakarta.ejb.TransactionAttribute} annotation (if present).
 *
 * <p>This class may also serve as base class for a custom TransactionAttributeSource,
 * or get customized through {@link TransactionAnnotationParser} strategies.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.2
 * @see Transactional
 * @see TransactionAnnotationParser
 * @see SpringTransactionAnnotationParser
 * @see Ejb3TransactionAnnotationParser
 * @see org.springframework.transaction.interceptor.TransactionInterceptor#setTransactionAttributeSource
 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean#setTransactionAttributeSource
 */
@SuppressWarnings("serial")
public class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
		implements Serializable {

	private static final boolean jtaPresent;

	private static final boolean ejb3Present;

	static {
		ClassLoader classLoader = AnnotationTransactionAttributeSource.class.getClassLoader();
		jtaPresent = ClassUtils.isPresent("jakarta.transaction.Transactional", classLoader);
		ejb3Present = ClassUtils.isPresent("jakarta.ejb.TransactionAttribute", classLoader);
	}

	private final Set<TransactionAnnotationParser> annotationParsers;

	private boolean publicMethodsOnly = true;

	private @Nullable Set<RollbackRuleAttribute> defaultRollbackRules;


	/**
	 * Create a default AnnotationTransactionAttributeSource, supporting
	 * public methods that carry the {@code Transactional} annotation
	 * or the EJB3 {@link jakarta.ejb.TransactionAttribute} annotation.
	 */
	public AnnotationTransactionAttributeSource() {
		if (jtaPresent || ejb3Present) {
			this.annotationParsers = CollectionUtils.newLinkedHashSet(3);
			this.annotationParsers.add(new SpringTransactionAnnotationParser());
			if (jtaPresent) {
				this.annotationParsers.add(new JtaTransactionAnnotationParser());
			}
			if (ejb3Present) {
				this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
			}
		}
		else {
			this.annotationParsers = Collections.singleton(new SpringTransactionAnnotationParser());
		}
	}

	/**
	 * Create a custom AnnotationTransactionAttributeSource, supporting
	 * public methods that carry the {@code Transactional} annotation
	 * or the EJB3 {@link jakarta.ejb.TransactionAttribute} annotation.
	 * @param publicMethodsOnly whether to support public methods that carry
	 * the {@code Transactional} annotation only (typically for use
	 * with proxy-based AOP), or protected/private methods as well
	 * (typically used with AspectJ class weaving)
	 * @see #setPublicMethodsOnly
	 */
	public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
		this();
		this.publicMethodsOnly = publicMethodsOnly;
	}

	/**
	 * Create a custom AnnotationTransactionAttributeSource.
	 * @param annotationParser the TransactionAnnotationParser to use
	 */
	public AnnotationTransactionAttributeSource(TransactionAnnotationParser annotationParser) {
		Assert.notNull(annotationParser, "TransactionAnnotationParser must not be null");
		this.annotationParsers = Collections.singleton(annotationParser);
	}

	/**
	 * Create a custom AnnotationTransactionAttributeSource.
	 * @param annotationParsers the TransactionAnnotationParsers to use
	 */
	public AnnotationTransactionAttributeSource(TransactionAnnotationParser... annotationParsers) {
		Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
		this.annotationParsers = Set.of(annotationParsers);
	}


	/**
	 * Set whether transactional methods are expected to be public.
	 * <p>The default is {@code true}.
	 * @since 6.2
	 * @see #AnnotationTransactionAttributeSource(boolean)
	 */
	public void setPublicMethodsOnly(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
	}

	/**
	 * Add a default rollback rule, to be applied to all rule-based
	 * transaction attributes returned by this source.
	 * <p>By default, a rollback will be triggered on unchecked exceptions
	 * but not on checked exceptions. A default rule may override this
	 * while still respecting any custom rules in the transaction attribute.
	 * @param rollbackRule a rollback rule overriding the default behavior,
	 * for example, {@link RollbackRuleAttribute#ROLLBACK_ON_ALL_EXCEPTIONS}
	 * @since 6.2
	 * @see RuleBasedTransactionAttribute#getRollbackRules()
	 * @see EnableTransactionManagement#rollbackOn()
	 * @see Transactional#rollbackFor()
	 * @see Transactional#noRollbackFor()
	 */
	public void addDefaultRollbackRule(RollbackRuleAttribute rollbackRule) {
		if (this.defaultRollbackRules == null) {
			this.defaultRollbackRules = new LinkedHashSet<>();
		}
		this.defaultRollbackRules.add(rollbackRule);
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (TransactionAnnotationParser parser : this.annotationParsers) {
			if (parser.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected @Nullable TransactionAttribute findTransactionAttribute(Class<?> clazz) {
		return determineTransactionAttribute(clazz);
	}

	@Override
	protected @Nullable TransactionAttribute findTransactionAttribute(Method method) {
		return determineTransactionAttribute(method);
	}

	/**
	 * Determine the transaction attribute for the given method or class.
	 * <p>This implementation delegates to configured
	 * {@link TransactionAnnotationParser TransactionAnnotationParsers}
	 * for parsing known annotations into Spring's metadata attribute class.
	 * Returns {@code null} if it's not transactional.
	 * <p>Can be overridden to support custom annotations that carry transaction metadata.
	 * @param element the annotated method or class
	 * @return the configured transaction attribute, or {@code null} if none was found
	 */
	protected @Nullable TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
		for (TransactionAnnotationParser parser : this.annotationParsers) {
			TransactionAttribute attr = parser.parseTransactionAnnotation(element);
			if (attr != null) {
				if (this.defaultRollbackRules != null && attr instanceof RuleBasedTransactionAttribute ruleAttr) {
					ruleAttr.getRollbackRules().addAll(this.defaultRollbackRules);
				}
				return attr;
			}
		}
		return null;
	}

	/**
	 * By default, only public methods can be made transactional.
	 * @see #setPublicMethodsOnly
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof AnnotationTransactionAttributeSource otherTas &&
				this.annotationParsers.equals(otherTas.annotationParsers) &&
				this.publicMethodsOnly == otherTas.publicMethodsOnly));
	}

	@Override
	public int hashCode() {
		return this.annotationParsers.hashCode();
	}

}
