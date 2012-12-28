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

package org.springframework.orm.ibatis;

import java.sql.SQLException;

import com.ibatis.sqlmap.client.SqlMapExecutor;

/**
 * Callback interface for data access code that works with the iBATIS
 * {@link com.ibatis.sqlmap.client.SqlMapExecutor} interface. To be used
 * with {@link SqlMapClientTemplate}'s {@code execute} method,
 * assumably often as anonymous classes within a method implementation.
 *
 * @author Juergen Hoeller
 * @since 24.02.2004
 * @see SqlMapClientTemplate
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @deprecated as of Spring 3.2, in favor of the native Spring support
 * in the Mybatis follow-up project (http://code.google.com/p/mybatis/)
 */
@Deprecated
public interface SqlMapClientCallback<T> {

	/**
	 * Gets called by {@code SqlMapClientTemplate.execute} with an active
	 * {@code SqlMapExecutor}. Does not need to care about activating
	 * or closing the {@code SqlMapExecutor}, or handling transactions.
	 *
	 * <p>If called without a thread-bound JDBC transaction (initiated by
	 * DataSourceTransactionManager), the code will simply get executed on the
	 * underlying JDBC connection with its transactional semantics. If using
	 * a JTA-aware DataSource, the JDBC connection and thus the callback code
	 * will be transactional if a JTA transaction is active.
	 *
	 * <p>Allows for returning a result object created within the callback,
	 * i.e. a domain object or a collection of domain objects.
	 * A thrown custom RuntimeException is treated as an application exception:
	 * It gets propagated to the caller of the template.
	 *
	 * @param executor an active iBATIS SqlMapSession, passed-in as
	 * SqlMapExecutor interface here to avoid manual lifecycle handling
	 * @return a result object, or {@code null} if none
	 * @throws SQLException if thrown by the iBATIS SQL Maps API
	 * @see SqlMapClientTemplate#execute
	 * @see SqlMapClientTemplate#executeWithListResult
	 * @see SqlMapClientTemplate#executeWithMapResult
	 */
	T doInSqlMapClient(SqlMapExecutor executor) throws SQLException;

}
