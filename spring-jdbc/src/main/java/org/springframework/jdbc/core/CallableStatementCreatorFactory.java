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

package org.springframework.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

/**
 * Helper class that efficiently creates multiple {@link CallableStatementCreator}
 * objects with different parameters based on a SQL statement and a single
 * set of parameter declarations.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class CallableStatementCreatorFactory {

	/** The SQL call string, which won't change when the parameters change. */
	private final String callString;

	/** List of SqlParameter objects. May not be {@code null}. */
	private final List<SqlParameter> declaredParameters;

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;

	private NativeJdbcExtractor nativeJdbcExtractor;


	/**
	 * Create a new factory. Will need to add parameters via the
	 * {@link #addParameter} method or have no parameters.
	 */
	public CallableStatementCreatorFactory(String callString) {
		this.callString = callString;
		this.declaredParameters = new LinkedList<SqlParameter>();
	}

	/**
	 * Create a new factory with the given SQL and the given parameters.
	 * @param callString the SQL call string
	 * @param declaredParameters list of {@link SqlParameter} objects
	 */
	public CallableStatementCreatorFactory(String callString, List<SqlParameter> declaredParameters) {
		this.callString = callString;
		this.declaredParameters = declaredParameters;
	}


	/**
	 * Add a new declared parameter.
	 * <p>Order of parameter addition is significant.
	 * @param param the parameter to add to the list of declared parameters
	 */
	public void addParameter(SqlParameter param) {
		this.declaredParameters.add(param);
	}

	/**
	 * Set whether to use prepared statements that return a specific type of ResultSet.
	 * specific type of ResultSet.
	 * @param resultSetType the ResultSet type
	 * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
	 * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
	 * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
	 */
	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	/**
	 * Set whether to use prepared statements capable of returning updatable ResultSets.
	 */
	public void setUpdatableResults(boolean updatableResults) {
		this.updatableResults = updatableResults;
	}

	/**
	 * Specify the NativeJdbcExtractor to use for unwrapping CallableStatements, if any.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}


	/**
	 * Return a new CallableStatementCreator instance given this parameters.
	 * @param params list of parameters (may be {@code null})
	 */
	public CallableStatementCreator newCallableStatementCreator(Map<String, ?> params) {
		return new CallableStatementCreatorImpl(params != null ? params : new HashMap<String, Object>());
	}

	/**
	 * Return a new CallableStatementCreator instance given this parameter mapper.
	 * @param inParamMapper ParameterMapper implementation that will return a Map of parameters
	 */
	public CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
		return new CallableStatementCreatorImpl(inParamMapper);
	}


	/**
	 * CallableStatementCreator implementation returned by this class.
	 */
	private class CallableStatementCreatorImpl implements CallableStatementCreator, SqlProvider, ParameterDisposer {

		private ParameterMapper inParameterMapper;

		private Map<String, ?> inParameters;

		/**
		 * Create a new CallableStatementCreatorImpl.
		 * @param inParamMapper ParameterMapper implementation for mapping input parameters
		 */
		public CallableStatementCreatorImpl(ParameterMapper inParamMapper) {
			this.inParameterMapper = inParamMapper;
		}

		/**
		 * Create a new CallableStatementCreatorImpl.
		 * @param inParams list of SqlParameter objects
		 */
		public CallableStatementCreatorImpl(Map<String, ?> inParams) {
			this.inParameters = inParams;
		}

		public CallableStatement createCallableStatement(Connection con) throws SQLException {
			// If we were given a ParameterMapper, we must let the mapper do its thing to create the Map.
			if (this.inParameterMapper != null) {
				this.inParameters = this.inParameterMapper.createMap(con);
			}
			else {
				if (this.inParameters == null) {
					throw new InvalidDataAccessApiUsageException(
							"A ParameterMapper or a Map of parameters must be provided");
				}
			}

			CallableStatement cs = null;
			if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
				cs = con.prepareCall(callString);
			}
			else {
				cs = con.prepareCall(callString, resultSetType,
						updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
			}

			// Determine CallabeStatement to pass to custom types.
			CallableStatement csToUse = cs;
			if (nativeJdbcExtractor != null) {
				csToUse = nativeJdbcExtractor.getNativeCallableStatement(cs);
			}

			int sqlColIndx = 1;
			for (SqlParameter declaredParam : declaredParameters) {
				if (!declaredParam.isResultsParameter()) {
					// So, it's a call parameter - part of the call string.
					// Get the value - it may still be null.
					Object inValue = this.inParameters.get(declaredParam.getName());
					if (declaredParam instanceof ResultSetSupportingSqlParameter) {
						// It's an output parameter: SqlReturnResultSet parameters already excluded.
						// It need not (but may be) supplied by the caller.
						if (declaredParam instanceof SqlOutParameter) {
							if (declaredParam.getTypeName() != null) {
								cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getTypeName());
							}
							else {
								if (declaredParam.getScale() != null) {
									cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getScale());
								}
								else {
									cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType());
								}
							}
							if (declaredParam.isInputValueProvided()) {
								StatementCreatorUtils.setParameterValue(csToUse, sqlColIndx, declaredParam, inValue);
							}
						}
					}
					else {
						// It's an input parameter; must be supplied by the caller.
						if (!this.inParameters.containsKey(declaredParam.getName())) {
							throw new InvalidDataAccessApiUsageException(
									"Required input parameter '" + declaredParam.getName() + "' is missing");
						}
						StatementCreatorUtils.setParameterValue(csToUse, sqlColIndx, declaredParam, inValue);
					}
					sqlColIndx++;
				}
			}

			return cs;
		}

		public String getSql() {
			return callString;
		}

		public void cleanupParameters() {
			if (this.inParameters != null) {
				StatementCreatorUtils.cleanupParameters(this.inParameters.values());
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("CallableStatementCreatorFactory.CallableStatementCreatorImpl: sql=[");
			sb.append(callString).append("]; parameters=").append(this.inParameters);
			return sb.toString();
		}
	}

}
