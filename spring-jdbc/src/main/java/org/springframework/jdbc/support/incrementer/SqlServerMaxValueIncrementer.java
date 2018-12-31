/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * A {@link DataFieldMaxValueIncrementer} for SQL Server that uses a sequence table to auto-increment a value.
 * 
 * <p>This incrementer should be used with SQL Server versions 2008 and older. SQL Server 2012 introduced native
 * database sequences and the {@link SqlServerSequenceMaxValueIncrementer} is a better choice when working with
 * newer versions of the database. 
 *
 * <p>The sequence is kept in a table and there should be one sequence table per table that needs an auto-generated
 * key. Note: If you use this class, your table key column should <i>NOT</i> be defined as an IDENTITY column, as
 * the sequence table does the job.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100))
 * create table tab_sequence (id bigint identity)
 * insert into tab_sequence default values</pre>
 *
 * If "cacheSize" is set, the intermediate values are served without querying the
 * database. If the server or your application is stopped or crashes or a transaction
 * is rolled back, the unused values will never be served. The maximum hole size in
 * numbering is consequently the value of cacheSize.
 *
 * <p><b>NOTE:</b> This class does NOT use {@code AbstractIdentityColumnMaxValueIncrementer} because
 * the locking model with SQL Server may use a page/table level lock when deleting records from
 * the sequence table. The delete within {@code AbstractIdentityColumnMaxValueIncrementer} can result in a
 * database deadlock error on SQL Server when multiple instances of the incrementer are running (in different
 * processes) and the incrementer is called within the context of an existing database transaction. 
 * 
 * <p>To get around the locking model, this class relies on a reaping strategy to clean up rows within the
 * sequence table. A reaper interval is used to keep track of the last time the rows were removed from the table.
 * Each time {@code getNextKey} is called and the reaping interval has been reached, this class will spin up a
 * thread to delete the data from the table. The use of a new thread insures the delete is handled outside the
 * scope of any current transaction.  The default reaper interval is 20 seconds.
 * 
 * <p><b>HINT:</b> Since Microsoft SQL Server supports the JDBC 3.0 {@code getGeneratedKeys}
 * method, it is recommended to use IDENTITY columns directly in the tables and then using a
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcInsert} or utilizing
 * a {@link org.springframework.jdbc.support.KeyHolder} when calling the with the
 * {@code update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)}
 * method of the {@link org.springframework.jdbc.core.JdbcTemplate}.
 *
 * <p>Thanks to Preben Nilsson for the suggestion!
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Tyler Van Gorder
 * @since 2.5.5
 */
public class SqlServerMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	private long[] valueCache;

	/** The next id to serve from the value cache */
	private int nextValueIndex = -1;

	private final ReapOldValues reaper = new ReapOldValues();	
	private int reaperIntervalSeconds = 20;
	private long nextReapTime;

	/**
	 * Default constructor for bean property style usage.
	 * 
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 * @see #setReaperInternalSeconds
	 */
	public SqlServerMaxValueIncrementer() {
		nextReapTime = getReapTime();
	}
	
	/**
	 * Convenience constructor. The default reaper interval will be 20 seconds.
	 * 
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 * @param columnName the name of the column in the sequence table to use
	 */
	public SqlServerMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
		nextReapTime = getReapTime();
	}
	
	private String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " default values";
	}

	private String getIdentityStatement() {
		return "select @@identity";
	}

	@Override
	protected synchronized long getNextKey() {
		if (this.nextValueIndex < 0 || this.nextValueIndex >= getCacheSize()) {
			/*
			* Need to use straight JDBC code because we need to make sure that the insert and select
			* are performed on the same connection (otherwise we can't be sure that @@identity
			* returns the correct value)
			*/
			Connection con = DataSourceUtils.getConnection(getDataSource());
			Statement stmt = null;
			try {
				stmt = con.createStatement();
				DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
				this.valueCache = new long[getCacheSize()];
				this.nextValueIndex = 0;
				for (int i = 0; i < getCacheSize(); i++) {
					stmt.executeUpdate(getIncrementStatement());
					ResultSet rs = stmt.executeQuery(getIdentityStatement());
					try {
						if (!rs.next()) {
							throw new DataAccessResourceFailureException("Identity statement failed after inserting");
						}
						this.valueCache[i] = rs.getLong(1);
					}
					finally {
						JdbcUtils.closeResultSet(rs);
					}
				}
				if (System.currentTimeMillis() > nextReapTime) {
					//If the current time has exceeded the reap time (default 20 seconds), spin up a thread to delete the old values.
					
					//NOTE: This class uses a new thread to isolate the delete in a separate transaction rather than
					//using a new transaction and requiring the PlatformTransactionManager as an injected dependency.					
					Thread reapingThread = new Thread(reaper, "Incrementer " +  getIncrementerName()  +  " Reaping Thread");
					reapingThread.setDaemon(true);
					reapingThread.start();
					nextReapTime = getReapTime();
				}
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not increment identity", ex);
			}
			finally {
				JdbcUtils.closeStatement(stmt);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
		return this.valueCache[this.nextValueIndex++];
	}

	private long getReapTime() {
		return System.currentTimeMillis() + (reaperIntervalSeconds * 1000);
	}

	public void setReaperInternalSeconds(int reaperInternalSeconds) {
		this.reaperIntervalSeconds = reaperInternalSeconds;
	}
	
	private class ReapOldValues implements Runnable {
		
		@Override
		public void run() {
			Connection con = DataSourceUtils.getConnection(getDataSource());
			Statement stmt = null;
			try {
				stmt = con.createStatement();
				DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
				stmt.executeUpdate("DELETE FROM " + getIncrementerName());
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not delete old identity values", ex);
			}
			finally {
				JdbcUtils.closeStatement(stmt);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}			
		}
	}
}
