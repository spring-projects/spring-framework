/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.namedparam;

import java.sql.SQLType;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link SqlParameterSource} implementations.
 * Provides registration of SQL types per parameter and a friendly
 * {@link #toString() toString} representation enumerating all parameters for
 * a {@code SqlParameterSource} implementing {@link #getParameterNames()}.
 * Concrete subclasses must implement {@link #hasValue} and {@link #getValue}.
 *
 * @author Juergen Hoeller
 * @author Jens Schauder
 * @since 2.0
 * @see #hasValue(String)
 * @see #getValue(String)
 * @see #getParameterNames()
 */
public abstract class AbstractSqlParameterSource implements SqlParameterSource {

	private final Map<String, Integer> sqlTypes = new HashMap<>();

	private final Map<String, String> typeNames = new HashMap<>();


	/**
	 * Register an SQL type for the given parameter.
	 * @param paramName the name of the parameter
	 * @param sqlType the SQL type from an enum such as {@link java.sql.JDBCType}
	 * @since 7.1.1
	 */
	public void registerSqlType(String paramName, SQLType sqlType) {
		Assert.notNull(paramName, "Parameter name must not be null");
		this.sqlTypes.put(paramName, sqlType.getVendorTypeNumber());
	}

	/**
	 * Register an SQL type for the given parameter.
	 * @param paramName the name of the parameter
	 * @param sqlType the SQL type of the parameter
	 */
	public void registerSqlType(String paramName, int sqlType) {
		Assert.notNull(paramName, "Parameter name must not be null");
		if (sqlType != TYPE_UNKNOWN) {
			this.sqlTypes.put(paramName, sqlType);
		}
		else {
			this.sqlTypes.remove(paramName);
		}
	}

	/**
	 * Register an SQL type for the given parameter.
	 * @param paramName the name of the parameter
	 * @param typeName the type name of the parameter
	 */
	public void registerTypeName(String paramName, @Nullable String typeName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		if (typeName != null) {
			this.typeNames.put(paramName, typeName);
		}
		else {
			this.typeNames.remove(paramName);
		}
	}

	/**
	 * Return the SQL type for the given parameter, if registered.
	 * @param paramName the name of the parameter
	 * @return the SQL type of the parameter,
	 * or {@code TYPE_UNKNOWN} if not registered
	 */
	@Override
	public int getSqlType(String paramName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		return this.sqlTypes.getOrDefault(paramName, TYPE_UNKNOWN);
	}

	/**
	 * Return the type name for the given parameter, if registered.
	 * @param paramName the name of the parameter
	 * @return the type name of the parameter,
	 * or {@code null} if not registered
	 */
	@Override
	public @Nullable String getTypeName(String paramName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		return this.typeNames.get(paramName);
	}


	/**
	 * Enumerate the parameter names and values with their corresponding SQL type if available,
	 * or just return the simple {@code SqlParameterSource} implementation class name otherwise.
	 * @since 5.2
	 * @see #getParameterNames()
	 */
	@Override
	public String toString() {
		String[] parameterNames = getParameterNames();
		if (parameterNames != null) {
			StringJoiner result = new StringJoiner(", ", getClass().getSimpleName() + " {", "}");
			for (String parameterName : parameterNames) {
				Object value = getValue(parameterName);
				if (value instanceof SqlParameterValue sqlParameterValue) {
					value = sqlParameterValue.getValue();
				}
				String typeName = getTypeName(parameterName);
				if (typeName == null) {
					int sqlType = getSqlType(parameterName);
					if (sqlType != TYPE_UNKNOWN) {
						typeName = JdbcUtils.resolveTypeName(sqlType);
						if (typeName == null) {
							typeName = String.valueOf(sqlType);
						}
					}
				}
				StringBuilder entry = new StringBuilder();
				entry.append(parameterName).append('=').append(value);
				if (typeName != null) {
					entry.append(" (type:").append(typeName).append(')');
				}
				result.add(entry);
			}
			return result.toString();
		}
		else {
			return getClass().getSimpleName();
		}
	}

}
