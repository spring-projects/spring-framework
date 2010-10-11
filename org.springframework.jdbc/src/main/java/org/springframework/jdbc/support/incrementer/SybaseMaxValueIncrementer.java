package org.springframework.jdbc.support.incrementer;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer} that increments 
 * the maximum value of a given Sybase SQL Server table
 * with the equivalent of an auto-increment column. Note: If you use this class, your table key
 * column should <i>NOT</i> be defined as an IDENTITY column, as the sequence table does the job.
 *
 * <p>This class is intended to be used with Sybase Adaptive Server.
 *
 * <p>The sequence is kept in a table. There should be one sequence table per
 * table that needs an auto-generated key.
 *
 * <p>Example:
 *
 * <pre class="code"> create table tab (id int not null primary key, text varchar(100))
 * create table tab_sequence (id bigint identity)
 * insert into tab_sequence values()</pre>
 *
 * If "cacheSize" is set, the intermediate values are served without querying the
 * database. If the server or your application is stopped or crashes or a transaction
 * is rolled back, the unused values will never be served. The maximum hole size in
 * numbering is consequently the value of cacheSize.
 *
 * <b>HINT:</b>  Since Sybase supports the JDBC 3.0 <code>getGeneratedKeys</code> method,
 * it is recommended to use IDENTITY columns directly in the tables and then using a
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcInsert} or utilizing
 * a {@link org.springframework.jdbc.support.KeyHolder} when calling the with the
 * <code>update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)</code>
 * method of the {@link org.springframework.jdbc.core.JdbcTemplate}.
 *
 * <p>Thanks to Yinwei Liu for the suggestion!
 *
 * @author Thomas Risberg
 * @since 2.5.5
 */
public class SybaseMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

	/** The current cache of values */
	private long[] valueCache;

	/** The next id to serve from the value cache */
	private int nextValueIndex = -1;


	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 */
	public SybaseMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 * @param columnName the name of the column in the sequence table to use
	 */
	public SybaseMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
	}


	@Override
	protected synchronized long getNextKey() throws DataAccessException {
		if (this.nextValueIndex < 0 || this.nextValueIndex >= getCacheSize()) {
			/*
			* Need to use straight JDBC code because we need to make sure that the insert and select
			* are performed on the same connection (otherwise we can't be sure that @@identity
			* returnes the correct value)
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
					ResultSet rs = stmt.executeQuery("select @@identity");
					try {
						if (!rs.next()) {
							throw new DataAccessResourceFailureException("@@identity failed after executing an update");
						}
						this.valueCache[i] = rs.getLong(1);
					}
					finally {
						JdbcUtils.closeResultSet(rs);
					}
				}
				long maxValue = this.valueCache[(this.valueCache.length - 1)];
				stmt.executeUpdate("delete from " + getIncrementerName() + " where " + getColumnName() + " < " + maxValue);
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

	/**
	 * Statement to use to increment the "sequence" value. Can be overridden by sub-classes.
	 * @return The SQL statement to use
	 */
	protected String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " values()";
	}

}
