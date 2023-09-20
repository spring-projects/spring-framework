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

package org.springframework.r2dbc.core;

import io.r2dbc.spi.Parameters;
import io.r2dbc.spi.Parameter;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.r2dbc.core.DatabaseClient.Params;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default {@link DatabaseClient.Params} implementation.
 *
 * @param byIndex index based bindings
 * @param byName  name based bindings
 * @author Mark Shiryaev
 * @since 6.1
 */
record DefaultParamsImpl(Map<Integer, Parameter> byIndex, Map<String, Parameter> byName) implements Params {
	public static final DefaultParamsImpl EMPTY_PARAMS = new DefaultParamsImpl(
			Collections.emptyMap(), Collections.emptyMap());

	@Override
	public Params bind(String name, Object value) {
		Assert.hasText(name, "Parameter name must not be null or empty");
		Assert.notNull(value, () -> String.format(
				"Value for parameter %s must not be null. Use bindNull(…) instead.", name));

		Map<String, Parameter> byName = new LinkedHashMap<>(this.byName);
		byName.put(name, resolveParameter(value));
		return new DefaultParamsImpl(this.byIndex, byName);
	}

	@Override
	public Params bind(int index, Object value) {
		Assert.notNull(value, () -> String.format(
				"Value at index %d must not be null. Use bindNull(…) instead.", index));

		Map<Integer, Parameter> byIndex = new LinkedHashMap<>(this.byIndex);
		byIndex.put(index, resolveParameter(value));

		return new DefaultParamsImpl(byIndex, this.byName);
	}

	@Override
	public Params bindNull(String name, Class<?> type) {
		Assert.hasText(name, "Parameter name must not be null or empty");

		Map<String, Parameter> byName = new LinkedHashMap<>(this.byName);
		byName.put(name, Parameters.in(type));

		return new DefaultParamsImpl(this.byIndex, byName);
	}

	@Override
	public Params bindNull(int index, Class<?> type) {
		Map<Integer, Parameter> byIndex = new LinkedHashMap<>(this.byIndex);
		byIndex.put(index, Parameters.in(type));

		return new DefaultParamsImpl(byIndex, this.byName);
	}

	@Override
	public Map<Integer, Parameter> byIndex() {
		return this.byIndex;
	}

	@Override
	public Map<String, Parameter> byName() {
		return this.byName;
	}

	@Override
	public Params bindValues(Map<String, ?> source) {
		Assert.notNull(source, "Parameter source must not be null");

		Map<String, Parameter> byName = new LinkedHashMap<>(this.byName);
		source.forEach((name, value) -> byName.put(name, resolveParameter(value)));

		return new DefaultParamsImpl(this.byIndex, byName);
	}

	@Override
	public Params bindProperties(Object source) {
		Assert.notNull(source, "Parameter source must not be null");

		Map<String, Parameter> byName = new LinkedHashMap<>(this.byName);
		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(source.getClass())) {
			if (pd.getReadMethod() != null && pd.getReadMethod().getDeclaringClass() != Object.class) {
				ReflectionUtils.makeAccessible(pd.getReadMethod());
				Object value = ReflectionUtils.invokeMethod(pd.getReadMethod(), source);
				byName.put(pd.getName(), (value != null ? Parameters.in(value) : Parameters.in(pd.getPropertyType())));
			}
		}

		return new DefaultParamsImpl(this.byIndex, byName);
	}

	@SuppressWarnings("deprecation")
	private Parameter resolveParameter(Object value) {
		if (value instanceof Parameter param) {
			return param;
		} else if (value instanceof org.springframework.r2dbc.core.Parameter param) {
			Object paramValue = param.getValue();
			return (paramValue != null ? Parameters.in(paramValue) : Parameters.in(param.getType()));
		} else {
			return Parameters.in(value);
		}
	}
}
