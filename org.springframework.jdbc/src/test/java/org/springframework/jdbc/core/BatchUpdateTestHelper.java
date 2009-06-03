package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * @author Thomas Risberg
 */
public abstract class BatchUpdateTestHelper {

	public static void prepareBatchUpdateMocks(String sqlToUse, Object ids, int[] sqlTypes,
			int[] rowsAffected,
			MockControl ctrlDataSource, DataSource mockDataSource, MockControl ctrlConnection, Connection mockConnection,
			MockControl ctrlPreparedStatement,
			PreparedStatement mockPreparedStatement, MockControl ctrlDatabaseMetaData, DatabaseMetaData mockDatabaseMetaData)
			throws SQLException {
		mockConnection.getMetaData();
		ctrlConnection.setDefaultReturnValue(null);
		mockConnection.close();
		ctrlConnection.setDefaultVoidCallable();

		mockDataSource.getConnection();
		ctrlDataSource.setDefaultReturnValue(mockConnection);

		mockPreparedStatement.getConnection();
		ctrlPreparedStatement.setReturnValue(mockConnection);
		int idLength = 0;
		if (ids instanceof SqlParameterSource[]) {
			idLength = ((SqlParameterSource[])ids).length;
		}
		else if (ids instanceof Map[]) {
			idLength = ((Map[])ids).length;
		}
		else {
			idLength = ((List)ids).size();
		}

		for (int i = 0; i < idLength; i++) {
			if (ids instanceof SqlParameterSource[]) {
				if (sqlTypes != null) {
					mockPreparedStatement.setObject(1, ((SqlParameterSource[])ids)[i].getValue("id"), sqlTypes[0]);
				}
				else {
					mockPreparedStatement.setObject(1, ((SqlParameterSource[])ids)[i].getValue("id"));
				}
			}
			else if (ids instanceof Map[]) {
				if (sqlTypes != null) {
					mockPreparedStatement.setObject(1, ((Map[])ids)[i].get("id"), sqlTypes[0]);
				}
				else {
					mockPreparedStatement.setObject(1, ((Map[])ids)[i].get("id"));
				}
			}
			else {
				if (sqlTypes != null) {
					mockPreparedStatement.setObject(1, ((Object[])((List)ids).get(i))[0], sqlTypes[0]);
				}
				else {
					mockPreparedStatement.setObject(1, ((Object[])((List)ids).get(i))[0]);
				}
			}
			ctrlPreparedStatement.setVoidCallable();
			mockPreparedStatement.addBatch();
			ctrlPreparedStatement.setVoidCallable();
		}
		mockPreparedStatement.executeBatch();
		ctrlPreparedStatement.setReturnValue(rowsAffected);
		if (LogFactory.getLog(JdbcTemplate.class).isDebugEnabled()) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MySQL");
		mockDatabaseMetaData.supportsBatchUpdates();
		ctrlDatabaseMetaData.setReturnValue(true);

		mockConnection.prepareStatement(sqlToUse);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.getMetaData();
		ctrlConnection.setReturnValue(mockDatabaseMetaData, 2);
	}

	public static void replayBatchUpdateMocks(MockControl ctrlDataSource,
			MockControl ctrlConnection,
			MockControl ctrlPreparedStatement,
			MockControl ctrlDatabaseMetaData) {
		ctrlPreparedStatement.replay();
		ctrlDatabaseMetaData.replay();
		ctrlDataSource.replay();
		ctrlConnection.replay();
	}

	public static void verifyBatchUpdateMocks(MockControl ctrlPreparedStatement, MockControl ctrlDatabaseMetaData) {
		ctrlPreparedStatement.verify();
		ctrlDatabaseMetaData.verify();
	}

}
