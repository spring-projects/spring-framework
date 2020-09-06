/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link SqlParameterSource} implementation that obtains parameter values
 * from bean properties of a given JavaBean object. The names of the bean
 * properties have to match the parameter names.
 *
 * <p>Uses a Spring BeanWrapper for bean property access underneath.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcTemplate
 * @see org.springframework.beans.BeanWrapper
 */
public class BeanPropertySqlParameterSource extends AbstractSqlParameterSource {

	private final BeanWrapper beanWrapper;

	@Nullable
	private String[] propertyNames;


	/**
	 * Create a new BeanPropertySqlParameterSource for the given bean.
	 * @param object the bean instance to wrap
	 */
	public BeanPropertySqlParameterSource(Object object) {
		this.beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
	}


	@Override
	public boolean hasValue(String paramName) {
		return this.beanWrapper.isReadableProperty(paramName);
	}

	@Override
	@Nullable
	public Object getValue(String paramName) throws IllegalArgumentException {
		try {
			return this.beanWrapper.getPropertyValue(paramName);
		}
		catch (NotReadablePropertyException ex) {
			throw new IllegalArgumentException(ex.getMessage());
		}
	}

	/**
	 * Derives a default SQL type from the corresponding property type.
	 * @see org.springframework.jdbc.core.StatementCreatorUtils#javaTypeToSqlParameterType
	 */
	@Override
	public int getSqlType(String paramName) {
		int sqlType = super.getSqlType(paramName);
		if (sqlType != TYPE_UNKNOWN) {
			return sqlType;
		}
		Class<?> propType = this.beanWrapper.getPropertyType(paramName);
		return StatementCreatorUtils.javaTypeToSqlParameterType(propType);
	}

	@Override
	@NonNull
	public String[] getParameterNames() {
		return getReadablePropertyNames();
	}

	/**
	 * Provide access to the property names of the wrapped bean.
	 * Uses support provided in the {@link PropertyAccessor} interface.
	 * @return an array containing all the known property names
	 */
	public String[] getReadablePropertyNames() {
		if (this.propertyNames == null) {
			List<String> names = new ArrayList<>();
			PropertyDescriptor[] props = this.beanWrapper.getPropertyDescriptors();
			for (PropertyDescriptor pd : props) {
				if (this.beanWrapper.isReadableProperty(pd.getName())) {
					names.add(pd.getName());
				}
			}
			this.propertyNames = StringUtils.toStringArray(names);
		}
		return this.propertyNames;
	}

}
