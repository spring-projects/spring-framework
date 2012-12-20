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

package org.springframework.orm.ibatis.support;

import javax.sql.DataSource;

import com.ibatis.sqlmap.client.SqlMapClient;

import org.springframework.dao.support.DaoSupport;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.util.Assert;

/**
 * Convenient super class for iBATIS SqlMapClient data access objects.
 * Requires a SqlMapClient to be set, providing a SqlMapClientTemplate
 * based on it to subclasses.
 *
 * <p>Instead of a plain SqlMapClient, you can also pass a preconfigured
 * SqlMapClientTemplate instance in. This allows you to share your
 * SqlMapClientTemplate configuration for all your DAOs, for example
 * a custom SQLExceptionTranslator to use.
 *
 * @author Juergen Hoeller
 * @since 24.02.2004
 * @see #setSqlMapClient
 * @see #setSqlMapClientTemplate
 * @see org.springframework.orm.ibatis.SqlMapClientTemplate
 * @see org.springframework.orm.ibatis.SqlMapClientTemplate#setExceptionTranslator
 * @deprecated as of Spring 3.2, in favor of the native Spring support
 * in the Mybatis follow-up project (http://code.google.com/p/mybatis/)
 */
@Deprecated
public abstract class SqlMapClientDaoSupport extends DaoSupport {

	private SqlMapClientTemplate sqlMapClientTemplate = new SqlMapClientTemplate();

	private boolean externalTemplate = false;


	/**
	 * Set the JDBC DataSource to be used by this DAO.
	 * Not required: The SqlMapClient might carry a shared DataSource.
	 * @see #setSqlMapClient
	 */
	public final void setDataSource(DataSource dataSource) {
		if (!this.externalTemplate) {
	  	this.sqlMapClientTemplate.setDataSource(dataSource);
		}
	}

	/**
	 * Return the JDBC DataSource used by this DAO.
	 */
	public final DataSource getDataSource() {
		return this.sqlMapClientTemplate.getDataSource();
	}

	/**
	 * Set the iBATIS Database Layer SqlMapClient to work with.
	 * Either this or a "sqlMapClientTemplate" is required.
	 * @see #setSqlMapClientTemplate
	 */
	public final void setSqlMapClient(SqlMapClient sqlMapClient) {
		if (!this.externalTemplate) {
			this.sqlMapClientTemplate.setSqlMapClient(sqlMapClient);
		}
	}

	/**
	 * Return the iBATIS Database Layer SqlMapClient that this template works with.
	 */
	public final SqlMapClient getSqlMapClient() {
		return this.sqlMapClientTemplate.getSqlMapClient();
	}

	/**
	 * Set the SqlMapClientTemplate for this DAO explicitly,
	 * as an alternative to specifying a SqlMapClient.
	 * @see #setSqlMapClient
	 */
	public final void setSqlMapClientTemplate(SqlMapClientTemplate sqlMapClientTemplate) {
		Assert.notNull(sqlMapClientTemplate, "SqlMapClientTemplate must not be null");
		this.sqlMapClientTemplate = sqlMapClientTemplate;
		this.externalTemplate = true;
	}

	/**
	 * Return the SqlMapClientTemplate for this DAO,
	 * pre-initialized with the SqlMapClient or set explicitly.
	 */
	public final SqlMapClientTemplate getSqlMapClientTemplate() {
	  return this.sqlMapClientTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (!this.externalTemplate) {
			this.sqlMapClientTemplate.afterPropertiesSet();
		}
	}

}
