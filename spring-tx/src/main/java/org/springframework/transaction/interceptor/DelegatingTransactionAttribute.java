/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.support.DelegatingTransactionDefinition;

/**
 * {@link TransactionAttribute} implementation that delegates all calls to a given target
 * {@link TransactionAttribute} instance. Abstract because it is meant to be subclassed,
 * with subclasses overriding specific methods that are not supposed to simply delegate
 * to the target instance.
 *
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @since 1.2
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionAttribute extends DelegatingTransactionDefinition
		implements TransactionAttribute, Serializable {

	private final TransactionAttribute targetAttribute;


	/**
	 * Create a DelegatingTransactionAttribute for the given target attribute.
	 * @param targetAttribute the target TransactionAttribute to delegate to
	 */
	public DelegatingTransactionAttribute(TransactionAttribute targetAttribute) {
		super(targetAttribute);
		this.targetAttribute = targetAttribute;
	}


	@Override
	public @Nullable String getQualifier() {
		return this.targetAttribute.getQualifier();
	}

	@Override
	public Collection<String> getLabels() {
		return this.targetAttribute.getLabels();
	}

	@Override
	public boolean rollbackOn(Throwable ex) {
		return this.targetAttribute.rollbackOn(ex);
	}

}
