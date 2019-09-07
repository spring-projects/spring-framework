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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * RdbmsOperation using a JdbcTemplate and representing an SQL-based
 * call such as a stored procedure or a stored function.
 *
 * <p>Configures a CallableStatementCreatorFactory based on the declared
 * parameters.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see CallableStatementCreatorFactory
 */
public abstract class SqlCall extends RdbmsOperation {

	/**
	 * Flag used to indicate that this call is for a function and to
	 * use the {? = call get_invoice_count(?)} syntax.
	 */
	private boolean function = false;

	/**
	 * Flag used to indicate that the sql for this call should be used exactly as
	 * it is defined. No need to add the escape syntax and parameter place holders.
	 */
	private boolean sqlReadyForUse = false;

	/**
	 * Call string as defined in java.sql.CallableStatement.
	 * String of form {call add_invoice(?, ?, ?)} or {? = call get_invoice_count(?)}
	 * if isFunction is set to true. Updated after each parameter is added.
	 */
	@Nullable
	private String callString;

	/**
	 * Object enabling us to create CallableStatementCreators
	 * efficiently, based on this class's declared parameters.
	 */
	@Nullable
	private CallableStatementCreatorFactory callableStatementFactory;


	/**
	 * Constructor to allow use as a JavaBean.
	 * A DataSource, SQL and any parameters must be supplied before
	 * invoking the {@code compile} method and using this object.
	 * @see #setDataSource
	 * @see #setSql
	 * @see #compile
	 */
	public SqlCall() {
	}

	/**
	 * Create a new SqlCall object with SQL, but without parameters.
	 * Must add parameters or settle with none.
	 * @param ds the DataSource to obtain connections from
	 * @param sql the SQL to execute
	 */
	public SqlCall(DataSource ds, String sql) {
		setDataSource(ds);
		setSql(sql);
	}


	/**
	 * Set whether this call is for a function.
	 */
	public void setFunction(boolean function) {
		this.function = function;
	}

	/**
	 * Return whether this call is for a function.
	 */
	public boolean isFunction() {
		return this.function;
	}

	/**
	 * Set whether the SQL can be used as is.
	 */
	public void setSqlReadyForUse(boolean sqlReadyForUse) {
		this.sqlReadyForUse = sqlReadyForUse;
	}

	/**
	 * Return whether the SQL can be used as is.
	 */
	public boolean isSqlReadyForUse() {
		return this.sqlReadyForUse;
	}


	/**
	 * Overridden method to configure the CallableStatementCreatorFactory
	 * based on our declared parameters.
	 * @see RdbmsOperation#compileInternal()
	 */
	@Override
	protected final void compileInternal() {
		if (isSqlReadyForUse()) {
			this.callString = resolveSql();
		}
		else {
			StringBuilder callString = new StringBuilder(32);
			List<SqlParameter> parameters = getDeclaredParameters();
			int parameterCount = 0;
			if (isFunction()) {
				callString.append("{? = call ").append(resolveSql()).append('(');
				parameterCount = -1;
			}
			else {
				callString.append("{call ").append(resolveSql()).append('(');
			}
			for (SqlParameter parameter : parameters) {
				if (!parameter.isResultsParameter()) {
					if (parameterCount > 0) {
						callString.append(", ");
					}
					if (parameterCount >= 0) {
						callString.append('?');
					}
					parameterCount++;
				}
			}
			callString.append(")}");
			this.callString = callString.toString();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled stored procedure. Call string is [" + this.callString + "]");
		}

		this.callableStatementFactory = new CallableStatementCreatorFactory(this.callString, getDeclaredParameters());
		this.callableStatementFactory.setResultSetType(getResultSetType());
		this.callableStatementFactory.setUpdatableResults(isUpdatableResults());

		onCompileInternal();
	}

	/**
	 * Hook method that subclasses may override to react to compilation.
	 * This implementation does nothing.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * Get the call string.
	 */
	@Nullable
	public String getCallString() {
		return this.callString;
	}

	/**
	 * Return a CallableStatementCreator to perform an operation
	 * with this parameters.
	 * @param inParams parameters. May be {@code null}.
	 */
	protected CallableStatementCreator newCallableStatementCreator(@Nullable Map<String, ?> inParams) {
		Assert.state(this.callableStatementFactory != null, "No CallableStatementFactory available");
		return this.callableStatementFactory.newCallableStatementCreator(inParams);
	}

	/**
	 * Return a CallableStatementCreator to perform an operation
	 * with the parameters returned from this ParameterMapper.
	 * @param inParamMapper parametermapper. May not be {@code null}.
	 */
	protected CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
		Assert.state(this.callableStatementFactory != null, "No CallableStatementFactory available");
		return this.callableStatementFactory.newCallableStatementCreator(inParamMapper);
	}

}
