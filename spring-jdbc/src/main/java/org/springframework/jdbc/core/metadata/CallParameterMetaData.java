/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;

import org.jspecify.annotations.Nullable;

/**
 * Holder of meta-data for a specific parameter that is used for call processing.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see GenericCallMetaDataProvider
 */
public class CallParameterMetaData {

	private final boolean function;

	private final @Nullable String parameterName;

	private final int parameterType;

	private final int sqlType;

	private final @Nullable String typeName;

	private final boolean nullable;


	/**
	 * Constructor taking all the properties including the function marker.
	 * @since 5.2.9
	 */
	public CallParameterMetaData(boolean function, @Nullable String columnName, int columnType,
			int sqlType, @Nullable String typeName, boolean nullable) {

		this.function = function;
		this.parameterName = columnName;
		this.parameterType = columnType;
		this.sqlType = sqlType;
		this.typeName = typeName;
		this.nullable = nullable;
	}


	/**
	 * Return whether this parameter is declared in a function.
	 * @since 5.2.9
	 */
	public boolean isFunction() {
		return this.function;
	}

	/**
	 * Return the parameter name.
	 */
	public @Nullable String getParameterName() {
		return this.parameterName;
	}

	/**
	 * Return the parameter type.
	 */
	public int getParameterType() {
		return this.parameterType;
	}

	/**
	 * Determine whether the declared parameter qualifies as a 'return' parameter
	 * for our purposes: type {@link DatabaseMetaData#procedureColumnReturn} or
	 * {@link DatabaseMetaData#procedureColumnResult}, or in case of a function,
	 * {@link DatabaseMetaData#functionReturn}.
	 * @since 4.3.15
	 */
	public boolean isReturnParameter() {
		return (this.function ? this.parameterType == DatabaseMetaData.functionReturn :
				(this.parameterType == DatabaseMetaData.procedureColumnReturn ||
						this.parameterType == DatabaseMetaData.procedureColumnResult));
	}

	/**
	 * Determine whether the declared parameter qualifies as an 'out' parameter
	 * for our purposes: type {@link DatabaseMetaData#procedureColumnOut},
	 * or in case of a function, {@link DatabaseMetaData#functionColumnOut}.
	 * @since 5.3.31
	 */
	public boolean isOutParameter() {
		return (this.function ? this.parameterType == DatabaseMetaData.functionColumnOut :
				this.parameterType == DatabaseMetaData.procedureColumnOut);
	}

	/**
	 * Determine whether the declared parameter qualifies as an 'in-out' parameter
	 * for our purposes: type {@link DatabaseMetaData#procedureColumnInOut},
	 * or in case of a function, {@link DatabaseMetaData#functionColumnInOut}.
	 * @since 5.3.31
	 */
	public boolean isInOutParameter() {
		return (this.function ? this.parameterType == DatabaseMetaData.functionColumnInOut :
				this.parameterType == DatabaseMetaData.procedureColumnInOut);
	}

	/**
	 * Return the parameter SQL type.
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * Return the parameter type name.
	 */
	public @Nullable String getTypeName() {
		return this.typeName;
	}

	/**
	 * Return whether the parameter is nullable.
	 */
	public boolean isNullable() {
		return this.nullable;
	}

}
