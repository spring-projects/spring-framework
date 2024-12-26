/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Generic callback interface for code that operates on a PreparedStatement.
 * Allows to execute any number of operations on a single PreparedStatement,
 * for example a single {@code executeUpdate} call or repeated
 * {@code executeUpdate} calls with varying parameters.
 *
 * <p>Used internally by JdbcTemplate, but also useful for application code.
 * Note that the passed-in PreparedStatement can have been created by the
 * framework or by a custom PreparedStatementCreator. However, the latter is
 * hardly ever necessary, as most custom callback actions will perform updates
 * in which case a standard PreparedStatement is fine. Custom actions will
 * always set parameter values themselves, so that PreparedStatementCreator
 * capability is not needed either.
 *
 * <p>预处理语句回调(PreparedStatementCallback)
 * <p>对PreparedStatement进行操作的代码的通用回调接口。允许对单个PreparedStatement执行任意数量的操作，
 * 例如，单个{@code executeUpdate}调用或重复调用｛@code executeUpdate｝调用具有不同参数。
 * <p>JdbcTemplate在内部使用，但对应用程序代码也很有用。请注意，传入的PreparedStatement可能是由
 * 框架或由自定义的PreparedStatementCreator创建。然而，后者是几乎没有必要，因为大多数自定义回调操作都会执行更新
 * 在这种情况下，标准的准备声明是可以的。自定义操作将始终自行设置参数值，以便PreparedStatementCreator也不需要能力。
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @see JdbcTemplate#execute(String, PreparedStatementCallback)
 * @see JdbcTemplate#execute(PreparedStatementCreator, PreparedStatementCallback)
 * @since 16.03.2004
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {

	/**
	 * Gets called by {@code JdbcTemplate.execute} with an active JDBC
	 * PreparedStatement. Does not need to care about closing the Statement
	 * or the Connection, or about handling transactions: this will all be
	 * handled by Spring's JdbcTemplate.
	 * <p><b>NOTE:</b> Any ResultSets opened should be closed in finally blocks
	 * within the callback implementation. Spring will close the Statement
	 * object after the callback returned, but this does not necessarily imply
	 * that the ResultSet resources will be closed: the Statement objects might
	 * get pooled by the connection pool, with {@code close} calls only
	 * returning the object to the pool but not physically closing the resources.
	 * <p>If called without a thread-bound JDBC transaction (initiated by
	 * DataSourceTransactionManager), the code will simply get executed on the
	 * JDBC connection with its transactional semantics. If JdbcTemplate is
	 * configured to use a JTA-aware DataSource, the JDBC connection and thus
	 * the callback code will be transactional if a JTA transaction is active.
	 * <p>Allows for returning a result object created within the callback, i.e.
	 * a domain object or a collection of domain objects. Note that there's
	 * special support for single step actions: see JdbcTemplate.queryForObject etc.
	 * A thrown RuntimeException is treated as application exception, it gets
	 * propagated to the caller of the template.
	 *
	 * <p>在准备好的声明中(doInPreparedStatement)
	 * <p>处理一些通用方法外的个性化处理, 也就是PreparedStatementCallback类型的参数的doInPreparedStatement方法的回调
	 * <p>由具有活动JDBC的{@code JdbcTemplate.execute}调用准备声明。不需要关心结束声明或连接，
	 * 或关于处理事务：这将是由Spring的JdbcTemplate处理。
	 * <p><b>NOTE:</b>任何打开的ResultSets都应该在finally块中关闭在回调实现中。
	 * Spring将结束声明回调返回后的对象，但这并不一定意味着ResultSet资源将被关闭：
	 * Statement对象可能由连接池池池池化，仅使用{@code close}调用将对象返回到池中，但不物理关闭资源。
	 * <p>如果调用时没有线程绑定的JDBC事务（由DataSourceTransactionManager），
	 * 代码将在JDBC连接及其事务语义。如果JdbcTemplate是配置为使用支持JTA的DataSource、JDBC连接，
	 * 因此如果JTA事务处于活动状态，则回调代码将是事务性的。
	 * <p>允许返回在回调中创建的结果对象，即。
	 * 域对象或域对象的集合。请注意，有对单步操作的特殊支持：请参阅JdbcTemplate.queryForObject等。
	 * 抛出的RuntimeException被视为应用程序异常，它得到传播到模板的调用者。
	 *
	 * @param ps active JDBC PreparedStatement
	 * @return a result object, or {@code null} if none
	 * @throws SQLException        if thrown by a JDBC method, to be auto-converted
	 *                             to a DataAccessException by an SQLExceptionTranslator
	 * @throws DataAccessException in case of custom exceptions
	 * @see JdbcTemplate#queryForObject(String, Object[], Class)
	 * @see JdbcTemplate#queryForList(String, Object[])
	 */
	@Nullable
	T doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException;

}
