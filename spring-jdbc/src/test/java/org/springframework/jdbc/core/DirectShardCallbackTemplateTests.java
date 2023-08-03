package org.springframework.jdbc.core;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKey;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.ShardingKeyDataSourceAdapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

public class DirectShardCallbackTemplateTests {
	private DataSource dataSource = mock();
	private ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);
	private DirectShardCallbackTemplate shardingTemplate = new DirectShardCallbackTemplate(dataSourceAdapter);
	private JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSourceAdapter);

	@Test
	public void testDirectShardRoutingIsNotSupported() throws SQLException {
		ShardingKey shardingKey = mock();
		given(dataSource.createConnectionBuilder()).willThrow(SQLFeatureNotSupportedException.class);

		assertThatThrownBy(
				() -> shardingTemplate.execute(shardingKey, () -> {
					assertThat(dataSourceAdapter.getShardingKeyForCurrentThread()).isEqualTo(shardingKey);
					jdbcTemplate.execute("SELECT * FROM CUSTOMERS");
				})
		).isInstanceOf(CannotGetJdbcConnectionException.class);
		assertThat(dataSourceAdapter.getShardingKeyForCurrentThread()).isNull();
	}

	@Test
	public void testDirectShardRoutingIsSupported() {
		ShardingKey shardingKey = mock();
		shardingTemplate.execute(shardingKey, () -> {
			assertThat(dataSourceAdapter.getShardingKeyForCurrentThread()).isEqualTo(shardingKey);
			return null;
		});
		assertThat(dataSourceAdapter.getShardingKeyForCurrentThread()).isNull();
	}
}