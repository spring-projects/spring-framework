/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.hibernate4;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.service.jta.platform.internal.AbstractJtaPlatform;

import org.springframework.transaction.jta.UserTransactionAdapter;
import org.springframework.util.Assert;

/**
 * Implementation of Hibernate 4's {@link org.hibernate.service.jta.platform.spi.JtaPlatform}
 * SPI, exposing passed-in {@link TransactionManager} and {@link UserTransaction} references.
 *
 * @author Juergen Hoeller
 * @since 3.1.2
 */
class ConfigurableJtaPlatform extends AbstractJtaPlatform {

	private final TransactionManager transactionManager;

	private final UserTransaction userTransaction;


	/**
	 * Create a new ConfigurableJtaPlatform instance with the given
	 * JTA TransactionManager and optionally a given UserTransaction.
	 * @param tm the JTA TransactionManager reference (required)
	 * @param ut the JTA UserTransaction reference (optional)
	 */
	public ConfigurableJtaPlatform(TransactionManager tm, UserTransaction ut) {
		Assert.notNull(tm, "TransactionManager reference must not be null");
		this.transactionManager = tm;
		this.userTransaction = (ut != null ? ut : new UserTransactionAdapter(tm));
	}


	@Override
	protected TransactionManager locateTransactionManager() {
		return this.transactionManager;
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return this.userTransaction;
	}

	@Override
	protected boolean canCacheTransactionManager() {
		return true;
	}

	@Override
	protected boolean canCacheUserTransaction() {
		return true;
	}

}
