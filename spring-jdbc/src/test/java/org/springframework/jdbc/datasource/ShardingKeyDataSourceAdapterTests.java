package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.ShardingKeyProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

public class ShardingKeyDataSourceAdapterTests {
	private final Connection connection = mock();
	private final Connection shardConnection = mock();
	private final DataSource dataSource = mock();
	private final ConnectionBuilder connectionBuilder = mock(ConnectionBuilder.class, RETURNS_DEEP_STUBS);
	private final ConnectionBuilder shardConnectionBuilder = mock(ConnectionBuilder.class, RETURNS_DEEP_STUBS);
	private final ShardingKey shardingKey = mock();
	private final ShardingKey superShardingKey = mock();
	private final ShardingKeyProvider shardingKeyProvider = new ShardingKeyProvider() {
		@Override
		public ShardingKey getShardingKey() throws SQLException {
			return shardingKey;
		}

		@Override
		public ShardingKey getSuperShardingKey() throws SQLException {
			return superShardingKey;
		}
	};

	@BeforeEach
	public void setUp() throws SQLException {
		given(dataSource.createConnectionBuilder()).willReturn(connectionBuilder);
		when(connectionBuilder.shardingKey(null).superShardingKey(null)).thenReturn(connectionBuilder);
		when(connectionBuilder.shardingKey(shardingKey).superShardingKey(superShardingKey))
				.thenReturn(shardConnectionBuilder);
	}

	@Test
	public void testGetConnectionNoKeyProvider() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);

		when(connectionBuilder.build()).thenReturn(connection);

		assertThat(dataSourceAdapter.getConnection()).isEqualTo(connection);
	}

	@Test
	public void testGetConnectionWithKeyProvider() throws SQLException {

		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(
				dataSource,
				shardingKeyProvider);

		when(shardConnectionBuilder.build()).thenReturn(shardConnection);

		assertThat(dataSourceAdapter.getConnection()).isEqualTo(shardConnection);
	}

	@Test
	public void testGetConnectionWithCredentialsNoKeyProvider() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);

		String username = "Anir";
		String password = "spring";

		Connection userConnection = mock();

		when(connectionBuilder.user(username).password(password).build()).thenReturn(userConnection);

		assertThat(dataSourceAdapter.getConnection(username, password)).isEqualTo(userConnection);
	}

	@Test
	public void testGetConnectionWithCredentialsAndKeyProvider() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(
				dataSource,
				shardingKeyProvider);

		String username = "mbekraou";
		String password = "jdbc";

		Connection userWithKeyProviderConnection = mock();

		when(shardConnectionBuilder.user(username).password(password).build())
				.thenReturn(userWithKeyProviderConnection);

		assertThat(dataSourceAdapter.getConnection(username, password)).isEqualTo(userWithKeyProviderConnection);
	}
}