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

package org.springframework.orm.hibernate3;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * Subclass of LocalDataSourceConnectionProvider that returns a
 * transaction-aware proxy for the exposed DataSource. Used if
 * LocalSessionFactoryBean's "useTransactionAwareDataSource" flag is on.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocalSessionFactoryBean#setUseTransactionAwareDataSource
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class TransactionAwareDataSourceConnectionProvider extends LocalDataSourceConnectionProvider {

	/**
	 * Return a TransactionAwareDataSourceProxy for the given DataSource,
	 * provided that it isn't a TransactionAwareDataSourceProxy already.
	 */
	@Override
	protected DataSource getDataSourceToUse(DataSource originalDataSource) {
		if (originalDataSource instanceof TransactionAwareDataSourceProxy) {
			return originalDataSource;
		}
		return new TransactionAwareDataSourceProxy(originalDataSource);
	}

	/**
	 * This implementation returns {@code true}: We can guarantee
	 * to receive the same Connection within a transaction, as we are
	 * exposing a TransactionAwareDataSourceProxy.
	 */
	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

}
