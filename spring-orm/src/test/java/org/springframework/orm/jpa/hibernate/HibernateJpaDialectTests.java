/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.orm.jpa.hibernate;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.HibernateException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.junit.jupiter.api.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class HibernateJpaDialectTests {

	private final JpaDialect dialect = new HibernateJpaDialect();


	@Test
	void testTranslateException() {
		// Plain JPA exception
		RuntimeException ex = new OptimisticLockException();
		assertThat(dialect.translateExceptionIfPossible(ex))
				.isInstanceOf(JpaOptimisticLockingFailureException.class).hasCause(ex);

		// Hibernate-specific exception
		ex = new OptimisticEntityLockException("", "");
		assertThat(dialect.translateExceptionIfPossible(ex))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class).hasCause(ex);

		// Nested Hibernate-specific exception
		ex = new HibernateException(new OptimisticEntityLockException("", ""));
		assertThat(dialect.translateExceptionIfPossible(ex))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class).hasCause(ex);

		// IllegalArgumentException
		ex = new IllegalArgumentException("");
		assertThat(dialect.translateExceptionIfPossible(ex))
				.isInstanceOf(InvalidDataAccessApiUsageException.class).hasCause(ex);
	}

}
