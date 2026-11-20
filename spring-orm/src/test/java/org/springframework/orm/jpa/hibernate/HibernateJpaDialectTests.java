/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void translateException() {
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
