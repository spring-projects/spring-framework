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

package org.springframework.r2dbc.core;

import java.util.Locale;
import java.util.Map;

import io.r2dbc.spi.test.MockColumnMetadata;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockRowMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for R2DBC-based {@link ColumnMapRowMapper}.
 *
 * @author Sharang Gupta
 * @since 7.1
 */
class R2dbcColumnMapRowMapperTests {

	@Test
	void columnKeysAreCaseInsensitive() {
		Map<String, Object> row = ColumnMapRowMapper.INSTANCE.apply(ID_ROW, ID_ROW.getMetadata());

		assertThat(row).containsEntry("ID", 42);
		assertThat(row.get("id")).isEqualTo(42);
		assertThat(row.get("Id")).isEqualTo(42);
	}

	@Test  // "ID" vs "id" must keep matching when the default Locale folds 'I' to a dotless i
	void columnKeysAreCaseInsensitiveRegardlessOfDefaultLocale() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr-TR"));
		try {
			Map<String, Object> row = ColumnMapRowMapper.INSTANCE.apply(ID_ROW, ID_ROW.getMetadata());

			assertThat(row.get("id")).as("lower-case lookup under Turkish default Locale").isEqualTo(42);
			assertThat(row.containsKey("id")).isTrue();
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}


	private static final MockRow ID_ROW = MockRow.builder()
			.metadata(MockRowMetadata.builder()
					.columnMetadata(MockColumnMetadata.builder().name("ID").javaType(Integer.class).build())
					.build())
			.identified(0, Object.class, 42)
			.build();

}
