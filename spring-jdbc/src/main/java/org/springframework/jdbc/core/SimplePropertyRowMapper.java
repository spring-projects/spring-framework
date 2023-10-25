/*
 * Copyright 2002-2023 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RowMapper} implementation that converts a row into a new instance
 * of the specified mapped target class. The mapped target class must be a
 * top-level class or {@code static} nested class, and it may expose either a
 * <em>data class</em> constructor with named parameters corresponding to column
 * names or classic bean property setter methods with property names corresponding
 * to column names or fields with corresponding field names.
 *
 * <p>When combining a data class constructor with setter methods, any property
 * mapped successfully via a constructor argument will not be mapped additionally
 * via a corresponding setter method or field mapping. This means that constructor
 * arguments take precedence over property setter methods which in turn take
 * precedence over direct field mappings.
 *
 * <p>To facilitate mapping between columns and properties that don't have matching
 * names, try using underscore-separated column aliases in the SQL statement like
 * {@code "select fname as first_name from customer"}, where {@code first_name}
 * can be mapped to a {@code setFirstName(String)} method in the target class.
 *
 * <p>This is a flexible alternative to {@link DataClassRowMapper} and
 * {@link BeanPropertyRowMapper} for scenarios where no specific customization
 * and no pre-defined property mappings are needed.
 *
 * <p>In terms of its fallback property discovery algorithm, this class is similar to
 * {@link org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource}
 * and is similarly used for {@link org.springframework.jdbc.core.simple.JdbcClient}.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @param <T> the result type
 * @see DataClassRowMapper
 * @see BeanPropertyRowMapper
 * @see org.springframework.jdbc.core.simple.JdbcClient.StatementSpec#query(Class)
 * @see org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource
 */
public class SimplePropertyRowMapper<T> implements RowMapper<T> {

	private static final Object NO_DESCRIPTOR = new Object();

	private final Class<T> mappedClass;

	private final ConversionService conversionService;

	private final Constructor<T> mappedConstructor;

	private final String[] constructorParameterNames;

	private final TypeDescriptor[] constructorParameterTypes;

	private final Map<String, Object> propertyDescriptors = new ConcurrentHashMap<>();


	/**
	 * Create a new {@code SimplePropertyRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 */
	public SimplePropertyRowMapper(Class<T> mappedClass) {
		this(mappedClass, DefaultConversionService.getSharedInstance());
	}

	/**
	 * Create a new {@code SimplePropertyRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 * @param conversionService a {@link ConversionService} for binding
	 * JDBC values to bean properties
	 */
	public SimplePropertyRowMapper(Class<T> mappedClass, ConversionService conversionService) {
		Assert.notNull(mappedClass, "Mapped Class must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.mappedClass = mappedClass;
		this.conversionService = conversionService;

		this.mappedConstructor = BeanUtils.getResolvableConstructor(mappedClass);
		int paramCount = this.mappedConstructor.getParameterCount();
		this.constructorParameterNames = (paramCount > 0 ?
				BeanUtils.getParameterNames(this.mappedConstructor) : new String[0]);
		this.constructorParameterTypes = new TypeDescriptor[paramCount];
		for (int i = 0; i < paramCount; i++) {
			this.constructorParameterTypes[i] = new TypeDescriptor(new MethodParameter(this.mappedConstructor, i));
		}
	}


	@Override
	public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
		Object[] args = new Object[this.constructorParameterNames.length];
		Set<Integer> usedIndex = new HashSet<>();
		for (int i = 0; i < args.length; i++) {
			String name = this.constructorParameterNames[i];
			int index;
			try {
				// Try direct name match first
				index = rs.findColumn(name);
			}
			catch (SQLException ex) {
				// Try underscored name match instead
				index = rs.findColumn(JdbcUtils.convertPropertyNameToUnderscoreName(name));
			}
			TypeDescriptor td = this.constructorParameterTypes[i];
			Object value = JdbcUtils.getResultSetValue(rs, index, td.getType());
			usedIndex.add(index);
			args[i] = this.conversionService.convert(value, td);
		}
		T mappedObject = BeanUtils.instantiateClass(this.mappedConstructor, args);

		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		for (int index = 1; index <= columnCount; index++) {
			if (!usedIndex.contains(index)) {
				Object desc = getDescriptor(JdbcUtils.lookupColumnName(rsmd, index));
				if (desc instanceof MethodParameter mp) {
					Method method = mp.getMethod();
					if (method != null) {
						Object value = JdbcUtils.getResultSetValue(rs, index, mp.getParameterType());
						value = this.conversionService.convert(value, new TypeDescriptor(mp));
						ReflectionUtils.makeAccessible(method);
						ReflectionUtils.invokeMethod(method, mappedObject, value);
					}
				}
				else if (desc instanceof Field field) {
					Object value = JdbcUtils.getResultSetValue(rs, index, field.getType());
					value = this.conversionService.convert(value, new TypeDescriptor(field));
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, mappedObject, value);
				}
			}
		}

		return mappedObject;
	}

	private Object getDescriptor(String column) {
		return this.propertyDescriptors.computeIfAbsent(column, name -> {

			// Try direct match first
			PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.mappedClass, name);
			if (pd != null && pd.getWriteMethod() != null) {
				return BeanUtils.getWriteMethodParameter(pd);
			}
			Field field = ReflectionUtils.findField(this.mappedClass, name);
			if (field != null) {
				return field;
			}

			// Try de-underscored match instead
			String adaptedName = JdbcUtils.convertUnderscoreNameToPropertyName(name);
			if (!adaptedName.equals(name)) {
				pd = BeanUtils.getPropertyDescriptor(this.mappedClass, adaptedName);
				if (pd != null && pd.getWriteMethod() != null) {
					return BeanUtils.getWriteMethodParameter(pd);
				}
				field = ReflectionUtils.findField(this.mappedClass, adaptedName);
				if (field != null) {
					return field;
				}
			}

			// Fallback: case-insensitive match
			PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(this.mappedClass);
			for (PropertyDescriptor candidate : pds) {
				if (name.equalsIgnoreCase(candidate.getName())) {
					return BeanUtils.getWriteMethodParameter(candidate);
				}
			}
			field = ReflectionUtils.findFieldIgnoreCase(this.mappedClass, name);
			if (field != null) {
				return field;
			}

			return NO_DESCRIPTOR;
		});
	}

}
