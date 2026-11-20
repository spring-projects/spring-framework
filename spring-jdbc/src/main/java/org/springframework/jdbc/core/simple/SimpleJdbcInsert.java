/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.simple;

import java.util.Arrays;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

/**
 * A {@code SimpleJdbcInsert} is a multi-threaded, reusable object providing easy
 * (batch) insert capabilities for a table. It provides meta-data processing to
 * simplify the code needed to construct a basic insert statement. All you need
 * to provide is the name of the table and a {@code Map} containing the column
 * names and the column values.
 *
 * <p>The meta-data processing is based on the {@code DatabaseMetaData} provided
 * by the JDBC driver. As long as the JDBC driver can provide the names of the columns
 * for a specified table then we can rely on this auto-detection feature. If that
 * is not the case, then the column names must be specified explicitly.
 *
 * <p>The actual (batch) insert is handled using Spring's {@link JdbcTemplate}.
 *
 * <p>Many of the configuration methods return the current instance of the
 * {@code SimpleJdbcInsert} to provide the ability to chain multiple ones together
 * in a "fluent" API style.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see java.sql.DatabaseMetaData
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public class SimpleJdbcInsert extends AbstractJdbcInsert implements SimpleJdbcInsertOperations {

	/**
	 * Constructor that accepts the JDBC {@link DataSource} to use when creating
	 * the {@link JdbcTemplate}.
	 * @param dataSource the {@code DataSource} to use
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public SimpleJdbcInsert(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * Alternative constructor that accepts the {@link JdbcTemplate} to be used.
	 * @param jdbcTemplate the {@code JdbcTemplate} to use
	 * @see org.springframework.jdbc.core.JdbcTemplate#setDataSource
	 */
	public SimpleJdbcInsert(JdbcTemplate jdbcTemplate) {
		super(jdbcTemplate);
	}


	@Override
	public SimpleJdbcInsert withTableName(String tableName) {
		setTableName(tableName);
		return this;
	}

	@Override
	public SimpleJdbcInsert withSchemaName(String schemaName) {
		setSchemaName(schemaName);
		return this;
	}

	@Override
	public SimpleJdbcInsert withCatalogName(String catalogName) {
		setCatalogName(catalogName);
		return this;
	}

	@Override
	public SimpleJdbcInsert usingColumns(String... columnNames) {
		setColumnNames(Arrays.asList(columnNames));
		return this;
	}

	@Override
	public SimpleJdbcInsert usingGeneratedKeyColumns(String... columnNames) {
		setGeneratedKeyNames(columnNames);
		return this;
	}

	@Override
	public SimpleJdbcInsert usingQuotedIdentifiers() {
		setQuoteIdentifiers(true);
		return this;
	}

	@Override
	public SimpleJdbcInsert withoutTableColumnMetaDataAccess() {
		setAccessTableColumnMetaData(false);
		return this;
	}

	@Override
	public SimpleJdbcInsert includeSynonymsForTableColumnMetaData() {
		setOverrideIncludeSynonymsDefault(true);
		return this;
	}

	@Override
	public int execute(Map<String, ?> args) {
		return doExecute(args);
	}

	@Override
	public int execute(SqlParameterSource parameterSource) {
		return doExecute(parameterSource);
	}

	@Override
	public Number executeAndReturnKey(Map<String, ?> args) {
		return doExecuteAndReturnKey(args);
	}

	@Override
	public Number executeAndReturnKey(SqlParameterSource parameterSource) {
		return doExecuteAndReturnKey(parameterSource);
	}

	@Override
	public KeyHolder executeAndReturnKeyHolder(Map<String, ?> args) {
		return doExecuteAndReturnKeyHolder(args);
	}

	@Override
	public KeyHolder executeAndReturnKeyHolder(SqlParameterSource parameterSource) {
		return doExecuteAndReturnKeyHolder(parameterSource);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int[] executeBatch(Map<String, ?>... batch) {
		return doExecuteBatch(batch);
	}

	@Override
	public int[] executeBatch(SqlParameterSource... batch) {
		return doExecuteBatch(batch);
	}

}
