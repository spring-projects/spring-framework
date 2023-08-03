package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.ShardingKeyProvider;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

public class ShardingKeyDataSourceAdapterTests {
	private final Connection connection = mock();
	private final Connection shardConnection = mock();
	private final DataSource dataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);
	private final ConnectionBuilder connectionBuilder = mock(ConnectionBuilder.class, RETURNS_DEEP_STUBS);
	private final ConnectionBuilder shardConnectionBuilder = mock();

	@Test
	public void testThreadBoundShardingKeys() throws SQLException {
		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);
		ShardingKey shardingKey = mock();
		ShardingKey superShardingKey = mock();

		given(this.dataSource.createConnectionBuilder()).willReturn(this.connectionBuilder);
		when(this.connectionBuilder.shardingKey(shardingKey).superShardingKey(superShardingKey)).thenReturn(this.shardConnectionBuilder);
		given(this.shardConnectionBuilder.build()).willReturn(this.shardConnection);

		dataSourceAdapter.setShardingKeyForCurrentThread(shardingKey);
		dataSourceAdapter.setSuperShardingKeyForCurrentThread(superShardingKey);

		assertThat(dataSourceAdapter.getConnection()).isEqualTo(shardConnection);
	}

	@Test
	public void testThreadBoundShardingKeyAndProviderAreNotNull() throws SQLException {
		ShardingKey providerKey = mock();
		ShardingKey threadBoundKey = mock();
		Connection providerKeyShardConn = mock();
		Connection threadBoundShardConn = mock();

		Connection resultConnection = getShardedConnection(providerKey, threadBoundKey, providerKeyShardConn, threadBoundShardConn);
		assertThat(resultConnection).isEqualTo(threadBoundShardConn);
	}

	@Test
	public void testThreadBoundKeyIsNullAndProviderKeyIsNotNull() throws SQLException {
		ShardingKey providerKey = mock();
		ShardingKey threadBoundKey = null;
		Connection providerKeyShardConn = mock();
		Connection threadBoundShardConn = mock();

		Connection resultConnection = getShardedConnection(providerKey, threadBoundKey, providerKeyShardConn, threadBoundShardConn);
		assertThat(resultConnection).isEqualTo(providerKeyShardConn);
	}


	@Test
	public void testThreadBoundShardingKeyAndProviderAreNull() throws SQLException {
		ShardingKey providerKey = null;
		ShardingKey threadBoundKey = null;
		Connection providerKeyShardConn = mock();
		Connection threadBoundShardConn = mock();

		Connection resultConnection = getShardedConnection(providerKey, threadBoundKey, providerKeyShardConn, threadBoundShardConn);
		assertThat(resultConnection).isEqualTo(this.connection);
	}

	@Test
	public void testConnectionWithUsernameAndPassword() throws SQLException {
		ShardingKeyDataSourceAdapter shardingKeyDataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);
		ShardingKey shardingKey = mock();
		ShardingKey superShardingKey = mock();
		String username = "anir";
		String password = "spring";

		given(this.dataSource.createConnectionBuilder()).willReturn(this.connectionBuilder);
		when(this.connectionBuilder.shardingKey(shardingKey).superShardingKey(superShardingKey)).thenReturn(this.connectionBuilder);
		when(this.connectionBuilder.user(username).password(password).build()).thenReturn(this.connection);

		shardingKeyDataSourceAdapter.setShardingKeyForCurrentThread(shardingKey);
		shardingKeyDataSourceAdapter.setSuperShardingKeyForCurrentThread(superShardingKey);

		Connection resultConnection = shardingKeyDataSourceAdapter.getConnection(username, password);

		Assertions.assertEquals(this.connection, resultConnection);
	}

	private Connection getShardedConnection(@Nullable ShardingKey providerKey, @Nullable ShardingKey threadBoundKey, Connection providerKeyShardConn, Connection threadBoundShardConn) throws SQLException {
		ShardingKeyProvider provider = mock();
		ConnectionBuilder providerConnectionBuilder = mock();
		ConnectionBuilder threadBoundConnectionBuilder = mock();

		given(this.dataSource.createConnectionBuilder()).willReturn(this.connectionBuilder);
		when(this.connectionBuilder.shardingKey(providerKey).superShardingKey(null)).thenReturn(providerConnectionBuilder);
		when(this.connectionBuilder.shardingKey(threadBoundKey).superShardingKey(null)).thenReturn(threadBoundConnectionBuilder);

		when(this.connectionBuilder.shardingKey(null).superShardingKey(null)).thenReturn(this.connectionBuilder);
		given(this.connectionBuilder.build()).willReturn(this.connection);

		given(providerConnectionBuilder.build()).willReturn(providerKeyShardConn);
		given(threadBoundConnectionBuilder.build()).willReturn(threadBoundShardConn);
		given(provider.getShardingKey()).willReturn(providerKey);
		given(provider.getSuperShardingKey()).willReturn(null);

		ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);
		dataSourceAdapter.setShardingKeyForCurrentThread(threadBoundKey);
		dataSourceAdapter.setShardingKeyProvider(provider);

		return dataSourceAdapter.getConnection();
	}
}