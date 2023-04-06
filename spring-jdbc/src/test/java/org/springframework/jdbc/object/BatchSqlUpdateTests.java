/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.object;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Types;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.SqlParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 22.02.2005
 */
public class BatchSqlUpdateTests {

	@Test
	public void testBatchUpdateWithExplicitFlush() throws Exception {
		doTestBatchUpdate(false);
	}

	@Test
	public void testBatchUpdateWithFlushThroughBatchSize() throws Exception {
		doTestBatchUpdate(true);
	}

	private void doTestBatchUpdate(boolean flushThroughBatchSize) throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		Connection connection = mock();
		DataSource dataSource = mock();
		given(dataSource.getConnection()).willReturn(connection);
		PreparedStatement preparedStatement = mock();
		given(preparedStatement.getConnection()).willReturn(connection);
		given(preparedStatement.executeBatch()).willReturn(rowsAffected);

		DatabaseMetaData mockDatabaseMetaData = mock();
		given(mockDatabaseMetaData.supportsBatchUpdates()).willReturn(true);
		given(connection.prepareStatement(sql)).willReturn(preparedStatement);
		given(connection.getMetaData()).willReturn(mockDatabaseMetaData);

		BatchSqlUpdate update = new BatchSqlUpdate(dataSource, sql);
		update.declareParameter(new SqlParameter(Types.INTEGER));
		if (flushThroughBatchSize) {
			update.setBatchSize(2);
		}

		update.update(ids[0]);
		update.update(ids[1]);

		if (flushThroughBatchSize) {
			assertThat(update.getQueueCount()).isEqualTo(0);
			assertThat(update.getRowsAffected()).hasSize(2);
		}
		else {
			assertThat(update.getQueueCount()).isEqualTo(2);
			assertThat(update.getRowsAffected()).isEmpty();
		}

		int[] actualRowsAffected = update.flush();
		assertThat(update.getQueueCount()).isEqualTo(0);

		if (flushThroughBatchSize) {
			assertThat(actualRowsAffected).as("flush did not execute updates").isEmpty();
		}
		else {
			assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
			assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
			assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);
		}

		actualRowsAffected = update.getRowsAffected();
		assertThat(actualRowsAffected).as("executed 2 updates").hasSize(2);
		assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
		assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

		update.reset();
		assertThat(update.getRowsAffected()).isEmpty();

		verify(preparedStatement).setObject(1, ids[0], Types.INTEGER);
		verify(preparedStatement).setObject(1, ids[1], Types.INTEGER);
		verify(preparedStatement, times(2)).addBatch();
		verify(preparedStatement).close();
	}
}
