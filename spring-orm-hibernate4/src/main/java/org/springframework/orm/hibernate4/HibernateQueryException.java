/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.orm.hibernate4;

import org.hibernate.QueryException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Hibernate-specific subclass of InvalidDataAccessResourceUsageException,
 * thrown on invalid HQL query syntax.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see SessionFactoryUtils#convertHibernateAccessException
 */
@SuppressWarnings("serial")
public class HibernateQueryException extends InvalidDataAccessResourceUsageException {

	public HibernateQueryException(QueryException ex) {
		super(ex.getMessage(), ex);
	}

	/**
	 * Return the HQL query string that was invalid.
	 */
	public String getQueryString() {
		return ((QueryException) getCause()).getQueryString();
	}

}
