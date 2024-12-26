/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * General callback interface used by the {@link JdbcTemplate} class.
 *
 * <p>This interface sets values on a {@link java.sql.PreparedStatement} provided
 * by the JdbcTemplate class, for each of a number of updates in a batch using the
 * same SQL. Implementations are responsible for setting any necessary parameters.
 * SQL with placeholders will already have been supplied.
 *
 * <p>It's easier to use this interface than {@link PreparedStatementCreator}:
 * The JdbcTemplate will create the PreparedStatement, with the callback
 * only being responsible for setting parameter values.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with
 * SQLExceptions that may be thrown from operations they attempt.
 * The JdbcTemplate class will catch and handle SQLExceptions appropriately.
 *
 * <p>{@link JdbcTemplate}类使用的通用回调接口.
 * <p>此接口在提供的{@link java.sql.PreparedStatement}上设置值通过JdbcTemplate类，
 * 使用相同的SQL。实现负责设置任何必要的参数。已提供带占位符的SQL。
 * <p>使用此界面比{@link PreparedStatementCreator}更容易：
 * JdbcTemplate将创建带有回调的PreparedStatement仅负责设置参数值。
 * <p>实现<i>不需要关心</i>可能从他们尝试的操作中抛出的SQLException。
 * JdbcTemplate类将适当地捕获和处理SQLExceptions。
 *
 * @author Rod Johnson
 * @see JdbcTemplate#update(String, PreparedStatementSetter)
 * @see JdbcTemplate#query(String, PreparedStatementSetter, ResultSetExtractor)
 * @since March 2, 2003
 */
@FunctionalInterface
public interface PreparedStatementSetter {

	/**
	 * Set parameter values on the given PreparedStatement.
	 * <p>在给定的PreparedStatement(准备声明)上设置参数值
	 *
	 * @param ps the PreparedStatement to invoke setter methods on
	 * @throws SQLException if an SQLException is encountered
	 *                      (i.e. there is no need to catch SQLException)
	 */
	void setValues(PreparedStatement ps) throws SQLException;

}
