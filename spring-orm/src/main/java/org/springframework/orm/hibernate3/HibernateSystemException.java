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

package org.springframework.orm.hibernate3;

import org.hibernate.HibernateException;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Hibernate-specific subclass of UncategorizedDataAccessException,
 * for Hibernate system errors that do not match any concrete
 * {@code org.springframework.dao} exceptions.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see SessionFactoryUtils#convertHibernateAccessException
 */
@SuppressWarnings("serial")
public class HibernateSystemException extends UncategorizedDataAccessException {

	/**
	 * Create a new HibernateSystemException,
	 * wrapping an arbitrary HibernateException.
	 * @param cause the HibernateException thrown
	 */
	public HibernateSystemException(HibernateException cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}

}
