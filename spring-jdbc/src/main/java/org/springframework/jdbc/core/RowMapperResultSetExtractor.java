/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Adapter implementation of the ResultSetExtractor interface that delegates
 * to a RowMapper which is supposed to create an object for each row.
 * Each object is added to the results List of this ResultSetExtractor.
 *
 * <p>Useful for the typical case of one object per row in the database table.
 * The number of entries in the results list will match the number of rows.
 *
 * <p>Note that a RowMapper object is typically stateless and thus reusable;
 * just the RowMapperResultSetExtractor adapter is stateful.
 *
 * <p>A usage example with JdbcTemplate:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * RowMapper rowMapper = new UserRowMapper();  // reusable object
 *
 * List allUsers = (List) jdbcTemplate.query(
 *     "select * from user",
 *     new RowMapperResultSetExtractor(rowMapper, 10));
 *
 * User user = (User) jdbcTemplate.queryForObject(
 *     "select * from user where id=?", new Object[] {id},
 *     new RowMapperResultSetExtractor(rowMapper, 1));</pre>
 *
 * <p>Alternatively, consider subclassing MappingSqlQuery from the <code>jdbc.object</code>
 * package: Instead of working with separate JdbcTemplate and RowMapper objects,
 * you can have executable query objects (containing row-mapping logic) there.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see RowMapper
 * @see JdbcTemplate
 * @see org.springframework.jdbc.object.MappingSqlQuery
 */
public class RowMapperResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

	private final RowMapper<T> rowMapper;

	private final int rowsExpected;


	/**
	 * Create a new RowMapperResultSetExtractor.
	 * @param rowMapper the RowMapper which creates an object for each row
	 */
	public RowMapperResultSetExtractor(RowMapper<T> rowMapper) {
		this(rowMapper, 0);
	}

	/**
	 * Create a new RowMapperResultSetExtractor.
	 * @param rowMapper the RowMapper which creates an object for each row
	 * @param rowsExpected the number of expected rows
	 * (just used for optimized collection handling)
	 */
	public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected) {
		Assert.notNull(rowMapper, "RowMapper is required");
		this.rowMapper = rowMapper;
		this.rowsExpected = rowsExpected;
	}


	public List<T> extractData(ResultSet rs) throws SQLException {
		List<T> results = (this.rowsExpected > 0 ? new ArrayList<T>(this.rowsExpected) : new ArrayList<T>());
		int rowNum = 0;
		while (rs.next()) {
			results.add(this.rowMapper.mapRow(rs, rowNum++));
		}
		return results;
	}

}
