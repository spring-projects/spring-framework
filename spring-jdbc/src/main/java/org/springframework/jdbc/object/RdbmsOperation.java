/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An "RDBMS operation" is a multi-threaded, reusable object representing a query,
 * update, or stored procedure call. An RDBMS operation is <b>not</b> a command,
 * as a command is not reusable. However, execute methods may take commands as
 * arguments. Subclasses should be JavaBeans, allowing easy configuration.
 *
 * <p>This class and subclasses throw runtime exceptions, defined in the
 * <codeorg.springframework.dao package</code> (and as thrown by the
 * {@code org.springframework.jdbc.core} package, which the classes
 * in this package use under the hood to perform raw JDBC operations).
 *
 * <p>Subclasses should set SQL and add parameters before invoking the
 * {@link #compile()} method. The order in which parameters are added is
 * significant. The appropriate {@code execute} or {@code update}
 * method can then be invoked.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SqlQuery
 * @see SqlUpdate
 * @see StoredProcedure
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public abstract class RdbmsOperation implements InitializingBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Lower-level class used to execute SQL */
	private JdbcTemplate jdbcTemplate = new JdbcTemplate();

	private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

	private boolean updatableResults = false;

	private boolean returnGeneratedKeys = false;

	@Nullable
	private String[] generatedKeysColumnNames;

	@Nullable
	private String sql;

	private final List<SqlParameter> declaredParameters = new LinkedList<>();

	/**
	 * Has this operation been compiled? Compilation means at
	 * least checking that a DataSource and sql have been provided,
	 * but subclasses may also implement their own custom validation.
	 */
	private volatile boolean compiled;


	/**
	 * An alternative to the more commonly used {@link #setDataSource} when you want to
	 * use the same {@link JdbcTemplate} in multiple {@code RdbmsOperations}. This is
	 * appropriate if the {@code JdbcTemplate} has special configuration such as a
	 * {@link org.springframework.jdbc.support.SQLExceptionTranslator} to be reused.
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Return the {@link JdbcTemplate} used by this operation object.
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * Set the JDBC {@link DataSource} to obtain connections from.
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate.setDataSource(dataSource);
	}

	/**
	 * Set the fetch size for this RDBMS operation. This is important for processing
	 * large result sets: Setting this higher than the default value will increase
	 * processing speed at the cost of memory consumption; setting this lower can
	 * avoid transferring row data that will never be read by the application.
	 * <p>Default is 0, indicating to use the driver's default.
	 * @see org.springframework.jdbc.core.JdbcTemplate#setFetchSize
	 */
	public void setFetchSize(int fetchSize) {
		this.jdbcTemplate.setFetchSize(fetchSize);
	}

	/**
	 * Set the maximum number of rows for this RDBMS operation. This is important
	 * for processing subsets of large result sets, avoiding to read and hold
	 * the entire result set in the database or in the JDBC driver.
	 * <p>Default is 0, indicating to use the driver's default.
	 * @see org.springframework.jdbc.core.JdbcTemplate#setMaxRows
	 */
	public void setMaxRows(int maxRows) {
		this.jdbcTemplate.setMaxRows(maxRows);
	}

	/**
	 * Set the query timeout for statements that this RDBMS operation executes.
	 * <p>Default is 0, indicating to use the JDBC driver's default.
	 * <p>Note: Any timeout specified here will be overridden by the remaining
	 * transaction timeout when executing within a transaction that has a
	 * timeout specified at the transaction level.
	 */
	public void setQueryTimeout(int queryTimeout) {
		this.jdbcTemplate.setQueryTimeout(queryTimeout);
	}

	/**
	 * Set whether to use statements that return a specific type of ResultSet.
	 * @param resultSetType the ResultSet type
	 * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
	 * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
	 * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
	 * @see java.sql.Connection#prepareStatement(String, int, int)
	 */
	public void setResultSetType(int resultSetType) {
		this.resultSetType = resultSetType;
	}

	/**
	 * Return whether statements will return a specific type of ResultSet.
	 */
	public int getResultSetType() {
		return this.resultSetType;
	}

	/**
	 * Set whether to use statements that are capable of returning
	 * updatable ResultSets.
	 * @see java.sql.Connection#prepareStatement(String, int, int)
	 */
	public void setUpdatableResults(boolean updatableResults) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The updateableResults flag must be set before the operation is compiled");
		}
		this.updatableResults = updatableResults;
	}

	/**
	 * Return whether statements will return updatable ResultSets.
	 */
	public boolean isUpdatableResults() {
		return this.updatableResults;
	}

	/**
	 * Set whether prepared statements should be capable of returning
	 * auto-generated keys.
	 * @see java.sql.Connection#prepareStatement(String, int)
	 */
	public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The returnGeneratedKeys flag must be set before the operation is compiled");
		}
		this.returnGeneratedKeys = returnGeneratedKeys;
	}

	/**
	 * Return whether statements should be capable of returning
	 * auto-generated keys.
	 */
	public boolean isReturnGeneratedKeys() {
		return this.returnGeneratedKeys;
	}

	/**
	 * Set the column names of the auto-generated keys.
	 * @see java.sql.Connection#prepareStatement(String, String[])
	 */
	public void setGeneratedKeysColumnNames(String... names) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"The column names for the generated keys must be set before the operation is compiled");
		}
		this.generatedKeysColumnNames = names;
	}

	/**
	 * Return the column names of the auto generated keys.
	 */
	@Nullable
	public String[] getGeneratedKeysColumnNames() {
		return this.generatedKeysColumnNames;
	}

	/**
	 * Set the SQL executed by this operation.
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	/**
	 * Subclasses can override this to supply dynamic SQL if they wish, but SQL is
	 * normally set by calling the {@link #setSql} method or in a subclass constructor.
	 */
	@Nullable
	public String getSql() {
		return this.sql;
	}

	/**
	 * Resolve the configured SQL for actual use.
	 * @return the SQL (never {@code null})
	 * @since 5.0
	 */
	protected String resolveSql() {
		String sql = getSql();
		Assert.state(sql != null, "No SQL set");
		return sql;
	}

	/**
	 * Add anonymous parameters, specifying only their SQL types
	 * as defined in the {@code java.sql.Types} class.
	 * <p>Parameter ordering is significant. This method is an alternative
	 * to the {@link #declareParameter} method, which should normally be preferred.
	 * @param types array of SQL types as defined in the
	 * {@code java.sql.Types} class
	 * @throws InvalidDataAccessApiUsageException if the operation is already compiled
	 */
	public void setTypes(@Nullable int[] types) throws InvalidDataAccessApiUsageException {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once query is compiled");
		}
		if (types != null) {
			for (int type : types) {
				declareParameter(new SqlParameter(type));
			}
		}
	}

	/**
	 * Declare a parameter for this operation.
	 * <p>The order in which this method is called is significant when using
	 * positional parameters. It is not significant when using named parameters
	 * with named SqlParameter objects here; it remains significant when using
	 * named parameters in combination with unnamed SqlParameter objects here.
	 * @param param the SqlParameter to add. This will specify SQL type and (optionally)
	 * the parameter's name. Note that you typically use the {@link SqlParameter} class
	 * itself here, not any of its subclasses.
	 * @throws InvalidDataAccessApiUsageException if the operation is already compiled,
	 * and hence cannot be configured further
	 */
	public void declareParameter(SqlParameter param) throws InvalidDataAccessApiUsageException {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
		}
		this.declaredParameters.add(param);
	}

	/**
	 * Add one or more declared parameters. Used for configuring this operation
	 * when used in a bean factory.  Each parameter will specify SQL type and (optionally)
	 * the parameter's name.
	 * @param parameters Array containing the declared {@link SqlParameter} objects
	 * @see #declaredParameters
	 */
	public void setParameters(SqlParameter... parameters) {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
		}
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i] != null) {
				this.declaredParameters.add(parameters[i]);
			}
			else {
				throw new InvalidDataAccessApiUsageException("Cannot add parameter at index " + i + " from " +
						Arrays.asList(parameters) + " since it is 'null'");
			}
		}
	}

	/**
	 * Return a list of the declared {@link SqlParameter} objects.
	 */
	protected List<SqlParameter> getDeclaredParameters() {
		return this.declaredParameters;
	}


	/**
	 * Ensures compilation if used in a bean factory.
	 */
	@Override
	public void afterPropertiesSet() {
		compile();
	}

	/**
	 * Compile this query.
	 * Ignores subsequent attempts to compile.
	 * @throws InvalidDataAccessApiUsageException if the object hasn't
	 * been correctly initialized, for example if no DataSource has been provided
	 */
	public final void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getSql() == null) {
				throw new InvalidDataAccessApiUsageException("Property 'sql' is required");
			}

			try {
				this.jdbcTemplate.afterPropertiesSet();
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(ex.getMessage());
			}

			compileInternal();
			this.compiled = true;

			if (logger.isDebugEnabled()) {
				logger.debug("RdbmsOperation with SQL [" + getSql() + "] compiled");
			}
		}
	}

	/**
	 * Is this operation "compiled"? Compilation, as in JDO,
	 * means that the operation is fully configured, and ready to use.
	 * The exact meaning of compilation will vary between subclasses.
	 * @return whether this operation is compiled and ready to use
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * Check whether this operation has been compiled already;
	 * lazily compile it if not already compiled.
	 * <p>Automatically called by {@code validateParameters}.
	 * @see #validateParameters
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("SQL operation not compiled before execution - invoking compile");
			compile();
		}
	}

	/**
	 * Validate the parameters passed to an execute method based on declared parameters.
	 * Subclasses should invoke this method before every {@code executeQuery()}
	 * or {@code update()} method.
	 * @param parameters parameters supplied (may be {@code null})
	 * @throws InvalidDataAccessApiUsageException if the parameters are invalid
	 */
	protected void validateParameters(@Nullable Object[] parameters) throws InvalidDataAccessApiUsageException {
		checkCompiled();
		int declaredInParameters = 0;
		for (SqlParameter param : this.declaredParameters) {
			if (param.isInputValueProvided()) {
				if (!supportsLobParameters() &&
						(param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
					throw new InvalidDataAccessApiUsageException(
							"BLOB or CLOB parameters are not allowed for this kind of operation");
				}
				declaredInParameters++;
			}
		}
		validateParameterCount((parameters != null ? parameters.length : 0), declaredInParameters);
	}

	/**
	 * Validate the named parameters passed to an execute method based on declared parameters.
	 * Subclasses should invoke this method before every {@code executeQuery()} or
	 * {@code update()} method.
	 * @param parameters parameter Map supplied (may be {@code null})
	 * @throws InvalidDataAccessApiUsageException if the parameters are invalid
	 */
	protected void validateNamedParameters(@Nullable Map<String, ?> parameters) throws InvalidDataAccessApiUsageException {
		checkCompiled();
		Map<String, ?> paramsToUse = (parameters != null ? parameters : Collections.<String, Object> emptyMap());
		int declaredInParameters = 0;
		for (SqlParameter param : this.declaredParameters) {
			if (param.isInputValueProvided()) {
				if (!supportsLobParameters() &&
						(param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
					throw new InvalidDataAccessApiUsageException(
							"BLOB or CLOB parameters are not allowed for this kind of operation");
				}
				if (param.getName() != null && !paramsToUse.containsKey(param.getName())) {
					throw new InvalidDataAccessApiUsageException("The parameter named '" + param.getName() +
							"' was not among the parameters supplied: " + paramsToUse.keySet());
				}
				declaredInParameters++;
			}
		}
		validateParameterCount(paramsToUse.size(), declaredInParameters);
	}

	/**
	 * Validate the given parameter count against the given declared parameters.
	 * @param suppliedParamCount the number of actual parameters given
	 * @param declaredInParamCount the number of input parameters declared
	 */
	private void validateParameterCount(int suppliedParamCount, int declaredInParamCount) {
		if (suppliedParamCount < declaredInParamCount) {
			throw new InvalidDataAccessApiUsageException(suppliedParamCount + " parameters were supplied, but " +
					declaredInParamCount + " in parameters were declared in class [" + getClass().getName() + "]");
		}
		if (suppliedParamCount > this.declaredParameters.size() && !allowsUnusedParameters()) {
			throw new InvalidDataAccessApiUsageException(suppliedParamCount + " parameters were supplied, but " +
					declaredInParamCount + " parameters were declared in class [" + getClass().getName() + "]");
		}
	}


	/**
	 * Subclasses must implement this template method to perform their own compilation.
	 * Invoked after this base class's compilation is complete.
	 * <p>Subclasses can assume that SQL and a DataSource have been supplied.
	 * @throws InvalidDataAccessApiUsageException if the subclass hasn't been
	 * properly configured
	 */
	protected abstract void compileInternal() throws InvalidDataAccessApiUsageException;

	/**
	 * Return whether BLOB/CLOB parameters are supported for this kind of operation.
	 * <p>The default is {@code true}.
	 */
	protected boolean supportsLobParameters() {
		return true;
	}

	/**
	 * Return whether this operation accepts additional parameters that are
	 * given but not actually used. Applies in particular to parameter Maps.
	 * <p>The default is {@code false}.
	 * @see StoredProcedure
	 */
	protected boolean allowsUnusedParameters() {
		return false;
	}

}
