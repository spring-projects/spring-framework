/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.object;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.SqlParameter;

/**
 * Superclass for object abstractions of RDBMS stored procedures.
 * This class is abstract and it is intended that subclasses will provide a typed
 * method for invocation that delegates to the supplied {@link #execute} method.
 *
 * <p>The inherited {@link #setSql sql} property is the name of the stored procedure
 * in the RDBMS.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 */
public abstract class StoredProcedure extends SqlCall {

	/**
	 * Allow use as a bean.
	 */
	protected StoredProcedure() {
	}

	/**
	 * Create a new object wrapper for a stored procedure.
	 * @param ds the DataSource to use throughout the lifetime
	 * of this object to obtain connections
	 * @param name the name of the stored procedure in the database
	 */
	protected StoredProcedure(DataSource ds, String name) {
		setDataSource(ds);
		setSql(name);
	}

	/**
	 * Create a new object wrapper for a stored procedure.
	 * @param jdbcTemplate the JdbcTemplate which wraps DataSource
	 * @param name the name of the stored procedure in the database
	 */
	protected StoredProcedure(JdbcTemplate jdbcTemplate, String name) {
		setJdbcTemplate(jdbcTemplate);
		setSql(name);
	}


	/**
	 * StoredProcedure parameter Maps are by default allowed to contain
	 * additional entries that are not actually used as parameters.
	 */
	@Override
	protected boolean allowsUnusedParameters() {
		return true;
	}

	/**
	 * Declare a parameter.
	 * <p>Parameters declared as {@code SqlParameter} and {@code SqlInOutParameter}
	 * will always be used to provide input values. In addition to this, any parameter declared
	 * as {@code SqlOutParameter} where a non-null input value is provided will also be used
	 * as an input parameter.
	 * <b>Note: Calls to declareParameter must be made in the same order as
	 * they appear in the database's stored procedure parameter list.</b>
	 * <p>Names are purely used to help mapping.
	 * @param param the parameter object
	 */
	@Override
	public void declareParameter(SqlParameter param) throws InvalidDataAccessApiUsageException {
		if (param.getName() == null) {
			throw new InvalidDataAccessApiUsageException("Parameters to stored procedures must have names as well as types");
		}
		super.declareParameter(param);
	}

	/**
	 * Execute the stored procedure with the provided parameter values. This is
	 * a convenience method where the order of the passed in parameter values
	 * must match the order that the parameters where declared in.
	 * @param inParams variable number of input parameters. Output parameters should
	 * not be included in this map. It is legal for values to be {@code null}, and this
	 * will produce the correct behavior using a NULL argument to the stored procedure.
	 * @return map of output params, keyed by name as in parameter declarations.
	 * Output parameters will appear here, with their values after the stored procedure
	 * has been called.
	 */
	public Map<String, Object> execute(Object... inParams) {
		Map<String, Object> paramsToUse = new HashMap<>();
		validateParameters(inParams);
		int i = 0;
		for (SqlParameter sqlParameter : getDeclaredParameters()) {
			if (sqlParameter.isInputValueProvided() && i < inParams.length) {
				paramsToUse.put(sqlParameter.getName(), inParams[i++]);
			}
		}
		return getJdbcTemplate().call(newCallableStatementCreator(paramsToUse), getDeclaredParameters());
	}

	/**
	 * Execute the stored procedure. Subclasses should define a strongly typed
	 * execute method (with a meaningful name) that invokes this method, populating
	 * the input map and extracting typed values from the output map. Subclass
	 * execute methods will often take domain objects as arguments and return values.
	 * Alternatively, they can return void.
	 * @param inParams map of input parameters, keyed by name as in parameter
	 * declarations. Output parameters need not (but can) be included in this map.
	 * It is legal for map entries to be {@code null}, and this will produce the
	 * correct behavior using a NULL argument to the stored procedure.
	 * @return map of output params, keyed by name as in parameter declarations.
	 * Output parameters will appear here, with their values after the
	 * stored procedure has been called.
	 */
	public Map<String, Object> execute(Map<String, ?> inParams) throws DataAccessException {
		validateParameters(inParams.values().toArray());
		return getJdbcTemplate().call(newCallableStatementCreator(inParams), getDeclaredParameters());
	}

	/**
	 * Execute the stored procedure. Subclasses should define a strongly typed
	 * execute method (with a meaningful name) that invokes this method, passing in
	 * a ParameterMapper that will populate the input map.  This allows mapping database
	 * specific features since the ParameterMapper has access to the Connection object.
	 * The execute method is also responsible for extracting typed values from the output map.
	 * Subclass execute methods will often take domain objects as arguments and return values.
	 * Alternatively, they can return void.
	 * @param inParamMapper map of input parameters, keyed by name as in parameter
	 * declarations. Output parameters need not (but can) be included in this map.
	 * It is legal for map entries to be {@code null}, and this will produce the correct
	 * behavior using a NULL argument to the stored procedure.
	 * @return map of output params, keyed by name as in parameter declarations.
	 * Output parameters will appear here, with their values after the
	 * stored procedure has been called.
	 */
	public Map<String, Object> execute(ParameterMapper inParamMapper) throws DataAccessException {
		checkCompiled();
		return getJdbcTemplate().call(newCallableStatementCreator(inParamMapper), getDeclaredParameters());
	}

}
