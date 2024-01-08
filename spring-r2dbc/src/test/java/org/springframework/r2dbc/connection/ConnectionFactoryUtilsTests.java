/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.r2dbc.connection;

import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import io.r2dbc.spi.R2dbcRollbackException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import org.junit.jupiter.api.Test;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.r2dbc.UncategorizedR2dbcException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionFactoryUtils}.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
class ConnectionFactoryUtilsTests {

	@Test
	void shouldTranslateTransientResourceException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcTransientResourceException(""));
		assertThat(exception).isExactlyInstanceOf(TransientDataAccessResourceException.class);
	}

	@Test
	void shouldTranslateRollbackException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcRollbackException());
		assertThat(exception).isExactlyInstanceOf(PessimisticLockingFailureException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcRollbackException("reason", "40001"));
		assertThat(exception).isExactlyInstanceOf(CannotAcquireLockException.class);
	}

	@Test
	void shouldTranslateTimeoutException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcTimeoutException());
		assertThat(exception).isExactlyInstanceOf(QueryTimeoutException.class);
	}

	@Test
	void shouldNotTranslateUnknownExceptions() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new MyTransientException());
		assertThat(exception).isExactlyInstanceOf(UncategorizedR2dbcException.class);
	}

	@Test
	void shouldTranslateNonTransientResourceException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcNonTransientResourceException());
		assertThat(exception).isExactlyInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	void shouldTranslateIntegrityViolationException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException());
		assertThat(exception).isExactlyInstanceOf(DataIntegrityViolationException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", "23505"));
		assertThat(exception).isExactlyInstanceOf(DuplicateKeyException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", "23000", 1));
		assertThat(exception).as("Oracle").isExactlyInstanceOf(DuplicateKeyException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", "23000", 301));
		assertThat(exception).as("SAP HANA").isExactlyInstanceOf(DuplicateKeyException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", "23000", 1062));
		assertThat(exception).as("MySQL/MariaDB").isExactlyInstanceOf(DuplicateKeyException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", "23000", 2601));
		assertThat(exception).as("MS SQL Server").isExactlyInstanceOf(DuplicateKeyException.class);

		exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", "23000", 2627));
		assertThat(exception).as("MS SQL Server").isExactlyInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void shouldTranslatePermissionDeniedException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcPermissionDeniedException());
		assertThat(exception).isExactlyInstanceOf(PermissionDeniedDataAccessException.class);
	}

	@Test
	void shouldTranslateBadSqlGrammarException() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcBadGrammarException());
		assertThat(exception).isExactlyInstanceOf(BadSqlGrammarException.class);
	}

	@Test
	void messageGeneration() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK",
				"SOME-SQL", new R2dbcTransientResourceException("MESSAGE"));
		assertThat(exception).isExactlyInstanceOf(
				TransientDataAccessResourceException.class).hasMessage("TASK; SQL [SOME-SQL]; MESSAGE");
	}

	@Test
	void messageGenerationNullSQL() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK", null,
				new R2dbcTransientResourceException("MESSAGE"));
		assertThat(exception).isExactlyInstanceOf(
				TransientDataAccessResourceException.class).hasMessage("TASK; MESSAGE");
	}

	@Test
	void messageGenerationNullMessage() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK",
				"SOME-SQL", new R2dbcTransientResourceException());
		assertThat(exception).isExactlyInstanceOf(
				TransientDataAccessResourceException.class).hasMessage("TASK; SQL [SOME-SQL]; null");
	}


	@SuppressWarnings("serial")
	private static class MyTransientException extends R2dbcException {
	}

}
