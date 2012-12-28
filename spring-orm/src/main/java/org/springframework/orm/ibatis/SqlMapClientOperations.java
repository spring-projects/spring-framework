/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.ibatis;

import java.util.List;
import java.util.Map;

import com.ibatis.sqlmap.client.event.RowHandler;

import org.springframework.dao.DataAccessException;

/**
 * Interface that specifies a basic set of iBATIS SqlMapClient operations,
 * implemented by {@link SqlMapClientTemplate}. Not often used, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Defines SqlMapClientTemplate's convenience methods that mirror
 * the iBATIS {@link com.ibatis.sqlmap.client.SqlMapExecutor}'s execution
 * methods. Users are strongly encouraged to read the iBATIS javadocs
 * for details on the semantics of those methods.
 *
 * @author Juergen Hoeller
 * @since 24.02.2004
 * @see SqlMapClientTemplate
 * @see com.ibatis.sqlmap.client.SqlMapClient
 * @see com.ibatis.sqlmap.client.SqlMapExecutor
 * @deprecated as of Spring 3.2, in favor of the native Spring support
 * in the Mybatis follow-up project (http://code.google.com/p/mybatis/)
 */
@Deprecated
public interface SqlMapClientOperations {

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForObject(String)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	Object queryForObject(String statementName) throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForObject(String, Object)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	Object queryForObject(String statementName, Object parameterObject)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForObject(String, Object, Object)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	Object queryForObject(String statementName, Object parameterObject,	Object resultObject)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	List queryForList(String statementName) throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	List queryForList(String statementName, Object parameterObject)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, int, int)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	List queryForList(String statementName, int skipResults, int maxResults)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForList(String, Object, int, int)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	List queryForList(String statementName, Object parameterObject, int skipResults, int maxResults)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryWithRowHandler(String, RowHandler)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	void queryWithRowHandler(String statementName, RowHandler rowHandler)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryWithRowHandler(String, Object, RowHandler)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	void queryWithRowHandler(String statementName, Object parameterObject, RowHandler rowHandler)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	Map queryForMap(String statementName, Object parameterObject, String keyProperty)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#queryForMap(String, Object, String, String)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	Map queryForMap(String statementName, Object parameterObject, String keyProperty, String valueProperty)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#insert(String)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	Object insert(String statementName) throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#insert(String, Object)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	Object insert(String statementName, Object parameterObject) throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#update(String)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	int update(String statementName) throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#update(String, Object)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	int update(String statementName, Object parameterObject) throws DataAccessException;

	/**
	 * Convenience method provided by Spring: execute an update operation
	 * with an automatic check that the update affected the given required
	 * number of rows.
	 * @param statementName the name of the mapped statement
	 * @param parameterObject the parameter object
	 * @param requiredRowsAffected the number of rows that the update is
	 * required to affect
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	void update(String statementName, Object parameterObject, int requiredRowsAffected)
			throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#delete(String)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	int delete(String statementName) throws DataAccessException;

	/**
	 * @see com.ibatis.sqlmap.client.SqlMapExecutor#delete(String, Object)
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	int delete(String statementName, Object parameterObject) throws DataAccessException;

	/**
	 * Convenience method provided by Spring: execute a delete operation
	 * with an automatic check that the delete affected the given required
	 * number of rows.
	 * @param statementName the name of the mapped statement
	 * @param parameterObject the parameter object
	 * @param requiredRowsAffected the number of rows that the delete is
	 * required to affect
	 * @throws org.springframework.dao.DataAccessException in case of errors
	 */
	void delete(String statementName, Object parameterObject, int requiredRowsAffected)
			throws DataAccessException;

}
