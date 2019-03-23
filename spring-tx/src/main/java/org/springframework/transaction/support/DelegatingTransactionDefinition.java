/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.support;

import java.io.Serializable;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.Assert;

/**
 * {@link TransactionDefinition} implementation that delegates all calls to a given target
 * {@link TransactionDefinition} instance. Abstract because it is meant to be subclassed,
 * with subclasses overriding specific methods that are not supposed to simply delegate
 * to the target instance.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class DelegatingTransactionDefinition implements TransactionDefinition, Serializable {

	private final TransactionDefinition targetDefinition;


	/**
	 * Create a DelegatingTransactionAttribute for the given target attribute.
	 * @param targetDefinition the target TransactionAttribute to delegate to
	 */
	public DelegatingTransactionDefinition(TransactionDefinition targetDefinition) {
		Assert.notNull(targetDefinition, "Target definition must not be null");
		this.targetDefinition = targetDefinition;
	}


	@Override
	public int getPropagationBehavior() {
		return this.targetDefinition.getPropagationBehavior();
	}

	@Override
	public int getIsolationLevel() {
		return this.targetDefinition.getIsolationLevel();
	}

	@Override
	public int getTimeout() {
		return this.targetDefinition.getTimeout();
	}

	@Override
	public boolean isReadOnly() {
		return this.targetDefinition.isReadOnly();
	}

	@Override
	public String getName() {
		return this.targetDefinition.getName();
	}


	@Override
	public boolean equals(Object obj) {
		return this.targetDefinition.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.targetDefinition.hashCode();
	}

	@Override
	public String toString() {
		return this.targetDefinition.toString();
	}

}
