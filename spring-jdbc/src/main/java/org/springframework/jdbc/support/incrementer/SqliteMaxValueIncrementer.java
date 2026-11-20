/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.incrementer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * {@link DataFieldMaxValueIncrementer} that increments the maximum value of a given table with
 * the equivalent of an auto-increment column, using an SQLite {@code select max(rowid)} query.
 *
 * @author Luke Taylor
 * @author Juergen Hoeller
 * @since 7.0
 */
public class SqliteMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 */
	public SqliteMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 * @param columnName the name of the column in the sequence table to use
	 */
	public SqliteMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	@Override
	protected synchronized long getNextKey() {
		Connection con = DataSourceUtils.getConnection(getDataSource());
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
			stmt.executeUpdate("insert into " + getIncrementerName() + " values(null)");
			ResultSet rs = stmt.executeQuery("select max(rowid) from " + getIncrementerName());
			if (!rs.next()) {
				throw new DataAccessResourceFailureException("rowid query failed after executing an update");
			}
			long nextKey = rs.getLong(1);
			stmt.executeUpdate("delete from " + getIncrementerName() + " where " + getColumnName() + " < " + nextKey);
			return nextKey;
		}
		catch (SQLException ex) {
			throw new DataAccessResourceFailureException("Could not obtain rowid", ex);
		}
		finally {
			JdbcUtils.closeStatement(stmt);
			DataSourceUtils.releaseConnection(con, getDataSource());
		}
	}

}
