/*
 * Copyright 2002-2024 the original author or authors.
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

import java.sql.Types;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.support.JdbcUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class MapSqlParameterSourceTests {

	@Test
	void nullParameterValuesPassedToCtorIsOk() {
		new MapSqlParameterSource(null);
	}

	@Test
	void getValueChokesIfParameterIsNotPresent() {
		MapSqlParameterSource source = new MapSqlParameterSource();
		assertThatIllegalArgumentException().isThrownBy(() ->
				source.getValue("pechorin was right!"));
	}

	@Test
	void sqlParameterValueRegistersSqlType() {
		MapSqlParameterSource msps = new MapSqlParameterSource("FOO", new SqlParameterValue(Types.NUMERIC, "Foo"));
		assertThat(msps.getSqlType("FOO")).as("Correct SQL Type not registered").isEqualTo(2);
		MapSqlParameterSource msps2 = new MapSqlParameterSource();
		msps2.addValues(msps.getValues());
		assertThat(msps2.getSqlType("FOO")).as("Correct SQL Type not registered").isEqualTo(2);
	}

	@Test
	void toStringShowsParameterDetails() {
		MapSqlParameterSource source = new MapSqlParameterSource("FOO", new SqlParameterValue(Types.NUMERIC, "Foo"));
		assertThat(source.toString()).isEqualTo("MapSqlParameterSource {FOO=Foo (type:NUMERIC)}");
	}

	@Test
	void toStringShowsCustomSqlType() {
		MapSqlParameterSource source = new MapSqlParameterSource("FOO", new SqlParameterValue(Integer.MAX_VALUE, "Foo"));
		assertThat(source.toString()).isEqualTo(("MapSqlParameterSource {FOO=Foo (type:" + Integer.MAX_VALUE + ")}"));
	}

	@Test
	void toStringDoesNotShowTypeUnknown() {
		MapSqlParameterSource source = new MapSqlParameterSource("FOO", new SqlParameterValue(JdbcUtils.TYPE_UNKNOWN, "Foo"));
		assertThat(source.toString()).isEqualTo("MapSqlParameterSource {FOO=Foo}");
	}

}
