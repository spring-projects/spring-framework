/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.object;

import java.util.Map;

import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

public class GenericSqlQuery<T> extends SqlQuery<T> {

	Class<?> rowMapperClass;

	RowMapper<?> rowMapper;

	@SuppressWarnings("rawtypes")
	public void setRowMapperClass(Class<? extends RowMapper> rowMapperClass)
			throws IllegalAccessException, InstantiationException {
		this.rowMapperClass = rowMapperClass;
		if (!RowMapper.class.isAssignableFrom(rowMapperClass))
			throw new IllegalStateException("The specified class '" +
					rowMapperClass.getName() + " is not a sub class of " +
					"'org.springframework.jdbc.core.RowMapper'");
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(rowMapperClass, "The 'rowMapperClass' property is required");
	}

	@Override
	@SuppressWarnings("unchecked")
	protected RowMapper<T> newRowMapper(Object[] parameters, Map<?, ?> context) {
		try {
			return (RowMapper<T>) rowMapperClass.newInstance();
		}
		catch (InstantiationException e) {
			throw new InvalidDataAccessResourceUsageException("Unable to instantiate RowMapper", e);
		}
		catch (IllegalAccessException e) {
			throw new InvalidDataAccessResourceUsageException("Unable to instantiate RowMapper", e);
		}
	}
}
