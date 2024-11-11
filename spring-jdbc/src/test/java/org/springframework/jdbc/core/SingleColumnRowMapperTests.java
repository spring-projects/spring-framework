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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.TypeMismatchDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SingleColumnRowMapper}.
 *
 * @author Kazuki Shimizu
 * @since 5.0.4
 */
class SingleColumnRowMapperTests {

	@Test  // SPR-16483
	public void useDefaultConversionService() throws SQLException {
		Timestamp timestamp = new Timestamp(0);

		SingleColumnRowMapper<LocalDateTime> rowMapper = SingleColumnRowMapper.newInstance(LocalDateTime.class);

		ResultSet resultSet = mock();
		ResultSetMetaData metaData = mock();
		given(metaData.getColumnCount()).willReturn(1);
		given(resultSet.getMetaData()).willReturn(metaData);
		given(resultSet.getObject(1, LocalDateTime.class))
				.willThrow(new SQLFeatureNotSupportedException());
		given(resultSet.getTimestamp(1)).willReturn(timestamp);

		LocalDateTime actualLocalDateTime = rowMapper.mapRow(resultSet, 1);

		assertThat(actualLocalDateTime).isEqualTo(timestamp.toLocalDateTime());
	}

	@Test  // SPR-16483
	public void useCustomConversionService() throws SQLException {
		Timestamp timestamp = new Timestamp(0);

		DefaultConversionService myConversionService = new DefaultConversionService();
		myConversionService.addConverter(Timestamp.class, MyLocalDateTime.class,
				source -> new MyLocalDateTime(source.toLocalDateTime()));
		SingleColumnRowMapper<MyLocalDateTime> rowMapper =
				SingleColumnRowMapper.newInstance(MyLocalDateTime.class, myConversionService);

		ResultSet resultSet = mock();
		ResultSetMetaData metaData = mock();
		given(metaData.getColumnCount()).willReturn(1);
		given(resultSet.getMetaData()).willReturn(metaData);
		given(resultSet.getObject(1, MyLocalDateTime.class))
				.willThrow(new SQLFeatureNotSupportedException());
		given(resultSet.getObject(1)).willReturn(timestamp);

		MyLocalDateTime actualMyLocalDateTime = rowMapper.mapRow(resultSet, 1);

		assertThat(actualMyLocalDateTime).isNotNull();
		assertThat(actualMyLocalDateTime.value).isEqualTo(timestamp.toLocalDateTime());
	}

	@Test // SPR-16483
	public void doesNotUseConversionService() throws SQLException {
		SingleColumnRowMapper<LocalDateTime> rowMapper =
				SingleColumnRowMapper.newInstance(LocalDateTime.class, null);

		ResultSet resultSet = mock();
		ResultSetMetaData metaData = mock();
		given(metaData.getColumnCount()).willReturn(1);
		given(resultSet.getMetaData()).willReturn(metaData);
		given(resultSet.getObject(1, LocalDateTime.class))
				.willThrow(new SQLFeatureNotSupportedException());
		given(resultSet.getTimestamp(1)).willReturn(new Timestamp(0));
		assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
				rowMapper.mapRow(resultSet, 1));
	}


	private static class MyLocalDateTime {

		private final LocalDateTime value;

		public MyLocalDateTime(LocalDateTime value) {
			this.value = value;
		}
	}

}
