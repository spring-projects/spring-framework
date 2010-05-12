/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract class to provide base functionality for easy stored procedure calls
 * based on configuration options and database metadata.
 * This class provides the base SPI for {@link SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @since 2.5
 */
public abstract class AbstractJdbcCall {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Lower-level class used to execute SQL */
	private final JdbcTemplate jdbcTemplate;

	/** List of SqlParameter objects */
	private final List<SqlParameter> declaredParameters = new ArrayList<SqlParameter>();

	/** List of RefCursor/ResultSet RowMapper objects */
	private final Map<String, RowMapper> declaredRowMappers = new LinkedHashMap<String, RowMapper>();

	/**
	 * Has this operation been compiled? Compilation means at
	 * least checking that a DataSource and sql have been provided,
	 * but subclasses may also implement their own custom validation.
	 */
	private boolean compiled = false;

	/** the generated string used for call statement */
	private String callString;

	/** context used to retrieve and manage database metadata */
	private CallMetaDataContext callMetaDataContext = new CallMetaDataContext();

	/**
	 * Object enabling us to create CallableStatementCreators
	 * efficiently, based on this class's declared parameters.
	 */
	private CallableStatementCreatorFactory callableStatementFactory;


	/**
	 * Constructor to be used when initializing using a {@link DataSource}.
	 * @param dataSource the DataSource to be used
	 */
	protected AbstractJdbcCall(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Constructor to be used when initializing using a {@link JdbcTemplate}.
	 * @param jdbcTemplate the JdbcTemplate to use
	 */
	protected AbstractJdbcCall(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}


	/**
	 * Get the configured {@link JdbcTemplate}
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * Get the {@link CallableStatementCreatorFactory} being used
	 */
	protected CallableStatementCreatorFactory getCallableStatementFactory() {
		return this.callableStatementFactory;
	}


	/**
	 * Set the name of the stored procedure.
	 */
	public void setProcedureName(String procedureName) {
		this.callMetaDataContext.setProcedureName(procedureName);
	}

	/**
	 * Get the name of the stored procedure.
	 */
	public String getProcedureName() {
		return this.callMetaDataContext.getProcedureName();
	}

	/**
	 * Set the names of in parameters to be used.
	 */
	public void setInParameterNames(Set<String> inParameterNames) {
		this.callMetaDataContext.setLimitedInParameterNames(inParameterNames);
	}

	/**
	 * Get the names of in parameters to be used.
	 */
	public Set<String> getInParameterNames() {
		return this.callMetaDataContext.getLimitedInParameterNames();
	}

	/**
	 * Set the catalog name to use.
	 */
	public void setCatalogName(String catalogName) {
		this.callMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * Get the catalog name used.
	 */
	public String getCatalogName() {
		return this.callMetaDataContext.getCatalogName();
	}

	/**
	 * Set the schema name to use,
	 */
	public void setSchemaName(String schemaName) {
		this.callMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * Get the schema name used.
	 */
	public String getSchemaName() {
		return this.callMetaDataContext.getSchemaName();
	}

	/**
	 * Specify whether this call is a function call.
	 */
	public void setFunction(boolean function) {
		this.callMetaDataContext.setFunction(function);
	}

	/**
	 * Is this call a function call?
	 */
	public boolean isFunction() {
		return this.callMetaDataContext.isFunction();
	}

	/**
	 * Specify whether the call requires a rerurn value.
	 */
	public void setReturnValueRequired(boolean b) {
		this.callMetaDataContext.setReturnValueRequired(b);
	}

	/**
	 * Does the call require a return value?
	 */
	public boolean isReturnValueRequired() {
		return this.callMetaDataContext.isReturnValueRequired();
	}

	/**
	 * Add a declared parameter to the list of parameters for the call.
	 * Only parameters declared as <code>SqlParameter</code> and <code>SqlInOutParameter</code>
	 * will be used to provide input values.  This is different from the <code>StoredProcedure</code> class
	 * which for backwards compatibility reasons allows input values to be provided for parameters declared
	 * as <code>SqlOutParameter</code>.
	 * @param parameter the {@link SqlParameter} to add
	 */
	public void addDeclaredParameter(SqlParameter parameter) {
		Assert.notNull(parameter, "The supplied parameter must not be null");
		if (!StringUtils.hasText(parameter.getName())) {
			throw new InvalidDataAccessApiUsageException(
					"You must specify a parameter name when declaring parameters for \"" + getProcedureName() + "\"");
		}
		this.declaredParameters.add(parameter);
		if (logger.isDebugEnabled()) {
			logger.debug("Added declared parameter for [" + getProcedureName() + "]: " + parameter.getName());
		}
	}

	/**
	 * Add a {@link org.springframework.jdbc.core.RowMapper} for the specified parameter or column.
	 * @param parameterName name of parameter or column
	 * @param rowMapper the RowMapper implementation to use
	 */
	public void addDeclaredRowMapper(String parameterName, RowMapper rowMapper) {
		this.declaredRowMappers.put(parameterName, rowMapper);
		if (logger.isDebugEnabled()) {
			logger.debug("Added row mapper for [" + getProcedureName() + "]: " + parameterName);
		}
	}

	/**
	 * Add a {@link org.springframework.jdbc.core.RowMapper} for the specified parameter or column.
	 * @deprecated in favor of {@link #addDeclaredRowMapper(String, org.springframework.jdbc.core.RowMapper)}
	 */
	@Deprecated
	public void addDeclaredRowMapper(String parameterName, ParameterizedRowMapper rowMapper) {
		addDeclaredRowMapper(parameterName, (RowMapper) rowMapper);
	}

	/**
	 * Get the call string that should be used based on parameters and meta data
	 */
	public String getCallString() {
		return this.callString;
	}

	/**
	 * Specify whether the parameter metadata for the call should be used.  The default is true.
	 */
	public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
		this.callMetaDataContext.setAccessCallParameterMetaData(accessCallParameterMetaData);
	}


	//-------------------------------------------------------------------------
	// Methods handling compilation issues
	//-------------------------------------------------------------------------

	/**
	 * Compile this JdbcCall using provided parameters and meta data plus other settings.  This
	 * finalizes the configuration for this object and subsequent attempts to compile are ignored.
	 * This will be implicitly called the first time an un-compiled call is executed.
	 * @throws org.springframework.dao.InvalidDataAccessApiUsageException if the object hasn't
	 * been correctly initialized, for example if no DataSource has been provided
	 */
	public synchronized final void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getProcedureName() == null) {
				throw new InvalidDataAccessApiUsageException("Procedure or Function name is required");
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
				logger.debug("SqlCall for " + (isFunction() ? "function" : "procedure") + " [" + getProcedureName() + "] compiled");
			}
		}
	}

	/**
	 * Method to perform the actual compilation.  Subclasses can override this template method to perform
	 * their own compilation.  Invoked after this base class's compilation is complete.
	 */
	protected void compileInternal() {
		this.callMetaDataContext.initializeMetaData(getJdbcTemplate().getDataSource());

		// iterate over the declared RowMappers and register the corresponding SqlParameter
		for (Map.Entry<String, RowMapper> entry : this.declaredRowMappers.entrySet()) {
			SqlParameter resultSetParameter =
					this.callMetaDataContext.createReturnResultSetParameter(entry.getKey(), entry.getValue());
			this.declaredParameters.add(resultSetParameter);
		}
		callMetaDataContext.processParameters(this.declaredParameters);

		this.callString = this.callMetaDataContext.createCallString();
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled stored procedure. Call string is [" + this.callString + "]");
		}

		this.callableStatementFactory =
				new CallableStatementCreatorFactory(getCallString(), this.callMetaDataContext.getCallParameters());
		this.callableStatementFactory.setNativeJdbcExtractor(getJdbcTemplate().getNativeJdbcExtractor());

		onCompileInternal();
	}

	/**
	 * Hook method that subclasses may override to react to compilation.
	 * This implementation does nothing.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * Is this operation "compiled"?
	 * @return whether this operation is compiled, and ready to use.
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * Check whether this operation has been compiled already;
	 * lazily compile it if not already compiled.
	 * <p>Automatically called by <code>doExecute</code>.
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("JdbcCall call not compiled before execution - invoking compile");
			compile();
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling execution
	//-------------------------------------------------------------------------

	/**
	 * Method that provides execution of the call using the passed in {@link SqlParameterSource}
	 * @param parameterSource parameter names and values to be used in call
	 * @return Map of out parameters
	 */
	protected Map<String, Object> doExecute(SqlParameterSource parameterSource) {
		checkCompiled();
		Map<String, Object> params = matchInParameterValuesWithCallParameters(parameterSource);
		return executeCallInternal(params);
	}

	/**
	 * Method that provides execution of the call using the passed in array of parameters
	 * @param args array of parameter values; order must match the order declared for the stored procedure
	 * @return Map of out parameters
	 */
	protected Map<String, Object> doExecute(Object[] args) {
		checkCompiled();
		Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
		return executeCallInternal(params);
	}

	/**
	 * Method that provides execution of the call using the passed in Map of parameters
	 * @param args Map of parameter name and values
	 * @return Map of out parameters
	 */
	protected Map<String, Object> doExecute(Map<String, ?> args) {
		checkCompiled();
		Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
		return executeCallInternal(params);
	}

	/**
	 * Method to perform the actual call processing
	 */
	private Map<String, Object> executeCallInternal(Map<String, ?> params) {
		CallableStatementCreator csc = getCallableStatementFactory().newCallableStatementCreator(params);
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for call " + getCallString() + " with: " + params);
			int i = 1;
			for (SqlParameter p : getCallParameters()) {
				logger.debug(i++ + ": " +  p.getName() + " SQL Type "+ p.getSqlType() + " Type Name " + p.getTypeName() + " " + p.getClass().getName());
			}
		}
		return getJdbcTemplate().call(csc, getCallParameters());
	}

	/**
	 * Get the name of a single out parameter or return value.
	 * Used for functions or procedures with one out parameter.
	 */
	protected String getScalarOutParameterName() {
		return this.callMetaDataContext.getScalarOutParameterName();
	}

	/**
	 * Match the provided in parameter values with registered parameters and
	 * parameters defined via metadata processing.
	 * @param parameterSource the parameter vakues provided as a {@link SqlParameterSource}
	 * @return Map with parameter names and values
	 */
	protected Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(parameterSource);
	}

	/**
	 * Match the provided in parameter values with registered parameters and
	 * parameters defined via metadata processing.
	 * @param args the parameter values provided as an array
	 * @return Map with parameter names and values
	 */
	private Map<String, ?> matchInParameterValuesWithCallParameters(Object[] args) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
	}

	/**
	 * Match the provided in parameter values with registered parameters and
	 * parameters defined via metadata processing.
	 * @param args the parameter values provided in a Map
	 * @return Map with parameter names and values
	 */
	protected Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> args) {
		return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
	}

	/**
	 * Get a List of all the call parameters to be used for call. This includes any parameters added
	 * based on meta data processing.
	 */
	protected List<SqlParameter> getCallParameters() {
		return this.callMetaDataContext.getCallParameters();
	}

}
