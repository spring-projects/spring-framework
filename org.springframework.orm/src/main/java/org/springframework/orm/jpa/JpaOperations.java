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

package org.springframework.orm.jpa;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;

/**
 * Interface that specifies a basic set of JPA operations,
 * implemented by {@link JpaTemplate}. Not often used, but a useful
 * option to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Defines <code>JpaTemplate</code>'s data access methods that mirror
 * various {@link javax.persistence.EntityManager} methods. Users are
 * strongly encouraged to read the JPA <code>EntityManager</code>
 * javadocs for details on the semantics of those methods.
 *
 * <p>Note that lazy loading will just work with an open JPA
 * <code>EntityManager</code>, either within a managed transaction or within
 * {@link org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter}/
 * {@link org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor}.
 * Furthermore, some operations just make sense within transactions,
 * for example: <code>flush</code>, <code>clear</code>.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see JpaTemplate
 * @see javax.persistence.EntityManager
 * @see JpaTransactionManager
 * @see JpaDialect
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter
 * @see org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
 */
public interface JpaOperations {

	<T> T execute(JpaCallback<T> action) throws DataAccessException;

	List executeFind(JpaCallback<?> action) throws DataAccessException;

	<T> T find(Class<T> entityClass, Object id) throws DataAccessException;

	<T> T getReference(Class<T> entityClass, Object id) throws DataAccessException;

	boolean contains(Object entity) throws DataAccessException;

	void refresh(Object entity) throws DataAccessException;

	void persist(Object entity) throws DataAccessException;

	<T> T merge(T entity) throws DataAccessException;

	void remove(Object entity) throws DataAccessException;

	void flush() throws DataAccessException;

	List find(String queryString) throws DataAccessException;

	List find(String queryString, Object... values) throws DataAccessException;

	List findByNamedParams(String queryString, Map<String, ?> params) throws DataAccessException;

	List findByNamedQuery(String queryName) throws DataAccessException;

	List findByNamedQuery(String queryName, Object... values) throws DataAccessException;

	List findByNamedQueryAndNamedParams(String queryName, Map<String, ?> params) throws DataAccessException;

}
