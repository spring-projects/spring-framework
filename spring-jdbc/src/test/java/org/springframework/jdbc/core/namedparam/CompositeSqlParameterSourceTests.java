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

import java.sql.Types;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CompositeSqlParameterSource}.
 *
 * @author Kazuki Shimizu
 * @since 5.2.2
 */
class CompositeSqlParameterSourceTests {

	@Test
	void withBeanPropertySqlParameterSource() {
		SqlParameterSource source = CompositeSqlParameterSource.of(
				new BeanPropertySqlParameterSource(new TestBean("tb", 99)));
		assertThat(source.getParameterNames()).contains("name", "age");
		assertThat(source.getParameterNames()).doesNotContain("foo");
		assertThat(source.hasValue("name")).isTrue();
		assertThat(source.hasValue("age")).isTrue();
		assertThat(source.hasValue("foo")).isFalse();
		assertThat(source.getValue("name")).isEqualTo("tb");
		assertThat(source.getValue("age")).isEqualTo(99);
		assertThat(source.getValue("foo")).isNull();
		assertThat(source.getSqlType("name")).isEqualTo(Types.VARCHAR);
		assertThat(source.getSqlType("age")).isEqualTo(Types.INTEGER);
		assertThat(source.getSqlType("foo")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
		assertThat(source.getTypeName("name")).isNull();
		assertThat(source.getTypeName("age")).isNull();
		assertThat(source.getTypeName("foo")).isNull();
	}

	@Test
	void withMapSqlParameterSource() {
		SqlParameterSource source = CompositeSqlParameterSource.of(new MapSqlParameterSource()
				.addValue("name", "tb").addValue("age", 99));
		assertThat(source.getParameterNames()).contains("name", "age");
		assertThat(source.getParameterNames()).doesNotContain("foo");
		assertThat(source.hasValue("name")).isTrue();
		assertThat(source.hasValue("age")).isTrue();
		assertThat(source.hasValue("foo")).isFalse();
		assertThat(source.getValue("name")).isEqualTo("tb");
		assertThat(source.getValue("age")).isEqualTo(99);
		assertThat(source.getValue("foo")).isNull();
		assertThat(source.getSqlType("name")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
		assertThat(source.getSqlType("age")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
		assertThat(source.getSqlType("foo")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
		assertThat(source.getTypeName("name")).isNull();
		assertThat(source.getTypeName("age")).isNull();
		assertThat(source.getTypeName("foo")).isNull();
	}

	@Test
	void withEmptySqlParameterSource() {
		SqlParameterSource source = CompositeSqlParameterSource.of(EmptySqlParameterSource.INSTANCE);
		assertThat(source.getParameterNames()).hasSize(0);
		assertThat(source.hasValue("foo")).isFalse();
		assertThat(source.getValue("foo")).isNull();
		assertThat(source.getSqlType("foo")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
		assertThat(source.getTypeName("foo")).isNull();
	}

	@Test
	void withMultipleSqlParameterSource() {
		SqlParameterSource source = CompositeSqlParameterSource.of(
				new BeanPropertySqlParameterSource(new TestBean("tb", 99)),
				new MapSqlParameterSource("operator", "op"));
		assertThat(source.getParameterNames()).contains("name", "age", "operator");
		assertThat(source.hasValue("name")).isTrue();
		assertThat(source.hasValue("age")).isTrue();
		assertThat(source.hasValue("operator")).isTrue();
		assertThat(source.getValue("name")).isEqualTo("tb");
		assertThat(source.getValue("age")).isEqualTo(99);
		assertThat(source.getValue("operator")).isEqualTo("op");
		assertThat(source.getSqlType("name")).isEqualTo(Types.VARCHAR);
		assertThat(source.getSqlType("age")).isEqualTo(Types.INTEGER);
		assertThat(source.getSqlType("operator")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
		assertThat(source.getTypeName("name")).isNull();
		assertThat(source.getTypeName("age")).isNull();
		assertThat(source.getTypeName("operator")).isNull();
	}

	@Test
	void withDuplicateParamNameOnMultipleSqlParameterSource() {
		SqlParameterSource source = CompositeSqlParameterSource.of(
				new MapSqlParameterSource("param", "1st"),
				new MapSqlParameterSource("param", "2nd"));
		assertThat(source.getParameterNames()).hasSize(1).contains("param");
		assertThat(source.hasValue("param")).isTrue();
		assertThat(source.getValue("param")).isEqualTo("1st");
	}

	@Test
	void withEmpty() {
		SqlParameterSource source = CompositeSqlParameterSource.of();
		assertThat(source.getParameterNames()).hasSize(0);
		assertThat(source.hasValue("foo")).isFalse();
		assertThat(source.getValue("foo")).isNull();
		assertThat(source.getSqlType("foo")).isEqualTo(JdbcUtils.TYPE_UNKNOWN);
		assertThat(source.getTypeName("foo")).isNull();
	}

}
