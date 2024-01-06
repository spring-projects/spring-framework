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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.RETURNS_DEEP_STUBS;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link ShardingKeyDataSourceAdapter}.
 *
 * @author Mohamed Lahyane (Anir)
 * @author Juergen Hoeller
 * @since 6.1.2
 */
class ShardingKeyDataSourceAdapterTests {

	private final Connection connection = mock();

	private final Connection shardConnection = mock();

	private final DataSource dataSource = mock();

	private final ConnectionBuilder connectionBuilder = mock(ConnectionBuilder.class, RETURNS_DEEP_STUBS);

	private final ConnectionBuilder shardConnectionBuilder = mock(ConnectionBuilder.class, RETURNS_DEEP_STUBS);

	private final ShardingKey shardingKey = mock();

	private final ShardingKey superShardingKey = mock();

	private final ShardingKeyProvider shardingKeyProvider = new ShardingKeyProvider() {
		@Override
		public ShardingKey getShardingKey() {
			return shardingKey;
		}
		@Override
		public ShardingKey getSuperShardingKey() {
			return superShardingKey;
		}
	};


	@BeforeEach
	void setup() throws SQLException {
		given(dataSource.createConnectionBuilder()).willReturn(connectionBuilder);
		when(connectionBuilder.shardingKey(null).superShardingKey(null)).thenReturn(connectionBuilder);
		when(connectionBuilder.shardingKey(shardingKey).superShardingKey(superShardingKey))
				.thenReturn(shardConnectionBuilder);
	}


	@Test
	void getConnectionNoKeyProvider() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);

		when(connectionBuilder.build()).thenReturn(connection);

		assertThat(dataSourceAdapter.getConnection()).isEqualTo(connection);
	}

	@Test
	void getConnectionWithKeyProvider() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter =
				new ShardingKeyDataSourceAdapter(dataSource, shardingKeyProvider);

		when(shardConnectionBuilder.build()).thenReturn(shardConnection);

		assertThat(dataSourceAdapter.getConnection()).isEqualTo(shardConnection);
	}

	@Test
	void getConnectionWithCredentialsNoKeyProvider() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);

		String username = "Anir";
		String password = "spring";

		when(connectionBuilder.user(username).password(password).build()).thenReturn(connection);

		assertThat(dataSourceAdapter.getConnection(username, password)).isEqualTo(connection);
	}

	@Test
	void getConnectionWithCredentialsAndKeyProvider() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter =
				new ShardingKeyDataSourceAdapter(dataSource, shardingKeyProvider);

		String username = "mbekraou";
		String password = "jdbc";

		when(shardConnectionBuilder.user(username).password(password).build()).thenReturn(connection);

		assertThat(dataSourceAdapter.getConnection(username, password)).isEqualTo(connection);
	}

}
