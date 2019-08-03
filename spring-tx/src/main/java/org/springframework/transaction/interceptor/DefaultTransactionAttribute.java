/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;

/**
 * Spring's common transaction attribute implementation.
 * Rolls back on runtime, but not checked, exceptions by default.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.03.2003
 */
@SuppressWarnings("serial")
public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {

	@Nullable
	private String qualifier;

	@Nullable
	private String descriptor;


	/**
	 * Create a new DefaultTransactionAttribute, with default settings.
	 * Can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionAttribute() {
		super();
	}

	/**
	 * Copy constructor. Definition can be modified through bean property setters.
	 * @see #setPropagationBehavior
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 * @see #setName
	 */
	public DefaultTransactionAttribute(TransactionAttribute other) {
		super(other);
	}

	/**
	 * Create a new DefaultTransactionAttribute with the given
	 * propagation behavior. Can be modified through bean property setters.
	 * @param propagationBehavior one of the propagation constants in the
	 * TransactionDefinition interface
	 * @see #setIsolationLevel
	 * @see #setTimeout
	 * @see #setReadOnly
	 */
	public DefaultTransactionAttribute(int propagationBehavior) {
		super(propagationBehavior);
	}


	/**
	 * Associate a qualifier value with this transaction attribute.
	 * <p>This may be used for choosing a corresponding transaction manager
	 * to process this specific transaction.
	 * @since 3.0
	 */
	public void setQualifier(@Nullable String qualifier) {
		this.qualifier = qualifier;
	}

	/**
	 * Return a qualifier value associated with this transaction attribute.
	 * @since 3.0
	 */
	@Override
	@Nullable
	public String getQualifier() {
		return this.qualifier;
	}

	/**
	 * Set a descriptor for this transaction attribute,
	 * e.g. indicating where the attribute is applying.
	 * @since 4.3.4
	 */
	public void setDescriptor(@Nullable String descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * Return a descriptor for this transaction attribute,
	 * or {@code null} if none.
	 * @since 4.3.4
	 */
	@Nullable
	public String getDescriptor() {
		return this.descriptor;
	}

	/**
	 * The default behavior is as with EJB: rollback on unchecked exception
	 * ({@link RuntimeException}), assuming an unexpected outcome outside of any
	 * business rules. Additionally, we also attempt to rollback on {@link Error} which
	 * is clearly an unexpected outcome as well. By contrast, a checked exception is
	 * considered a business exception and therefore a regular expected outcome of the
	 * transactional business method, i.e. a kind of alternative return value which
	 * still allows for regular completion of resource operations.
	 * <p>This is largely consistent with TransactionTemplate's default behavior,
	 * except that TransactionTemplate also rolls back on undeclared checked exceptions
	 * (a corner case). For declarative transactions, we expect checked exceptions to be
	 * intentionally declared as business exceptions, leading to a commit by default.
	 * @see org.springframework.transaction.support.TransactionTemplate#execute
	 */
	@Override
	public boolean rollbackOn(Throwable ex) {
		return (ex instanceof RuntimeException || ex instanceof Error);
	}


	/**
	 * Return an identifying description for this transaction attribute.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected final StringBuilder getAttributeDescription() {
		StringBuilder result = getDefinitionDescription();
		if (StringUtils.hasText(this.qualifier)) {
			result.append("; '").append(this.qualifier).append("'");
		}
		return result;
	}

}
