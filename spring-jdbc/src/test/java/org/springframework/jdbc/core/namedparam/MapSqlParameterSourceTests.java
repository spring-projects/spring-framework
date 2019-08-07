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

import org.junit.Test;

import org.springframework.jdbc.core.SqlParameterValue;

import static org.junit.Assert.*;

/**
 * @author Rick Evans
 * @author Arjen Poutsma
 */
public class MapSqlParameterSourceTests {

	@Test
	public void nullParameterValuesPassedToCtorIsOk() {
		new MapSqlParameterSource(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getValueChokesIfParameterIsNotPresent() {
		MapSqlParameterSource source = new MapSqlParameterSource();
		source.getValue("pechorin was right!");
	}

	@Test
	public void sqlParameterValueRegistersSqlType() {
		MapSqlParameterSource msps = new MapSqlParameterSource("FOO", new SqlParameterValue(2, "Foo"));
		assertEquals("Correct SQL Type not registered", 2, msps.getSqlType("FOO"));
		MapSqlParameterSource msps2 = new MapSqlParameterSource();
		msps2.addValues(msps.getValues());
		assertEquals("Correct SQL Type not registered", 2, msps2.getSqlType("FOO"));
	}

}
