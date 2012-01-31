/*
 * Copyright 2002-2005 the original author or authors.
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

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ibatis.sqlmap.engine.type.BaseTypeHandler;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Abstract base class for iBATIS TypeHandler implementations that map to LOBs.
 * Retrieves the LobHandler to use from SqlMapClientFactoryBean at config time.
 *
 * <p>For writing LOBs, an active Spring transaction synchronization is required,
 * to be able to register a synchronization that closes the LobCreator.
 *
 * <p>Offers template methods for setting parameters and getting result values,
 * passing in the LobHandler or LobCreator to use.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see org.springframework.jdbc.support.lob.LobHandler
 * @see org.springframework.jdbc.support.lob.LobCreator
 * @see org.springframework.orm.ibatis.SqlMapClientFactoryBean#setLobHandler
 */
public abstract class AbstractLobTypeHandler extends BaseTypeHandler {

	/**
	 * Order value for TransactionSynchronization objects that clean up LobCreators.
	 * Return DataSourceUtils.#CONNECTION_SYNCHRONIZATION_ORDER - 100 to execute
	 * LobCreator cleanup before JDBC Connection cleanup, if any.
	 * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
	 */
	public static final int LOB_CREATOR_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 200;

	private LobHandler lobHandler;


	/**
	 * Constructor used by iBATIS: fetches config-time LobHandler from
	 * SqlMapClientFactoryBean.
	 * @see org.springframework.orm.ibatis.SqlMapClientFactoryBean#getConfigTimeLobHandler
	 */
	public AbstractLobTypeHandler() {
		this(SqlMapClientFactoryBean.getConfigTimeLobHandler());
	}

	/**
	 * Constructor used for testing: takes an explicit LobHandler.
	 */
	protected AbstractLobTypeHandler(LobHandler lobHandler) {
		if (lobHandler == null) {
			throw new IllegalStateException("No LobHandler found for configuration - " +
			    "lobHandler property must be set on SqlMapClientFactoryBean");
		}
		this.lobHandler = lobHandler;
	}


	/**
	 * This implementation delegates to setParameterInternal,
	 * passing in a transaction-synchronized LobCreator for the
	 * LobHandler of this type.
	 * @see #setParameterInternal
	 */
	public final void setParameter(PreparedStatement ps, int i, Object parameter, String jdbcType)
			throws SQLException {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("Spring transaction synchronization needs to be active for " +
					"setting values in iBATIS TypeHandlers that delegate to a Spring LobHandler");
		}
		final LobCreator lobCreator = this.lobHandler.getLobCreator();
		try {
			setParameterInternal(ps, i, parameter, jdbcType, lobCreator);
		}
		catch (IOException ex) {
			throw new SQLException("I/O errors during LOB access: " + ex.getMessage());
		}

		TransactionSynchronizationManager.registerSynchronization(
				new LobCreatorSynchronization(lobCreator));
	}

	/**
	 * This implementation delegates to the getResult version
	 * that takes a column index.
	 * @see #getResult(java.sql.ResultSet, String)
	 * @see java.sql.ResultSet#findColumn
	 */
	public final Object getResult(ResultSet rs, String columnName) throws SQLException {
		return getResult(rs, rs.findColumn(columnName));
	}

	/**
	 * This implementation delegates to getResultInternal,
	 * passing in the LobHandler of this type.
	 * @see #getResultInternal
	 */
	public final Object getResult(ResultSet rs, int columnIndex) throws SQLException {
		try {
			return getResultInternal(rs, columnIndex, this.lobHandler);
		}
		catch (IOException ex) {
			throw new SQLException(
					"I/O errors during LOB access: " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}

	/**
	 * This implementation always throws a SQLException:
	 * retrieving LOBs from a CallableStatement is not supported.
	 */
	public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
		throw new SQLException("Retrieving LOBs from a CallableStatement is not supported");
	}


	/**
	 * Template method to set the given value on the given statement.
	 * @param ps the PreparedStatement to set on
	 * @param index the statement parameter index
	 * @param value the parameter value to set
	 * @param jdbcType the JDBC type of the parameter
	 * @param lobCreator the LobCreator to use
	 * @throws SQLException if thrown by JDBC methods
	 * @throws IOException if thrown by streaming methods
	 */
	protected abstract void setParameterInternal(
			PreparedStatement ps, int index, Object value, String jdbcType, LobCreator lobCreator)
			throws SQLException, IOException;

	/**
	 * Template method to extract a value from the given result set.
	 * @param rs the ResultSet to extract from
	 * @param index the index in the ResultSet
	 * @param lobHandler the LobHandler to use
	 * @return the extracted value
	 * @throws SQLException if thrown by JDBC methods
	 * @throws IOException if thrown by streaming methods
	 */
	protected abstract Object getResultInternal(ResultSet rs, int index, LobHandler lobHandler)
			throws SQLException, IOException;


	/**
	 * Callback for resource cleanup at the end of a Spring transaction.
	 * Invokes LobCreator.close to clean up temporary LOBs that might have been created.
	 * @see org.springframework.jdbc.support.lob.LobCreator#close
	 */
	private static class LobCreatorSynchronization extends TransactionSynchronizationAdapter {

		private final LobCreator lobCreator;

		public LobCreatorSynchronization(LobCreator lobCreator) {
			this.lobCreator = lobCreator;
		}

		@Override
		public int getOrder() {
			return LOB_CREATOR_SYNCHRONIZATION_ORDER;
		}

		@Override
		public void beforeCompletion() {
			this.lobCreator.close();
		}
	}

}
