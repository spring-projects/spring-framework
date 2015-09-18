/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.transaction;

import org.springframework.core.style.ToStringCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Configuration attributes for configuring transactional tests.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see TransactionConfiguration
 * @deprecated As of Spring Framework 4.2, this class is officially deprecated
 * and will be removed when {@code @TransactionConfiguration} is removed.
 */
@Deprecated
public class TransactionConfigurationAttributes {

	private final String transactionManagerName;

	private final boolean defaultRollback;


	/**
	 * Construct a new {@code TransactionConfigurationAttributes} instance
	 * using an empty string for the bean name of the
	 * {@link PlatformTransactionManager} and {@code true} for the default
	 * rollback flag.
	 * @see #TransactionConfigurationAttributes(String, boolean)
	 */
	public TransactionConfigurationAttributes() {
		this("", true);
	}

	/**
	 * Construct a new {@code TransactionConfigurationAttributes} instance
	 * from the supplied arguments.
	 * @param transactionManagerName the bean name of the
	 * {@link PlatformTransactionManager} that is to be used to drive
	 * <em>test-managed transactions</em>
	 * @param defaultRollback whether or not <em>test-managed transactions</em>
	 * should be rolled back by default
	 */
	public TransactionConfigurationAttributes(String transactionManagerName, boolean defaultRollback) {
		Assert.notNull(transactionManagerName, "transactionManagerName must not be null");
		this.transactionManagerName = transactionManagerName;
		this.defaultRollback = defaultRollback;
	}


	/**
	 * Get the bean name of the {@link PlatformTransactionManager} that is to
	 * be used to drive <em>test-managed transactions</em>.
	 */
	public final String getTransactionManagerName() {
		return this.transactionManagerName;
	}

	/**
	 * Whether <em>test-managed transactions</em> should be rolled back by default.
	 * @return the <em>default rollback</em> flag
	 */
	public final boolean isDefaultRollback() {
		return this.defaultRollback;
	}


	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("transactionManagerName", this.transactionManagerName)
				.append("defaultRollback", this.defaultRollback)
				.toString();
	}

}
