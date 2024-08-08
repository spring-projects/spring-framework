/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import io.r2dbc.spi.Parameter;
import io.r2dbc.spi.Parameters;

import org.springframework.util.Assert;

/**
 * {@link BindParameterSource} implementation that holds a given {@link Map} of parameters
 * encapsulated as {@link Parameter}.
 *
 * <p>This class is intended for passing in a simple Map of parameter values to the methods
 * of the {@code NamedParameterExpander} class.
 *
 * @author Mark Paluch
 * @since 5.3
 */
class MapBindParameterSource implements BindParameterSource {

	private final Map<String, Parameter> values;


	/**
	 * Create a new empty {@link MapBindParameterSource}.
	 */
	MapBindParameterSource() {
		this.values = new LinkedHashMap<>();
	}

	/**
	 * Creates a new {@link MapBindParameterSource} given {@link Map} of {@link Parameter}.
	 * @param values the parameter mapping
	 */
	MapBindParameterSource(Map<String, Parameter> values) {
		Assert.notNull(values, "Values must not be null");
		this.values = values;
	}


	/**
	 * Add a key-value pair to the {@link MapBindParameterSource}.
	 * @param paramName the name of the parameter
	 * @param value the parameter value to add (must not be {@code null})
	 */
	MapBindParameterSource addValue(String paramName, Object value) {
		Assert.notNull(paramName, "Parameter name must not be null");
		Assert.notNull(value, "Value must not be null");
		this.values.put(paramName, Parameters.in(value));
		return this;
	}

	@Override
	public boolean hasValue(String paramName) {
		Assert.notNull(paramName, "Parameter name must not be null");
		return this.values.containsKey(paramName);
	}

	@Override
	@SuppressWarnings("NullAway")
	public Parameter getValue(String paramName) throws IllegalArgumentException {
		if (!hasValue(paramName)) {
			throw new IllegalArgumentException("No value registered for key '" + paramName + "'");
		}
		return this.values.get(paramName);
	}

	@Override
	public Iterable<String> getParameterNames() {
		return this.values.keySet();
	}

}
