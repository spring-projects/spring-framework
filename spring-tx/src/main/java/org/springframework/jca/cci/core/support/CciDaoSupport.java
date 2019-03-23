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

package org.springframework.jca.cci.core.support;

import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.springframework.dao.support.DaoSupport;
import org.springframework.jca.cci.CannotGetCciConnectionException;
import org.springframework.jca.cci.connection.ConnectionFactoryUtils;
import org.springframework.jca.cci.core.CciTemplate;

/**
 * Convenient super class for CCI-based data access objects.
 *
 * <p>Requires a {@link javax.resource.cci.ConnectionFactory} to be set,
 * providing a {@link org.springframework.jca.cci.core.CciTemplate} based
 * on it to subclasses through the {@link #getCciTemplate()} method.
 *
 * <p>This base class is mainly intended for CciTemplate usage but can
 * also be used when working with a Connection directly or when using
 * {@code org.springframework.jca.cci.object} classes.
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setConnectionFactory
 * @see #getCciTemplate
 * @see org.springframework.jca.cci.core.CciTemplate
 */
public abstract class CciDaoSupport extends DaoSupport {

	private CciTemplate cciTemplate;


	/**
	 * Set the ConnectionFactory to be used by this DAO.
	 */
	public final void setConnectionFactory(ConnectionFactory connectionFactory) {
		if (this.cciTemplate == null || connectionFactory != this.cciTemplate.getConnectionFactory()) {
		  this.cciTemplate = createCciTemplate(connectionFactory);
		}
	}

	/**
	 * Create a CciTemplate for the given ConnectionFactory.
	 * Only invoked if populating the DAO with a ConnectionFactory reference!
	 * <p>Can be overridden in subclasses to provide a CciTemplate instance
	 * with different configuration, or a custom CciTemplate subclass.
	 * @param connectionFactory the CCI ConnectionFactory to create a CciTemplate for
	 * @return the new CciTemplate instance
	 * @see #setConnectionFactory(javax.resource.cci.ConnectionFactory)
	 */
	protected CciTemplate createCciTemplate(ConnectionFactory connectionFactory) {
		return new CciTemplate(connectionFactory);
	}

	/**
	 * Return the ConnectionFactory used by this DAO.
	 */
	public final ConnectionFactory getConnectionFactory() {
		return this.cciTemplate.getConnectionFactory();
	}

	/**
	 * Set the CciTemplate for this DAO explicitly,
	 * as an alternative to specifying a ConnectionFactory.
	 */
	public final void setCciTemplate(CciTemplate cciTemplate) {
		this.cciTemplate = cciTemplate;
	}

	/**
	 * Return the CciTemplate for this DAO,
	 * pre-initialized with the ConnectionFactory or set explicitly.
	 */
	public final CciTemplate getCciTemplate() {
	  return this.cciTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.cciTemplate == null) {
			throw new IllegalArgumentException("'connectionFactory' or 'cciTemplate' is required");
		}
	}


	/**
	 * Obtain a CciTemplate derived from the main template instance,
	 * inheriting the ConnectionFactory and other settings but
	 * overriding the ConnectionSpec used for obtaining Connections.
	 * @param connectionSpec the CCI ConnectionSpec that the returned
	 * template instance is supposed to obtain Connections for
	 * @return the derived template instance
	 * @see org.springframework.jca.cci.core.CciTemplate#getDerivedTemplate(javax.resource.cci.ConnectionSpec)
	 */
	protected final CciTemplate getCciTemplate(ConnectionSpec connectionSpec) {
		return getCciTemplate().getDerivedTemplate(connectionSpec);
	}

	/**
	 * Get a CCI Connection, either from the current transaction or a new one.
	 * @return the CCI Connection
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException
	 * if the attempt to get a Connection failed
	 * @see org.springframework.jca.cci.connection.ConnectionFactoryUtils#getConnection(javax.resource.cci.ConnectionFactory)
	 */
	protected final Connection getConnection() throws CannotGetCciConnectionException {
		return ConnectionFactoryUtils.getConnection(getConnectionFactory());
	}

	/**
	 * Close the given CCI Connection, created via this bean's ConnectionFactory,
	 * if it isn't bound to the thread.
	 * @param con Connection to close
	 * @see org.springframework.jca.cci.connection.ConnectionFactoryUtils#releaseConnection
	 */
	protected final void releaseConnection(Connection con) {
		ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
	}

}
