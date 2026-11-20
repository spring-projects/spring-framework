/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;

/**
 * Postgres-specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public class PostgresCallMetaDataProvider extends GenericCallMetaDataProvider {

	private static final String RETURN_VALUE_NAME = "returnValue";

	private final String schemaName;


	public PostgresCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
		super(databaseMetaData);

		// Use current schema (or public schema) if no schema specified
		String schema = databaseMetaData.getConnection().getSchema();
		this.schemaName = (schema != null ? schema : "public");
	}


	@Override
	public boolean isReturnResultSetSupported() {
		return false;
	}

	@Override
	public boolean isRefCursorSupported() {
		return true;
	}

	@Override
	public int getRefCursorSqlType() {
		return Types.OTHER;
	}

	@Override
	public @Nullable String metaDataSchemaNameToUse(@Nullable String schemaName) {
		return (schemaName == null ? this.schemaName : super.metaDataSchemaNameToUse(schemaName));
	}

	@Override
	public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
		if (meta.getSqlType() == Types.OTHER && "refcursor".equals(meta.getTypeName())) {
			return new SqlOutParameter(parameterName, getRefCursorSqlType(), new ColumnMapRowMapper());
		}
		else {
			return super.createDefaultOutParameter(parameterName, meta);
		}
	}

	@Override
	public boolean byPassReturnParameter(String parameterName) {
		return RETURN_VALUE_NAME.equals(parameterName);
	}

}
