/*
 * Copyright 2002-2009 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

/**
 * Callback interface for JPA code. To be used with {@link JpaTemplate}'s
 * execution method, often as anonymous classes within a method implementation.
 * A typical implementation will call <code>EntityManager.find/merge</code>
 * to perform some operations on persistent objects.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.orm.jpa.JpaTemplate
 * @see org.springframework.orm.jpa.JpaTransactionManager
 */
public interface JpaCallback<T> {

	/**
	 * Gets called by <code>JpaTemplate.execute</code> with an active
	 * JPA <code>EntityManager</code>. Does not need to care about activating
	 * or closing the <code>EntityManager</code>, or handling transactions.
	 *
	 * <p>Note that JPA callback code will not flush any modifications to the
	 * database if not executed within a transaction. Thus, you need to make
	 * sure that JpaTransactionManager has initiated a JPA transaction when
	 * the callback gets called, at least if you want to write to the database.
	 *
	 * <p>Allows for returning a result object created within the callback,
	 * i.e. a domain object or a collection of domain objects.
	 * A thrown custom RuntimeException is treated as an application exception:
	 * It gets propagated to the caller of the template.
	 *
	 * @param em active EntityManager
	 * @return a result object, or <code>null</code> if none
	 * @throws PersistenceException if thrown by the JPA API
	 * @see org.springframework.orm.jpa.JpaTemplate#execute
	 * @see org.springframework.orm.jpa.JpaTemplate#executeFind
	 */
	T doInJpa(EntityManager em) throws PersistenceException;

}
