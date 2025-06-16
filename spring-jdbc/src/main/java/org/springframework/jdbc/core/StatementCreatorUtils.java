/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.jdbc.core;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.SpringProperties;
import org.springframework.jdbc.support.SqlValue;

/**
 * Utility methods for PreparedStatementSetter/Creator and CallableStatementCreator
 * implementations, providing sophisticated parameter management (including support
 * for LOB values).
 *
 * <p>Used by PreparedStatementCreatorFactory and CallableStatementCreatorFactory,
 * but also available for direct use in custom setter/creator implementations.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see PreparedStatementSetter
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see PreparedStatementCreatorFactory
 * @see CallableStatementCreatorFactory
 * @see SqlParameter
 * @see SqlTypeValue
 * @see org.springframework.jdbc.core.support.SqlLobValue
 */
public abstract class StatementCreatorUtils {

	/**
	 * System property that instructs Spring to ignore {@link java.sql.ParameterMetaData#getParameterType}
	 * completely, i.e. to never even attempt to retrieve {@link PreparedStatement#getParameterMetaData()}
	 * for {@link StatementCreatorUtils#setNull} calls.
	 * <p>The default is "false", trying {@code getParameterType} calls first and falling back to
	 * {@link PreparedStatement#setNull} / {@link PreparedStatement#setObject} calls based on
	 * well-known behavior of common databases.
	 * <p>Consider switching this flag to "true" if you experience misbehavior at runtime,
	 * for example, with connection pool issues in case of an exception thrown from {@code getParameterType}
	 * (as reported on JBoss AS 7) or in case of performance problems (as reported on PostgreSQL).
	 */
	public static final String IGNORE_GETPARAMETERTYPE_PROPERTY_NAME = "spring.jdbc.getParameterType.ignore";


	private static final Log logger = LogFactory.getLog(StatementCreatorUtils.class);

	private static final Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<>(64);

	static @Nullable Boolean shouldIgnoreGetParameterType = SpringProperties.checkFlag(IGNORE_GETPARAMETERTYPE_PROPERTY_NAME);

	static {
		javaTypeToSqlTypeMap.put(boolean.class, Types.BOOLEAN);
		javaTypeToSqlTypeMap.put(Boolean.class, Types.BOOLEAN);
		javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
		javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
		javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
		javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
		javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
		javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
		javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
		javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
		javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
		javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
		javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
		javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
		javaTypeToSqlTypeMap.put(LocalDate.class, Types.DATE);
		javaTypeToSqlTypeMap.put(LocalTime.class, Types.TIME);
		javaTypeToSqlTypeMap.put(LocalDateTime.class, Types.TIMESTAMP);
		javaTypeToSqlTypeMap.put(OffsetTime.class, Types.TIME_WITH_TIMEZONE);
		javaTypeToSqlTypeMap.put(OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
		javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
		javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
		javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
		javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
		javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
	}


	/**
	 * Derive a default SQL type from the given Java type.
	 * @param javaType the Java type to translate
	 * @return the corresponding SQL type, or {@link SqlTypeValue#TYPE_UNKNOWN} if none found
	 */
	public static int javaTypeToSqlParameterType(@Nullable Class<?> javaType) {
		if (javaType == null) {
			return SqlTypeValue.TYPE_UNKNOWN;
		}
		Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
		if (sqlType != null) {
			return sqlType;
		}
		if (Number.class.isAssignableFrom(javaType)) {
			return Types.NUMERIC;
		}
		if (isStringValue(javaType)) {
			return Types.VARCHAR;
		}
		if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
			return Types.TIMESTAMP;
		}
		return SqlTypeValue.TYPE_UNKNOWN;
	}

	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param param the parameter as it is declared including type
	 * @param inValue the value to set
	 * @throws SQLException if thrown by PreparedStatement methods
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, SqlParameter param,
			@Nullable Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param.getScale(), inValue);
	}

	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param sqlType the SQL type of the parameter
	 * @param inValue the value to set (plain value or an SqlTypeValue)
	 * @throws SQLException if thrown by PreparedStatement methods
	 * @see SqlTypeValue
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
	}

	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param sqlType the SQL type of the parameter
	 * @param typeName the type name of the parameter
	 * (optional, only used for SQL NULL and SqlTypeValue)
	 * @param inValue the value to set (plain value or an SqlTypeValue)
	 * @throws SQLException if thrown by PreparedStatement methods
	 * @see SqlTypeValue
	 */
	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName,
			@Nullable Object inValue) throws SQLException {

		setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
	}

	/**
	 * Set the value for a parameter. The method used is based on the SQL type
	 * of the parameter and we can handle complex types like arrays and LOBs.
	 * @param ps the prepared statement or callable statement
	 * @param paramIndex index of the parameter we are setting
	 * @param sqlType the SQL type of the parameter
	 * @param typeName the type name of the parameter
	 * (optional, only used for SQL NULL and SqlTypeValue)
	 * @param scale the number of digits after the decimal point
	 * (for DECIMAL and NUMERIC types)
	 * @param inValue the value to set (plain value or an SqlTypeValue)
	 * @throws SQLException if thrown by PreparedStatement methods
	 * @see SqlTypeValue
	 */
	private static void setParameterValueInternal(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable String typeName, @Nullable Integer scale, @Nullable Object inValue) throws SQLException {

		String typeNameToUse = typeName;
		int sqlTypeToUse = sqlType;
		Object inValueToUse = inValue;

		// override type info?
		if (inValue instanceof SqlParameterValue parameterValue) {
			if (logger.isDebugEnabled()) {
				logger.debug("Overriding type info with runtime info from SqlParameterValue: column index " + paramIndex +
						", SQL type " + parameterValue.getSqlType() + ", type name " + parameterValue.getTypeName());
			}
			if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
				sqlTypeToUse = parameterValue.getSqlType();
			}
			if (parameterValue.getTypeName() != null) {
				typeNameToUse = parameterValue.getTypeName();
			}
			inValueToUse = parameterValue.getValue();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Setting SQL statement parameter value: column index " + paramIndex +
					", parameter value [" + inValueToUse +
					"], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") +
					"], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
		}

		if (inValueToUse == null) {
			setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
		}
		else {
			setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
		}
	}

	/**
	 * Set the specified PreparedStatement parameter to null,
	 * respecting database-specific peculiarities.
	 */
	private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException {

		if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER && typeName == null)) {
			boolean callGetParameterType = false;
			boolean useSetObject = false;
			Integer sqlTypeToUse = null;
			if (shouldIgnoreGetParameterType != null) {
				callGetParameterType = !shouldIgnoreGetParameterType;
			}
			else {
				String jdbcDriverName = ps.getConnection().getMetaData().getDriverName();
				if (jdbcDriverName.startsWith("PostgreSQL")) {
					sqlTypeToUse = Types.NULL;
				}
				else if (jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server")) {
					sqlTypeToUse = Types.NULL;
					useSetObject = true;
				}
				else {
					callGetParameterType = true;
				}
			}
			if (callGetParameterType) {
				try {
					sqlTypeToUse = ps.getParameterMetaData().getParameterType(paramIndex);
				}
				catch (SQLException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("JDBC getParameterType call failed - using fallback method instead: " + ex);
					}
				}
			}
			if (sqlTypeToUse == null) {
				// Proceed with database-specific checks
				sqlTypeToUse = Types.NULL;
				DatabaseMetaData dbmd = ps.getConnection().getMetaData();
				String jdbcDriverName = dbmd.getDriverName();
				String databaseProductName = dbmd.getDatabaseProductName();
				if (databaseProductName.startsWith("Informix") ||
						(jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server"))) {
						// "Microsoft SQL Server JDBC Driver 3.0" versus "Microsoft JDBC Driver 4.0 for SQL Server"
					useSetObject = true;
				}
				else if (databaseProductName.startsWith("DB2") ||
						jdbcDriverName.startsWith("jConnect") ||
						jdbcDriverName.startsWith("SQLServer") ||
						jdbcDriverName.startsWith("Apache Derby")) {
					sqlTypeToUse = Types.VARCHAR;
				}
			}
			if (useSetObject) {
				ps.setObject(paramIndex, null);
			}
			else {
				ps.setNull(paramIndex, sqlTypeToUse);
			}
		}
		else if (typeName != null) {
			ps.setNull(paramIndex, sqlType, typeName);
		}
		else {
			// Fall back to generic setNull call.
			try {
				// Try generic setNull call with SQL type specified.
				ps.setNull(paramIndex, sqlType);
			}
			catch (SQLFeatureNotSupportedException ex) {
				if (sqlType == Types.NULL) {
					throw ex;
				}
				// Fall back to generic setNull call without SQL type specified
				// (for example, for MySQL TIME_WITH_TIMEZONE / TIMESTAMP_WITH_TIMEZONE).
				ps.setNull(paramIndex, Types.NULL);
			}
		}
	}

	private static void setValue(PreparedStatement ps, int paramIndex, int sqlType,
			@Nullable String typeName, @Nullable Integer scale, Object inValue) throws SQLException {

		if (inValue instanceof SqlTypeValue sqlTypeValue) {
			sqlTypeValue.setTypeValue(ps, paramIndex, sqlType, typeName);
		}
		else if (inValue instanceof SqlValue sqlValue) {
			sqlValue.setValue(ps, paramIndex);
		}
		else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR ) {
			ps.setString(paramIndex, inValue.toString());
		}
		else if (sqlType == Types.NVARCHAR || sqlType == Types.LONGNVARCHAR) {
			ps.setNString(paramIndex, inValue.toString());
		}
		else if ((sqlType == Types.CLOB || sqlType == Types.NCLOB) && isStringValue(inValue.getClass())) {
			String strVal = inValue.toString();
			if (strVal.length() > 4000) {
				// Necessary for older Oracle drivers, in particular when running against an Oracle 10 database.
				// Should also work fine against other drivers/databases since it uses standard JDBC 4.0 API.
				if (sqlType == Types.NCLOB) {
					ps.setNClob(paramIndex, new StringReader(strVal), strVal.length());
				}
				else {
					ps.setClob(paramIndex, new StringReader(strVal), strVal.length());
				}
			}
			else {
				// Fallback: setString or setNString binding
				if (sqlType == Types.NCLOB) {
					ps.setNString(paramIndex, strVal);
				}
				else {
					ps.setString(paramIndex, strVal);
				}
			}
		}
		else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
			if (inValue instanceof BigDecimal bigDecimal) {
				ps.setBigDecimal(paramIndex, bigDecimal);
			}
			else if (scale != null) {
				ps.setObject(paramIndex, inValue, sqlType, scale);
			}
			else {
				ps.setObject(paramIndex, inValue, sqlType);
			}
		}
		else if (sqlType == Types.BOOLEAN) {
			if (inValue instanceof Boolean flag) {
				ps.setBoolean(paramIndex, flag);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.BOOLEAN);
			}
		}
		else if (sqlType == Types.DATE) {
			if (inValue instanceof java.util.Date date) {
				if (inValue instanceof java.sql.Date sqlDate) {
					ps.setDate(paramIndex, sqlDate);
				}
				else {
					ps.setDate(paramIndex, new java.sql.Date(date.getTime()));
				}
			}
			else if (inValue instanceof Calendar cal) {
				ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.DATE);
			}
		}
		else if (sqlType == Types.TIME) {
			if (inValue instanceof java.util.Date date) {
				if (inValue instanceof java.sql.Time time) {
					ps.setTime(paramIndex, time);
				}
				else {
					ps.setTime(paramIndex, new java.sql.Time(date.getTime()));
				}
			}
			else if (inValue instanceof Calendar cal) {
				ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.TIME);
			}
		}
		else if (sqlType == Types.TIMESTAMP) {
			if (inValue instanceof java.util.Date date) {
				if (inValue instanceof java.sql.Timestamp timestamp) {
					ps.setTimestamp(paramIndex, timestamp);
				}
				else {
					ps.setTimestamp(paramIndex, new java.sql.Timestamp(date.getTime()));
				}
			}
			else if (inValue instanceof Calendar cal) {
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
			}
			else {
				ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
			}
		}
		else if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER &&
				"Oracle".equals(ps.getConnection().getMetaData().getDatabaseProductName()))) {
			if (inValue instanceof byte[] bytes) {
				ps.setBytes(paramIndex, bytes);
			}
			else if (isStringValue(inValue.getClass())) {
				ps.setString(paramIndex, inValue.toString());
			}
			else if (isDateValue(inValue.getClass())) {
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
			}
			else if (inValue instanceof Calendar cal) {
				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
			}
			else {
				// Fall back to generic setObject call without SQL type specified.
				ps.setObject(paramIndex, inValue);
			}
		}
		else {
			// Fall back to generic setObject call.
			try {
				// Try generic setObject call with SQL type specified.
				ps.setObject(paramIndex, inValue, sqlType);
			}
			catch (SQLFeatureNotSupportedException ex) {
				// Fall back to generic setObject call without SQL type specified
				// (for example, for MySQL TIME_WITH_TIMEZONE / TIMESTAMP_WITH_TIMEZONE).
				ps.setObject(paramIndex, inValue);
			}
		}
	}

	/**
	 * Check whether the given value can be treated as a String value.
	 */
	private static boolean isStringValue(Class<?> inValueType) {
		// Consider any CharSequence (including StringBuffer and StringBuilder) as a String.
		return (CharSequence.class.isAssignableFrom(inValueType) ||
				StringWriter.class.isAssignableFrom(inValueType));
	}

	/**
	 * Check whether the given value is a {@code java.util.Date}
	 * (but not one of the JDBC-specific subclasses).
	 */
	private static boolean isDateValue(Class<?> inValueType) {
		return (java.util.Date.class.isAssignableFrom(inValueType) &&
				!(java.sql.Date.class.isAssignableFrom(inValueType) ||
						java.sql.Time.class.isAssignableFrom(inValueType) ||
						java.sql.Timestamp.class.isAssignableFrom(inValueType)));
	}

	/**
	 * Clean up all resources held by parameter values which were passed to an
	 * execute method. This is for example important for closing LOB values.
	 * @param paramValues parameter values supplied. May be {@code null}.
	 * @see DisposableSqlTypeValue#cleanup()
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 */
	public static void cleanupParameters(@Nullable Object @Nullable ... paramValues) {
		if (paramValues != null) {
			cleanupParameters(Arrays.asList(paramValues));
		}
	}

	/**
	 * Clean up all resources held by parameter values which were passed to an
	 * execute method. This is for example important for closing LOB values.
	 * @param paramValues parameter values supplied. May be {@code null}.
	 * @see DisposableSqlTypeValue#cleanup()
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 */
	public static void cleanupParameters(@Nullable Collection<?> paramValues) {
		if (paramValues != null) {
			for (Object inValue : paramValues) {
				// Unwrap SqlParameterValue first...
				if (inValue instanceof SqlParameterValue sqlParameterValue) {
					inValue = sqlParameterValue.getValue();
				}
				// Check for disposable value types
				if (inValue instanceof SqlValue sqlValue) {
					sqlValue.cleanup();
				}
				else if (inValue instanceof DisposableSqlTypeValue disposableSqlTypeValue) {
					disposableSqlTypeValue.cleanup();
				}
			}
		}
	}

}
