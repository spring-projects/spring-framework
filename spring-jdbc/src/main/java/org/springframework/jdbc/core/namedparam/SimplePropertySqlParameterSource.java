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
import java.util.HashMap;
import java.util.Map;

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
 */
public class SimplePropertySqlParameterSource extends AbstractSqlParameterSource {

	private final Object paramObject;

	private final Map<String, Object> descriptorMap = new HashMap<>();


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
		return (getDescriptor(paramName) != null);
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

	@Nullable
	private Object getDescriptor(String paramName) {
		return this.descriptorMap.computeIfAbsent(paramName, name -> {
			Object pd = BeanUtils.getPropertyDescriptor(this.paramObject.getClass(), name);
			if (pd == null) {
				pd = ReflectionUtils.findField(this.paramObject.getClass(), name);
			}
			return pd;
		});
	}

}
