/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jdbc.object;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Juergen Hoeller
 * @since 22.02.2005
 */
public class BatchSqlUpdateTests extends AbstractJdbcTests {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();


	public void testBatchUpdateWithExplicitFlush() throws Exception {
		doTestBatchUpdate(false);
	}

	public void testBatchUpdateWithFlushThroughBatchSize() throws Exception {
		doTestBatchUpdate(true);
	}

	private void doTestBatchUpdate(boolean flushThroughBatchSize) throws Exception {
		final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
		final int[] ids = new int[] { 100, 200 };
		final int[] rowsAffected = new int[] { 1, 2 };

		MockControl ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		PreparedStatement mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		mockPreparedStatement.setObject(1, new Integer(ids[0]), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setObject(1, new Integer(ids[1]), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.addBatch();
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(rowsAffected);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		MockControl ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		mockConnection.prepareStatement(sql);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 1);

		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		replay();

		BatchSqlUpdate update = new BatchSqlUpdate(mockDataSource, sql);
		update.declareParameter(new SqlParameter(Types.INTEGER));
		if (flushThroughBatchSize) {
			update.setBatchSize(2);
		}

		update.update(ids[0]);
		update.update(ids[1]);

		if (flushThroughBatchSize) {
			assertEquals(0, update.getQueueCount());
			assertEquals(2, update.getRowsAffected().length);
		}
		else {
			assertEquals(2, update.getQueueCount());
			assertEquals(0, update.getRowsAffected().length);
		}

		int[] actualRowsAffected = update.flush();
		assertEquals(0, update.getQueueCount());

		if (flushThroughBatchSize) {
			assertTrue("flush did not execute updates", actualRowsAffected.length == 0);
		}
		else {
			assertTrue("executed 2 updates", actualRowsAffected.length == 2);
			assertEquals(rowsAffected[0], actualRowsAffected[0]);
			assertEquals(rowsAffected[1], actualRowsAffected[1]);
		}

		actualRowsAffected = update.getRowsAffected();
		assertTrue("executed 2 updates", actualRowsAffected.length == 2);
		assertEquals(rowsAffected[0], actualRowsAffected[0]);
		assertEquals(rowsAffected[1], actualRowsAffected[1]);

		update.reset();
		assertEquals(0, update.getRowsAffected().length);

		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

}
