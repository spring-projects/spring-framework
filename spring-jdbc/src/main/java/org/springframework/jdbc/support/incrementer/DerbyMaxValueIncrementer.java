/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jdbc.support.incrementer;

import javax.sql.DataSource;

/**
 * {@link DataFieldMaxValueIncrementer} that increments the maximum value of a given Derby table
 * with the equivalent of an auto-increment column. Note: If you use this class, your Derby key
 * column should <i>NOT</i> be defined as an IDENTITY column, as the sequence table does the job.
 *
 * <p>The sequence is kept in a table. There should be one sequence table per
 * table that needs an auto-generated key.
 *
 * <p>Derby requires an additional column to be used for the insert since it is impossible
 * to insert a null into the identity column and have the value generated.  This is solved by
 * providing the name of a dummy column that also must be created in the sequence table.
 *
 * <p>Example:
 *
 * <pre class="code">create table tab (id int not null primary key, text varchar(100));
 * create table tab_sequence (value int generated always as identity, dummy char(1));
 * insert into tab_sequence (dummy) values(null);</pre>
 *
 * If "cacheSize" is set, the intermediate values are served without querying the
 * database. If the server or your application is stopped or crashes or a transaction
 * is rolled back, the unused values will never be served. The maximum hole size in
 * numbering is consequently the value of cacheSize.
 *
 * <b>HINT:</b> Since Derby supports the JDBC 3.0 {@code getGeneratedKeys} method,
 * it is recommended to use IDENTITY columns directly in the tables and then utilizing
 * a {@link org.springframework.jdbc.support.KeyHolder} when calling the with the
 * {@code update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)}
 * method of the {@link org.springframework.jdbc.core.JdbcTemplate}.
 *
 * <p>Thanks to Endre Stolsvik for the suggestion!
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 */
public class DerbyMaxValueIncrementer extends AbstractIdentityColumnMaxValueIncrementer {

	/** The default for dummy name */
	private static final String DEFAULT_DUMMY_NAME = "dummy";

	/** The name of the dummy column used for inserts */
	private String dummyName = DEFAULT_DUMMY_NAME;


	/**
	 * Default constructor for bean property style usage.
	 * @see #setDataSource
	 * @see #setIncrementerName
	 * @see #setColumnName
	 */
	public DerbyMaxValueIncrementer() {
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 * @param columnName the name of the column in the sequence table to use
	 */
	public DerbyMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
		super(dataSource, incrementerName, columnName);
		this.dummyName = DEFAULT_DUMMY_NAME;
	}

	/**
	 * Convenience constructor.
	 * @param dataSource the DataSource to use
	 * @param incrementerName the name of the sequence/table to use
	 * @param columnName the name of the column in the sequence table to use
	 * @param dummyName the name of the dummy column used for inserts
	 */
	public DerbyMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName, String dummyName) {
		super(dataSource, incrementerName, columnName);
		this.dummyName = dummyName;
	}


	/**
	 * Set the name of the dummy column.
	 */
	public void setDummyName(String dummyName) {
		this.dummyName = dummyName;
	}

	/**
	 * Return the name of the dummy column.
	 */
	public String getDummyName() {
		return this.dummyName;
	}


	@Override
	protected String getIncrementStatement() {
		return "insert into " + getIncrementerName() + " (" + getDummyName() + ") values(null)";
	}

	@Override
	protected String getIdentityStatement() {
		return "select IDENTITY_VAL_LOCAL() from " + getIncrementerName();
	}

}
