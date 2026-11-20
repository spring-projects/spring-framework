/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Composite {@link TransactionAttributeSource} implementation that iterates
 * over a given array of {@link TransactionAttributeSource} instances.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class CompositeTransactionAttributeSource implements TransactionAttributeSource, Serializable {

	private final TransactionAttributeSource[] transactionAttributeSources;


	/**
	 * Create a new CompositeTransactionAttributeSource for the given sources.
	 * @param transactionAttributeSources the TransactionAttributeSource instances to combine
	 */
	public CompositeTransactionAttributeSource(TransactionAttributeSource... transactionAttributeSources) {
		Assert.notNull(transactionAttributeSources, "TransactionAttributeSource array must not be null");
		this.transactionAttributeSources = transactionAttributeSources;
	}

	/**
	 * Return the TransactionAttributeSource instances that this
	 * CompositeTransactionAttributeSource combines.
	 */
	public final TransactionAttributeSource[] getTransactionAttributeSources() {
		return this.transactionAttributeSources;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (TransactionAttributeSource source : this.transactionAttributeSources) {
			if (source.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		for (TransactionAttributeSource source : this.transactionAttributeSources) {
			if (source.hasTransactionAttribute(method, targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public @Nullable TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
		for (TransactionAttributeSource source : this.transactionAttributeSources) {
			TransactionAttribute attr = source.getTransactionAttribute(method, targetClass);
			if (attr != null) {
				return attr;
			}
		}
		return null;
	}

}
