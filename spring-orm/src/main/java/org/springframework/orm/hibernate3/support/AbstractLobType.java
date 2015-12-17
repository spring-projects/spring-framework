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

package org.springframework.orm.hibernate3.support;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.hibernate.util.EqualsHelper;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobCreatorUtils;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Abstract base class for Hibernate UserType implementations that map to LOBs.
 * Retrieves the LobHandler to use from LocalSessionFactoryBean at config time.
 *
 * <p>For writing LOBs, either an active Spring transaction synchronization
 * or an active JTA transaction (with "jtaTransactionManager" specified on
 * LocalSessionFactoryBean or a Hibernate TransactionManagerLookup configured
 * through the corresponding Hibernate property) is required.
 *
 * <p>Offers template methods for setting parameters and getting result values,
 * passing in the LobHandler or LobCreator to use.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.jdbc.support.lob.LobHandler
 * @see org.springframework.jdbc.support.lob.LobCreator
 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#setLobHandler
 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#setJtaTransactionManager
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public abstract class AbstractLobType implements UserType {

	protected final Log logger = LogFactory.getLog(getClass());

	private final LobHandler lobHandler;

	private final TransactionManager jtaTransactionManager;


	/**
	 * Constructor used by Hibernate: fetches config-time LobHandler and
	 * config-time JTA TransactionManager from LocalSessionFactoryBean.
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeLobHandler
	 * @see org.springframework.orm.hibernate3.LocalSessionFactoryBean#getConfigTimeTransactionManager
	 */
	protected AbstractLobType() {
		this(org.springframework.orm.hibernate3.LocalSessionFactoryBean.getConfigTimeLobHandler(),
				org.springframework.orm.hibernate3.LocalSessionFactoryBean.getConfigTimeTransactionManager());
	}

	/**
	 * Constructor used for testing: takes an explicit LobHandler
	 * and an explicit JTA TransactionManager (can be {@code null}).
	 */
	protected AbstractLobType(LobHandler lobHandler, TransactionManager jtaTransactionManager) {
		this.lobHandler = lobHandler;
		this.jtaTransactionManager = jtaTransactionManager;
	}


	/**
	 * This implementation returns false.
	 */
	@Override
	public boolean isMutable() {
		return false;
	}

	/**
	 * This implementation delegates to the Hibernate EqualsHelper.
	 * @see org.hibernate.util.EqualsHelper#equals
	 */
	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return EqualsHelper.equals(x, y);
	}

	/**
	 * This implementation returns the hashCode of the given objectz.
	 */
	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	/**
	 * This implementation returns the passed-in value as-is.
	 */
	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	/**
	 * This implementation returns the passed-in value as-is.
	 */
	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	/**
	 * This implementation returns the passed-in value as-is.
	 */
	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	/**
	 * This implementation returns the passed-in original as-is.
	 */
	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}


	/**
	 * This implementation delegates to nullSafeGetInternal,
	 * passing in the LobHandler of this type.
	 * @see #nullSafeGetInternal
	 */
	@Override
	@Deprecated
	public final Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws HibernateException, SQLException {

		if (this.lobHandler == null) {
			throw new IllegalStateException("No LobHandler found for configuration - " +
				"lobHandler property must be set on LocalSessionFactoryBean");
		}

		try {
			return nullSafeGetInternal(rs, names, owner, this.lobHandler);
		}
		catch (IOException ex) {
			throw new HibernateException("I/O errors during LOB access", ex);
		}
	}

	/**
	 * This implementation delegates to nullSafeSetInternal,
	 * passing in a transaction-synchronized LobCreator for the
	 * LobHandler of this type.
	 * @see #nullSafeSetInternal
	 */
	@Override
	@Deprecated
	public final void nullSafeSet(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException {

		if (this.lobHandler == null) {
			throw new IllegalStateException("No LobHandler found for configuration - " +
				"lobHandler property must be set on LocalSessionFactoryBean");
		}

		LobCreator lobCreator = this.lobHandler.getLobCreator();
		try {
			nullSafeSetInternal(st, index, value, lobCreator);
		}
		catch (IOException ex) {
			throw new HibernateException("I/O errors during LOB access", ex);
		}
		LobCreatorUtils.registerTransactionSynchronization(lobCreator, this.jtaTransactionManager);
	}

	/**
	 * Template method to extract a value from the given result set.
	 * @param rs the ResultSet to extract from
	 * @param names the column names
	 * @param owner the containing entity
	 * @param lobHandler the LobHandler to use
	 * @return the extracted value
	 * @throws SQLException if thrown by JDBC methods
	 * @throws IOException if thrown by streaming methods
	 * @throws HibernateException in case of any other exceptions
	 */
	protected abstract Object nullSafeGetInternal(
			ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException, IOException, HibernateException;

	/**
	 * Template method to set the given parameter value on the given statement.
	 * @param ps the PreparedStatement to set on
	 * @param index the statement parameter index
	 * @param value the value to set
	 * @param lobCreator the LobCreator to use
	 * @throws SQLException if thrown by JDBC methods
	 * @throws IOException if thrown by streaming methods
	 * @throws HibernateException in case of any other exceptions
	 */
	protected abstract void nullSafeSetInternal(
		PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException, IOException, HibernateException;

}
