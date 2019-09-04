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

package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.core.metadata.TableMetaDataContext;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract class to provide base functionality for easy inserts
 * based on configuration options and database meta-data.
 *
 * <p>This class provides the base SPI for {@link SimpleJdbcInsert}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public abstract class AbstractJdbcInsert {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Lower-level class used to execute SQL. */
	private final JdbcTemplate jdbcTemplate;

	/** Context used to retrieve and manage database meta-data. */
	private final TableMetaDataContext tableMetaDataContext = new TableMetaDataContext();

	/** List of columns objects to be used in insert statement. */
	private final List<String> declaredColumns = new ArrayList<>();

	/** The names of the columns holding the generated key. */
	private String[] generatedKeyNames = new String[0];

	/**
	 * Has this operation been compiled? Compilation means at least checking
	 * that a DataSource or JdbcTemplate has been provided.
	 */
	private volatile boolean compiled = false;

	/** The generated string used for insert statement. */
	private String insertString = "";

	/** The SQL type information for the insert columns. */
	private int[] insertTypes = new int[0];


	/**
	 * Constructor to be used when initializing using a {@link DataSource}.
	 * @param dataSource the DataSource to be used
	 */
	protected AbstractJdbcInsert(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Constructor to be used when initializing using a {@link JdbcTemplate}.
	 * @param jdbcTemplate the JdbcTemplate to use
	 */
	protected AbstractJdbcInsert(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
	}


	//-------------------------------------------------------------------------
	// Methods dealing with configuration properties
	//-------------------------------------------------------------------------

	/**
	 * Get the configured {@link JdbcTemplate}.
	 */
	public JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	/**
	 * Set the name of the table for this insert.
	 */
	public void setTableName(@Nullable String tableName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setTableName(tableName);
	}

	/**
	 * Get the name of the table for this insert.
	 */
	@Nullable
	public String getTableName() {
		return this.tableMetaDataContext.getTableName();
	}

	/**
	 * Set the name of the schema for this insert.
	 */
	public void setSchemaName(@Nullable String schemaName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * Get the name of the schema for this insert.
	 */
	@Nullable
	public String getSchemaName() {
		return this.tableMetaDataContext.getSchemaName();
	}

	/**
	 * Set the name of the catalog for this insert.
	 */
	public void setCatalogName(@Nullable String catalogName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * Get the name of the catalog for this insert.
	 */
	@Nullable
	public String getCatalogName() {
		return this.tableMetaDataContext.getCatalogName();
	}

	/**
	 * Set the names of the columns to be used.
	 */
	public void setColumnNames(List<String> columnNames) {
		checkIfConfigurationModificationIsAllowed();
		this.declaredColumns.clear();
		this.declaredColumns.addAll(columnNames);
	}

	/**
	 * Get the names of the columns used.
	 */
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(this.declaredColumns);
	}

	/**
	 * Specify the name of a single generated key column.
	 */
	public void setGeneratedKeyName(String generatedKeyName) {
		checkIfConfigurationModificationIsAllowed();
		this.generatedKeyNames = new String[] {generatedKeyName};
	}

	/**
	 * Set the names of any generated keys.
	 */
	public void setGeneratedKeyNames(String... generatedKeyNames) {
		checkIfConfigurationModificationIsAllowed();
		this.generatedKeyNames = generatedKeyNames;
	}

	/**
	 * Get the names of any generated keys.
	 */
	public String[] getGeneratedKeyNames() {
		return this.generatedKeyNames;
	}

	/**
	 * Specify whether the parameter meta-data for the call should be used.
	 * The default is {@code true}.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.tableMetaDataContext.setAccessTableColumnMetaData(accessTableColumnMetaData);
	}

	/**
	 * Specify whether the default for including synonyms should be changed.
	 * The default is {@code false}.
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.tableMetaDataContext.setOverrideIncludeSynonymsDefault(override);
	}

	/**
	 * Get the insert string to be used.
	 */
	public String getInsertString() {
		return this.insertString;
	}

	/**
	 * Get the array of {@link java.sql.Types} to be used for insert.
	 */
	public int[] getInsertTypes() {
		return this.insertTypes;
	}


	//-------------------------------------------------------------------------
	// Methods handling compilation issues
	//-------------------------------------------------------------------------

	/**
	 * Compile this JdbcInsert using provided parameters and meta-data plus other settings.
	 * This finalizes the configuration for this object and subsequent attempts to compile are
	 * ignored. This will be implicitly called the first time an un-compiled insert is executed.
	 * @throws InvalidDataAccessApiUsageException if the object hasn't been correctly initialized,
	 * for example if no DataSource has been provided
	 */
	public final synchronized void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getTableName() == null) {
				throw new InvalidDataAccessApiUsageException("Table name is required");
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
				logger.debug("JdbcInsert for table [" + getTableName() + "] compiled");
			}
		}
	}

	/**
	 * Delegate method to perform the actual compilation.
	 * <p>Subclasses can override this template method to perform  their own compilation.
	 * Invoked after this base class's compilation is complete.
	 */
	protected void compileInternal() {
		DataSource dataSource = getJdbcTemplate().getDataSource();
		Assert.state(dataSource != null, "No DataSource set");
		this.tableMetaDataContext.processMetaData(dataSource, getColumnNames(), getGeneratedKeyNames());
		this.insertString = this.tableMetaDataContext.createInsertString(getGeneratedKeyNames());
		this.insertTypes = this.tableMetaDataContext.createInsertTypes();
		if (logger.isDebugEnabled()) {
			logger.debug("Compiled insert object: insert string is [" + this.insertString + "]");
		}
		onCompileInternal();
	}

	/**
	 * Hook method that subclasses may override to react to compilation.
	 * <p>This implementation is empty.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * Is this operation "compiled"?
	 * @return whether this operation is compiled and ready to use
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * Check whether this operation has been compiled already;
	 * lazily compile it if not already compiled.
	 * <p>Automatically called by {@code validateParameters}.
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("JdbcInsert not compiled before execution - invoking compile");
			compile();
		}
	}

	/**
	 * Method to check whether we are allowed to make any configuration changes at this time.
	 * If the class has been compiled, then no further changes to the configuration are allowed.
	 */
	protected void checkIfConfigurationModificationIsAllowed() {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException(
					"Configuration cannot be altered once the class has been compiled or used");
		}
	}


	//-------------------------------------------------------------------------
	// Methods handling execution
	//-------------------------------------------------------------------------

	/**
	 * Delegate method that executes the insert using the passed-in Map of parameters.
	 * @param args a Map with parameter names and values to be used in insert
	 * @return the number of rows affected
	 */
	protected int doExecute(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertInternal(values);
	}

	/**
	 * Delegate method that executes the insert using the passed-in {@link SqlParameterSource}.
	 * @param parameterSource parameter names and values to be used in insert
	 * @return the number of rows affected
	 */
	protected int doExecute(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertInternal(values);
	}

	/**
	 * Delegate method to execute the insert.
	 */
	private int executeInsertInternal(List<?> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for insert " + getInsertString() + " with: " + values);
		}
		return getJdbcTemplate().update(getInsertString(), values.toArray(), getInsertTypes());
	}

	/**
	 * Method that provides execution of the insert using the passed-in
	 * Map of parameters and returning a generated key.
	 * @param args a Map with parameter names and values to be used in insert
	 * @return the key generated by the insert
	 */
	protected Number doExecuteAndReturnKey(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertAndReturnKeyInternal(values);
	}

	/**
	 * Method that provides execution of the insert using the passed-in
	 * {@link SqlParameterSource} and returning a generated key.
	 * @param parameterSource parameter names and values to be used in insert
	 * @return the key generated by the insert
	 */
	protected Number doExecuteAndReturnKey(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertAndReturnKeyInternal(values);
	}

	/**
	 * Method that provides execution of the insert using the passed-in
	 * Map of parameters and returning all generated keys.
	 * @param args a Map with parameter names and values to be used in insert
	 * @return the KeyHolder containing keys generated by the insert
	 */
	protected KeyHolder doExecuteAndReturnKeyHolder(Map<String, ?> args) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(args);
		return executeInsertAndReturnKeyHolderInternal(values);
	}

	/**
	 * Method that provides execution of the insert using the passed-in
	 * {@link SqlParameterSource} and returning all generated keys.
	 * @param parameterSource parameter names and values to be used in insert
	 * @return the KeyHolder containing keys generated by the insert
	 */
	protected KeyHolder doExecuteAndReturnKeyHolder(SqlParameterSource parameterSource) {
		checkCompiled();
		List<Object> values = matchInParameterValuesWithInsertColumns(parameterSource);
		return executeInsertAndReturnKeyHolderInternal(values);
	}

	/**
	 * Delegate method to execute the insert, generating a single key.
	 */
	private Number executeInsertAndReturnKeyInternal(final List<?> values) {
		KeyHolder kh = executeInsertAndReturnKeyHolderInternal(values);
		if (kh.getKey() != null) {
			return kh.getKey();
		}
		else {
			throw new DataIntegrityViolationException(
					"Unable to retrieve the generated key for the insert: " + getInsertString());
		}
	}

	/**
	 * Delegate method to execute the insert, generating any number of keys.
	 */
	private KeyHolder executeInsertAndReturnKeyHolderInternal(final List<?> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for call " + getInsertString() + " with: " + values);
		}
		final KeyHolder keyHolder = new GeneratedKeyHolder();

		if (this.tableMetaDataContext.isGetGeneratedKeysSupported()) {
			getJdbcTemplate().update(
					con -> {
						PreparedStatement ps = prepareStatementForGeneratedKeys(con);
						setParameterValues(ps, values, getInsertTypes());
						return ps;
					},
					keyHolder);
		}

		else {
			if (!this.tableMetaDataContext.isGetGeneratedKeysSimulated()) {
				throw new InvalidDataAccessResourceUsageException(
						"The getGeneratedKeys feature is not supported by this database");
			}
			if (getGeneratedKeyNames().length < 1) {
				throw new InvalidDataAccessApiUsageException("Generated Key Name(s) not specified. " +
						"Using the generated keys features requires specifying the name(s) of the generated column(s)");
			}
			if (getGeneratedKeyNames().length > 1) {
				throw new InvalidDataAccessApiUsageException(
						"Current database only supports retrieving the key for a single column. There are " +
						getGeneratedKeyNames().length  + " columns specified: " + Arrays.asList(getGeneratedKeyNames()));
			}

			Assert.state(getTableName() != null, "No table name set");
			final String keyQuery = this.tableMetaDataContext.getSimpleQueryForGetGeneratedKey(
					getTableName(), getGeneratedKeyNames()[0]);
			Assert.state(keyQuery != null, "Query for simulating get generated keys must not be null");

			// This is a hack to be able to get the generated key from a database that doesn't support
			// get generated keys feature. HSQL is one, PostgreSQL is another. Postgres uses a RETURNING
			// clause while HSQL uses a second query that has to be executed with the same connection.

			if (keyQuery.toUpperCase().startsWith("RETURNING")) {
				Long key = getJdbcTemplate().queryForObject(
						getInsertString() + " " + keyQuery, values.toArray(), Long.class);
				Map<String, Object> keys = new HashMap<>(2);
				keys.put(getGeneratedKeyNames()[0], key);
				keyHolder.getKeyList().add(keys);
			}
			else {
				getJdbcTemplate().execute((ConnectionCallback<Object>) con -> {
					// Do the insert
					PreparedStatement ps = null;
					try {
						ps = con.prepareStatement(getInsertString());
						setParameterValues(ps, values, getInsertTypes());
						ps.executeUpdate();
					}
					finally {
						JdbcUtils.closeStatement(ps);
					}
					//Get the key
					Statement keyStmt = null;
					ResultSet rs = null;
					Map<String, Object> keys = new HashMap<>(2);
					try {
						keyStmt = con.createStatement();
						rs = keyStmt.executeQuery(keyQuery);
						if (rs.next()) {
							long key = rs.getLong(1);
							keys.put(getGeneratedKeyNames()[0], key);
							keyHolder.getKeyList().add(keys);
						}
					}
					finally {
						JdbcUtils.closeResultSet(rs);
						JdbcUtils.closeStatement(keyStmt);
					}
					return null;
				});
			}
		}

		return keyHolder;
	}

	/**
	 * Create a PreparedStatement to be used for an insert operation with generated keys.
	 * @param con the Connection to use
	 * @return the PreparedStatement
	 */
	private PreparedStatement prepareStatementForGeneratedKeys(Connection con) throws SQLException {
		if (getGeneratedKeyNames().length < 1) {
			throw new InvalidDataAccessApiUsageException("Generated Key Name(s) not specified. " +
					"Using the generated keys features requires specifying the name(s) of the generated column(s).");
		}
		PreparedStatement ps;
		if (this.tableMetaDataContext.isGeneratedKeysColumnNameArraySupported()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using generated keys support with array of column names.");
			}
			ps = con.prepareStatement(getInsertString(), getGeneratedKeyNames());
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Using generated keys support with Statement.RETURN_GENERATED_KEYS.");
			}
			ps = con.prepareStatement(getInsertString(), Statement.RETURN_GENERATED_KEYS);
		}
		return ps;
	}

	/**
	 * Delegate method that executes a batch insert using the passed-in Maps of parameters.
	 * @param batch array of Maps with parameter names and values to be used in batch insert
	 * @return array of number of rows affected
	 */
	@SuppressWarnings("unchecked")
	protected int[] doExecuteBatch(Map<String, ?>... batch) {
		checkCompiled();
		List<List<Object>> batchValues = new ArrayList<>(batch.length);
		for (Map<String, ?> args : batch) {
			batchValues.add(matchInParameterValuesWithInsertColumns(args));
		}
		return executeBatchInternal(batchValues);
	}

	/**
	 * Delegate method that executes a batch insert using the passed-in {@link SqlParameterSource SqlParameterSources}.
	 * @param batch array of SqlParameterSource with parameter names and values to be used in insert
	 * @return array of number of rows affected
	 */
	protected int[] doExecuteBatch(SqlParameterSource... batch) {
		checkCompiled();
		List<List<Object>> batchValues = new ArrayList<>(batch.length);
		for (SqlParameterSource parameterSource : batch) {
			batchValues.add(matchInParameterValuesWithInsertColumns(parameterSource));
		}
		return executeBatchInternal(batchValues);
	}

	/**
	 * Delegate method to execute the batch insert.
	 */
	private int[] executeBatchInternal(final List<List<Object>> batchValues) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing statement " + getInsertString() + " with batch of size: " + batchValues.size());
		}
		return getJdbcTemplate().batchUpdate(getInsertString(),
				new BatchPreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						setParameterValues(ps, batchValues.get(i), getInsertTypes());
					}
					@Override
					public int getBatchSize() {
						return batchValues.size();
					}
				});
	}

	/**
	 * Internal implementation for setting parameter values.
	 * @param preparedStatement the PreparedStatement
	 * @param values the values to be set
	 */
	private void setParameterValues(PreparedStatement preparedStatement, List<?> values, @Nullable int... columnTypes)
			throws SQLException {

		int colIndex = 0;
		for (Object value : values) {
			colIndex++;
			if (columnTypes == null || colIndex > columnTypes.length) {
				StatementCreatorUtils.setParameterValue(preparedStatement, colIndex, SqlTypeValue.TYPE_UNKNOWN, value);
			}
			else {
				StatementCreatorUtils.setParameterValue(preparedStatement, colIndex, columnTypes[colIndex - 1], value);
			}
		}
	}

	/**
	 * Match the provided in parameter values with registered parameters and parameters
	 * defined via meta-data processing.
	 * @param parameterSource the parameter values provided as a {@link SqlParameterSource}
	 * @return a Map with parameter names and values
	 */
	protected List<Object> matchInParameterValuesWithInsertColumns(SqlParameterSource parameterSource) {
		return this.tableMetaDataContext.matchInParameterValuesWithInsertColumns(parameterSource);
	}

	/**
	 * Match the provided in parameter values with registered parameters and parameters
	 * defined via meta-data processing.
	 * @param args the parameter values provided in a Map
	 * @return a Map with parameter names and values
	 */
	protected List<Object> matchInParameterValuesWithInsertColumns(Map<String, ?> args) {
		return this.tableMetaDataContext.matchInParameterValuesWithInsertColumns(args);
	}

}
