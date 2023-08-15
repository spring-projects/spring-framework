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

package org.springframework.jdbc.core.namedparam;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link SqlParameterSource} implementation that obtains parameter values
 * from bean properties of a given JavaBean object, from component accessors
 * of a record class, or from raw field access.
 *
 * <p>This is a more flexible variant of {@link BeanPropertySqlParameterSource},
 * with the limitation that it is not able to enumerate its
 * {@link #getParameterNames() parameter names}.
 *
 * <p>In terms of its fallback property discovery algorithm, this class is
 * similar to {@link org.springframework.validation.SimpleErrors} which is
 * also just used for property retrieval purposes (rather than binding).
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see NamedParameterJdbcTemplate
 * @see BeanPropertySqlParameterSource
 * @see org.springframework.jdbc.core.simple.JdbcClient.StatementSpec#paramSource(Object)
 * @see org.springframework.jdbc.core.SimplePropertyRowMapper
 */
public class SimplePropertySqlParameterSource extends AbstractSqlParameterSource {

	private static final Object NO_DESCRIPTOR = new Object();

	private final Object paramObject;

	private final Map<String, Object> propertyDescriptors = new ConcurrentHashMap<>();


	/**
	 * Create a new SqlParameterSource for the given bean, record or field holder.
	 * @param paramObject the bean, record or field holder instance to wrap
	 */
	public SimplePropertySqlParameterSource(Object paramObject) {
		Assert.notNull(paramObject, "Parameter object must not be null");
		this.paramObject = paramObject;
	}


	@Override
	public boolean hasValue(String paramName) {
		return (getDescriptor(paramName) != NO_DESCRIPTOR);
	}

	@Override
	@Nullable
	public Object getValue(String paramName) throws IllegalArgumentException {
		Object desc = getDescriptor(paramName);
		if (desc instanceof PropertyDescriptor pd) {
			ReflectionUtils.makeAccessible(pd.getReadMethod());
			return ReflectionUtils.invokeMethod(pd.getReadMethod(), this.paramObject);
		}
		else if (desc instanceof Field field) {
			ReflectionUtils.makeAccessible(field);
			return ReflectionUtils.getField(field, this.paramObject);
		}
		throw new IllegalArgumentException("Cannot retrieve value for parameter '" + paramName +
				"' - neither a getter method nor a raw field found");
	}

	/**
	 * Derives a default SQL type from the corresponding property type.
	 * @see StatementCreatorUtils#javaTypeToSqlParameterType
	 */
	@Override
	public int getSqlType(String paramName) {
		int sqlType = super.getSqlType(paramName);
		if (sqlType != TYPE_UNKNOWN) {
			return sqlType;
		}
		Object desc = getDescriptor(paramName);
		if (desc instanceof PropertyDescriptor pd) {
			return StatementCreatorUtils.javaTypeToSqlParameterType(pd.getPropertyType());
		}
		else if (desc instanceof Field field) {
			return StatementCreatorUtils.javaTypeToSqlParameterType(field.getType());
		}
		return TYPE_UNKNOWN;
	}

	private Object getDescriptor(String paramName) {
		return this.propertyDescriptors.computeIfAbsent(paramName, name -> {
			PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(this.paramObject.getClass(), name);
			if (pd != null && pd.getReadMethod() != null) {
				return pd;
			}
			Field field = ReflectionUtils.findField(this.paramObject.getClass(), name);
			if (field != null) {
				return field;
			}
			return NO_DESCRIPTOR;
		});
	}

}
