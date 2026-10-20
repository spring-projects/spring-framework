/*
 * Copyright 2002-present the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SqlParameterSourceUtils}.
 */
class SqlParameterSourceUtilsTests {

	@Test
	void createBatchWithMapBeanAndSqlParameterSource() {
		MapSqlParameterSource source = new MapSqlParameterSource("name", "source");
		SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(
				List.of(Map.of("name", "map"), new TestBean("bean"), source));

		assertThat(batch).hasSize(3);
		assertThat(batch[0]).isInstanceOf(MapSqlParameterSource.class);
		assertThat(batch[0].getValue("name")).isEqualTo("map");
		assertThat(batch[1]).isInstanceOf(BeanPropertySqlParameterSource.class);
		assertThat(batch[1].getValue("name")).isEqualTo("bean");
		assertThat(batch[2]).isSameAs(source);
	}

	@Test
	void createBatchWithSqlParameterSourceVarargs() {
		MapSqlParameterSource source = new MapSqlParameterSource("name", "source");
		SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(source);

		assertThat(batch).containsExactly(source);
	}

}
