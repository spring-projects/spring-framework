/*
 * Copyright 2002-present the original author or authors.
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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;

/**
 * {@link RowMapper} implementation that converts a single column into a single
 * result value per row. Expects to operate on a {@code java.sql.ResultSet}
 * that just contains a single column.
 *
 * <p>The type of the result value for each row can be specified. The value
 * for the single column will be extracted from the {@code ResultSet}
 * and converted into the specified target type.
 *
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @since 1.2
 * @param <T> the result type
 * @see JdbcTemplate#queryForList(String, Class)
 * @see JdbcTemplate#queryForObject(String, Class)
 */
public class SingleColumnRowMapper<T> implements RowMapper<T> {

	private @Nullable Class<?> requiredType;

	private @Nullable ConversionService conversionService = DefaultConversionService.getSharedInstance();


	/**
	 * Create a new {@code SingleColumnRowMapper} for bean-style configuration.
	 * @see #setRequiredType
	 */
	public SingleColumnRowMapper() {
	}

	/**
	 * Create a new {@code SingleColumnRowMapper}.
	 * @param requiredType the type that each result object is expected to match
	 */
	public SingleColumnRowMapper(Class<T> requiredType) {
		if (requiredType != Object.class) {
			setRequiredType(requiredType);
		}
	}

	/**
	 * Create a new {@code SingleColumnRowMapper}.
	 * @param requiredType the type that each result object is expected to match
	 * @param conversionService a {@link ConversionService} for converting a fetched value
	 * @since 7.0
	 */
	public SingleColumnRowMapper(Class<T> requiredType, @Nullable ConversionService conversionService) {
		if (requiredType != Object.class) {
			setRequiredType(requiredType);
		}
		setConversionService(conversionService);
	}


	/**
	 * Set the type that each result object is expected to match.
	 * <p>If not specified, the column value will be exposed as
	 * returned by the JDBC driver.
	 */
	public void setRequiredType(Class<T> requiredType) {
		this.requiredType = ClassUtils.resolvePrimitiveIfNecessary(requiredType);
	}

	/**
	 * Set a {@link ConversionService} for converting a fetched value.
	 * <p>Default is the {@link DefaultConversionService}.
	 * @since 5.0.4
	 * @see DefaultConversionService#getSharedInstance()
	 */
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	/**
	 * Extract a value for the single column in the current row.
	 * <p>Validates that there is only one column selected,
	 * then delegates to {@code getColumnValue()} and also
	 * {@code convertValueToRequiredType}, if necessary.
	 * @see java.sql.ResultSetMetaData#getColumnCount()
	 * @see #getColumnValue(java.sql.ResultSet, int, Class)
	 * @see #convertValueToRequiredType(Object, Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public @Nullable T mapRow(ResultSet rs, int rowNum) throws SQLException {
		// Validate column count.
		ResultSetMetaData rsmd = rs.getMetaData();
		int nrOfColumns = rsmd.getColumnCount();
		if (nrOfColumns != 1) {
			throw new IncorrectResultSetColumnCountException(1, nrOfColumns);
		}

		// Extract column value from JDBC ResultSet.
		Object result = getColumnValue(rs, 1, this.requiredType);
		if (result != null && this.requiredType != null && !this.requiredType.isInstance(result)) {
			// Extracted value does not match already: try to convert it.
			try {
				return (T) convertValueToRequiredType(result, this.requiredType);
			}
			catch (IllegalArgumentException ex) {
				throw new TypeMismatchDataAccessException(
						"Type mismatch affecting row number " + rowNum + " and column type '" +
						rsmd.getColumnTypeName(1) + "': " + ex.getMessage());
			}
		}
		return (T) result;
	}

	/**
	 * Retrieve a JDBC object value for the specified column.
	 * <p>The default implementation calls
	 * {@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}.
	 * If no required type has been specified, this method delegates to
	 * {@code getColumnValue(rs, index)}, which basically calls
	 * {@code ResultSet.getObject(index)} but applies some additional
	 * default conversion to appropriate value types.
	 * @param rs is the ResultSet holding the data
	 * @param index is the column index
	 * @param requiredType the type that each result object is expected to match
	 * (or {@code null} if none specified)
	 * @return the Object value
	 * @throws SQLException in case of extraction failure
	 * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)
	 * @see #getColumnValue(java.sql.ResultSet, int)
	 */
	protected @Nullable Object getColumnValue(ResultSet rs, int index, @Nullable Class<?> requiredType) throws SQLException {
		if (requiredType != null) {
			return JdbcUtils.getResultSetValue(rs, index, requiredType);
		}
		else {
			// No required type specified -> perform default extraction.
			return getColumnValue(rs, index);
		}
	}

	/**
	 * Retrieve a JDBC object value for the specified column, using the most
	 * appropriate value type. Called if no required type has been specified.
	 * <p>The default implementation delegates to {@code JdbcUtils.getResultSetValue()},
	 * which uses the {@code ResultSet.getObject(index)} method. Additionally,
	 * it includes a "hack" to get around Oracle returning a non-standard object for
	 * their TIMESTAMP datatype. See the {@code JdbcUtils#getResultSetValue()}
	 * javadoc for details.
	 * @param rs is the ResultSet holding the data
	 * @param index is the column index
	 * @return the Object value
	 * @throws SQLException in case of extraction failure
	 * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int)
	 */
	protected @Nullable Object getColumnValue(ResultSet rs, int index) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index);
	}

	/**
	 * Convert the given column value to the specified required type.
	 * Only called if the extracted column value does not match already.
	 * <p>If the required type is String, the value will simply get stringified
	 * via {@code toString()}. In case of a Number, the value will be
	 * converted into a Number, either through number conversion or through
	 * String parsing (depending on the value type). Otherwise, the value will
	 * be converted to a required type using the {@link ConversionService}.
	 * @param value the column value as extracted from {@code getColumnValue()}
	 * (never {@code null})
	 * @param requiredType the type that each result object is expected to match
	 * (never {@code null})
	 * @return the converted value
	 * @see #getColumnValue(java.sql.ResultSet, int, Class)
	 */
	@SuppressWarnings("unchecked")
	protected @Nullable Object convertValueToRequiredType(Object value, Class<?> requiredType) {
		if (String.class == requiredType) {
			return value.toString();
		}
		else if (Number.class.isAssignableFrom(requiredType)) {
			if (value instanceof Number number) {
				// Convert original Number to target Number class.
				return NumberUtils.convertNumberToTargetClass(number, (Class<Number>) requiredType);
			}
			else {
				// Convert stringified value to target Number class.
				return NumberUtils.parseNumber(value.toString(),(Class<Number>) requiredType);
			}
		}
		else if (this.conversionService != null && this.conversionService.canConvert(value.getClass(), requiredType)) {
			return this.conversionService.convert(value, requiredType);
		}
		else {
			throw new IllegalArgumentException(
					"Value [" + value + "] is of type [" + value.getClass().getName() +
					"] and cannot be converted to required type [" + requiredType.getName() + "]");
		}
	}


	/**
	 * Static factory method to create a new {@code SingleColumnRowMapper}.
	 * @param requiredType the type that each result object is expected to match
	 * @since 4.1
	 * @see #newInstance(Class, ConversionService)
	 */
	public static <T> SingleColumnRowMapper<T> newInstance(Class<T> requiredType) {
		return new SingleColumnRowMapper<>(requiredType);
	}

	/**
	 * Static factory method to create a new {@code SingleColumnRowMapper}.
	 * @param requiredType the type that each result object is expected to match
	 * @param conversionService the {@link ConversionService} for converting a
	 * fetched value, or {@code null} for none
	 * @since 5.0.4
	 * @see #newInstance(Class)
	 * @see #setConversionService
	 */
	public static <T> SingleColumnRowMapper<T> newInstance(
			Class<T> requiredType, @Nullable ConversionService conversionService) {

		SingleColumnRowMapper<T> rowMapper = newInstance(requiredType);
		rowMapper.setConversionService(conversionService);
		return rowMapper;
	}

}
