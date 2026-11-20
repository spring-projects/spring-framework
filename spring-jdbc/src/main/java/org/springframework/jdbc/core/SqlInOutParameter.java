/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.ResultSet;

/**
 * Subclass of {@link SqlOutParameter} to represent an INOUT parameter.
 * Will return {@code true} for SqlParameter's {@link #isInputValueProvided}
 * test, in contrast to a standard SqlOutParameter.
 *
 * <p>Output parameters - like all stored procedure parameters - must have names.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 */
public class SqlInOutParameter extends SqlOutParameter {

	/**
	 * Create a new SqlInOutParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 */
	public SqlInOutParameter(String name, int sqlType) {
		super(name, sqlType);
	}

	/**
	 * Create a new SqlInOutParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 */
	public SqlInOutParameter(String name, int sqlType, int scale) {
		super(name, sqlType, scale);
	}

	/**
	 * Create a new SqlInOutParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName) {
		super(name, sqlType, typeName);
	}

	/**
	 * Create a new SqlInOutParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param typeName the type name of the parameter (optional)
	 * @param sqlReturnType custom value handler for complex type (optional)
	 */
	public SqlInOutParameter(String name, int sqlType, String typeName, SqlReturnType sqlReturnType) {
		super(name, sqlType, typeName, sqlReturnType);
	}

	/**
	 * Create a new SqlInOutParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rse the {@link ResultSetExtractor} to use for parsing the {@link ResultSet}
	 */
	public SqlInOutParameter(String name, int sqlType, ResultSetExtractor<?> rse) {
		super(name, sqlType, rse);
	}

	/**
	 * Create a new SqlInOutParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rch the {@link RowCallbackHandler} to use for parsing the {@link ResultSet}
	 */
	public SqlInOutParameter(String name, int sqlType, RowCallbackHandler rch) {
		super(name, sqlType, rch);
	}

	/**
	 * Create a new SqlInOutParameter.
	 * @param name the name of the parameter, as used in input and output maps
	 * @param sqlType the parameter SQL type according to {@code java.sql.Types}
	 * @param rm the {@link RowMapper} to use for parsing the {@link ResultSet}
	 */
	public SqlInOutParameter(String name, int sqlType, RowMapper<?> rm) {
		super(name, sqlType, rm);
	}


	/**
	 * This implementation always returns {@code true}.
	 */
	@Override
	public boolean isInputValueProvided() {
		return true;
	}

}
