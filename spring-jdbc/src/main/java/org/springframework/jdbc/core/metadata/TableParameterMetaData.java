/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

/**
 * Holder of meta-data for a specific parameter that is used for table processing.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see GenericTableMetaDataProvider
 */
public class TableParameterMetaData {

	private final String parameterName;

	private final int sqlType;

	private final boolean nullable;


	/**
	 * Constructor taking all the properties.
	 */
	public TableParameterMetaData(String columnName, int sqlType, boolean nullable) {
		this.parameterName = columnName;
		this.sqlType = sqlType;
		this.nullable = nullable;
	}


	/**
	 * Get the parameter name.
	 */
	public String getParameterName() {
		return this.parameterName;
	}

	/**
	 * Get the parameter SQL type.
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * Get whether the parameter/column is nullable.
	 */
	public boolean isNullable() {
		return this.nullable;
	}

}
