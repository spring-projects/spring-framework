/*
 * Copyright 2002-2013 the original author or authors.
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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

/**
 * SqlUpdate subclass that performs batch update operations. Encapsulates
 * queuing up records to be updated, and adds them as a single batch once
 * {@code flush} is called or the given batch size has been met.
 *
 * <p>Note that this class is a <b>non-thread-safe object</b>, in contrast
 * to all other JDBC operations objects in this package. You need to create
 * a new instance of it for each use, or call {@code reset} before
 * reuse within the same thread.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.1
 * @see #flush
 * @see #reset
 */
public class BatchSqlUpdate extends SqlUpdate {

	/**
	 * Default number of inserts to accumulate before commiting a batch (5000).
	 */
	public static int DEFAULT_BATCH_SIZE = 5000;


	private int batchSize = DEFAULT_BATCH_SIZE;

	private boolean trackRowsAffected = true;
	
	private Map<String, ?> namedParamMap = null;

	private final LinkedList<Object[]> parameterQueue = new LinkedList<Object[]>();

	private final List<Integer> rowsAffected = new ArrayList<Integer>();


	/**
	 * Constructor to allow use as a JavaBean. DataSource and SQL
	 * must be supplied before compilation and use.
	 * @see #setDataSource
	 * @see #setSql
	 */
	public BatchSqlUpdate() {
		super();
	}

	/**
	 * Construct an update object with a given DataSource and SQL.
	 * @param ds DataSource to use to obtain connections
	 * @param sql SQL statement to execute
	 */
	public BatchSqlUpdate(DataSource ds, String sql) {
		super(ds, sql);
	}

	/**
	 * Construct an update object with a given DataSource, SQL
	 * and anonymous parameters.
	 * @param ds DataSource to use to obtain connections
	 * @param sql SQL statement to execute
	 * @param types SQL types of the parameters, as defined in the
	 * {@code java.sql.Types} class
	 * @see java.sql.Types
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types) {
		super(ds, sql, types);
	}

	/**
	 * Construct an update object with a given DataSource, SQL,
	 * anonymous parameters and specifying the maximum number of rows
	 * that may be affected.
	 * @param ds DataSource to use to obtain connections
	 * @param sql SQL statement to execute
	 * @param types SQL types of the parameters, as defined in the
	 * {@code java.sql.Types} class
	 * @param batchSize the number of statements that will trigger
	 * an automatic intermediate flush
	 * @see java.sql.Types
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types, int batchSize) {
		super(ds, sql, types);
		setBatchSize(batchSize);
	}


	/**
	 * Set the number of statements that will trigger an automatic intermediate
	 * flush. {@code update} calls or the given statement parameters will
	 * be queued until the batch size is met, at which point it will empty the
	 * queue and execute the batch.
	 * <p>You can also flush already queued statements with an explicit
	 * {@code flush} call. Note that you need to this after queueing
	 * all parameters to guarantee that all statements have been flushed.
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Set whether to track the rows affected by batch updates performed
	 * by this operation object.
	 * <p>Default is "true". Turn this off to save the memory needed for
	 * the list of row counts.
	 * @see #getRowsAffected()
	 */
	public void setTrackRowsAffected(boolean trackRowsAffected) {
		this.trackRowsAffected = trackRowsAffected;
	}

	/**
	 * BatchSqlUpdate does not support BLOB or CLOB parameters.
	 */
	@Override
	protected boolean supportsLobParameters() {
		return false;
	}


	/**
	 * Overridden version of {@code update} that adds the given statement
	 * parameters to the queue rather than executing them immediately.
	 * All other {@code update} methods of the SqlUpdate base class go
	 * through this method and will thus behave similarly.
	 * <p>You need to call {@code flush} to actually execute the batch.
	 * If the specified batch size is reached, an implicit flush will happen;
	 * you still need to finally call {@code flush} to flush all statements.
	 * @param params array of parameter objects
	 * @return the number of rows affected by the update (always -1,
	 * meaning "not applicable", as the statement is not actually
	 * executed by this method)
	 * @see #flush
	 */
	@Override
	public int update(Object... params) throws DataAccessException {
		validateParameters(params);
		this.parameterQueue.add(params.clone());

		if (this.parameterQueue.size() == this.batchSize) {
			if (logger.isDebugEnabled()) {
				logger.debug("Triggering auto-flush because queue reached batch size of " + this.batchSize);
			}
			flush();
		}

		return -1;
	}
	
	/**
	 * Overridden version of {@code updateByNamedParam} that adds the given named statement
	 * parameters to the queue rather than executing them immediately.
	 * <p>You need to call {@code flush} to actually execute the batch.
	 * If the specified batch size is reached, an implicit flush will happen;
	 * you still need to finally call {@code flush} to flush all statements.
	 * @param paramMap Map of parameter name to parameter object,
	 * matching named parameters specified in the SQL statement
	 * @return the number of rows affected by the update (always -1,
	 * meaning "not applicable", as the statement is not actually
	 * executed by this method)
	 * @see #flush
	 */
	@Override
	public int updateByNamedParam(Map<String, ?> paramMap) throws DataAccessException {
		validateNamedParameters(paramMap);
		this.parameterQueue.add(new Object[]{paramMap});
		this.namedParamMap = paramMap;
		
		if (this.parameterQueue.size() == this.batchSize) {
			if (logger.isDebugEnabled()) {
				logger.debug("Triggering auto-flush because queue reached batch size of " + this.batchSize);
			}
			
			flush();
		}
		
		return -1;
	}

	/**
	 * Trigger any queued update operations to be added as a final batch.
	 * @return an array of the number of rows affected by each statement
	 */
	public int[] flush() {
		if (this.parameterQueue.isEmpty()) {
			return new int[0];
		}
		
		if (this.namedParamMap != null) {
			return flushWithNamedParam();
		}

		int[] rowsAffected = getJdbcTemplate().batchUpdate(
				getSql(),
				new BatchPreparedStatementSetter() {
					public int getBatchSize() {
						return parameterQueue.size();
					}
					public void setValues(PreparedStatement ps, int index) throws SQLException {
						Object[] params = parameterQueue.removeFirst();
						newPreparedStatementSetter(params).setValues(ps);
					}
				});

		for (int rowCount : rowsAffected) {
			checkRowsAffected(rowCount);
			if (this.trackRowsAffected) {
				this.rowsAffected.add(rowCount);
			}
		}

		return rowsAffected;
	}
	
	/**
	 * Trigger any queued update operations to be added as a final batch
	 * (In case of queue which contains named parameter map.)
	 * @return an array of the number of rows affected by each statement
	 */
	protected int[] flushWithNamedParam() {
		int[] rowsAffected = getJdbcTemplate().batchUpdate(
				getNamedParameterParsedSql(),
				new BatchPreparedStatementSetter() {
					public int getBatchSize() {
						return parameterQueue.size();
					}
					public void setValues(PreparedStatement ps, int index) throws SQLException {
						Object[] params = convertNamedParamMap(parameterQueue.removeFirst());
						newPreparedStatementSetter(params).setValues(ps);
					}
				});
		
		for (int rowCount : rowsAffected) {
			checkRowsAffected(rowCount);
			if (this.trackRowsAffected) {
				this.rowsAffected.add(rowCount);
			}
		}
		
		return rowsAffected;
	}

	/**
	 * Return the parsed sql that named parameter is replaced by '?' (prepared statement binding)
	 * @see SqlUpdate#updateByNamedParam  
	 */
	protected String getNamedParameterParsedSql() {
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(namedParamMap);
		return NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
	}
	
	/**
	 * Return params array which is suitable for {@code newPreparedStatementSetter}
	 * and converted from named parameter map.
	 * @param Object array which contains named parameter map
	 * @see SqlUpdate#updateByNamedParam  
	 * @see SqlOperation#newPreparedStatementSetter
	 */
	protected Object[] convertNamedParamMap(Object[] map) {
		if(map[0] instanceof Map) {
			
		}
		Map<String, ?> paramMap = (Map<String, ?>) map[0];
		ParsedSql parsedSql = getParsedSql();
		MapSqlParameterSource paramSource = new MapSqlParameterSource(paramMap);
		return NamedParameterUtils.buildValueArray(parsedSql, paramSource, getDeclaredParameters());
	}

	/**
	 * Return the current number of statements or statement parameters
	 * in the queue.
	 */
	public int getQueueCount() {
		return this.parameterQueue.size();
	}

	/**
	 * Return the number of already executed statements.
	 */
	public int getExecutionCount() {
		return this.rowsAffected.size();
	}

	/**
	 * Return the number of affected rows for all already executed statements.
	 * Accumulates all of {@code flush}'s return values until
	 * {@code reset} is invoked.
	 * @return an array of the number of rows affected by each statement
	 * @see #reset
	 */
	public int[] getRowsAffected() {
		int[] result = new int[this.rowsAffected.size()];
		int i = 0;
		for (Iterator<Integer> it = this.rowsAffected.iterator(); it.hasNext(); i++) {
			result[i] = it.next();
		}
		return result;
	}

	/**
	 * Reset the statement parameter queue, the rows affected cache,
	 * and the execution count.
	 */
	public void reset() {
		this.parameterQueue.clear();
		this.rowsAffected.clear();
	}

}
