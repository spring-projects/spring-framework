/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
