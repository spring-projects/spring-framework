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

package org.springframework.r2dbc.connection;

import java.util.List;

import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import io.r2dbc.spi.R2dbcRollbackException;
import io.r2dbc.spi.R2dbcTimeoutException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

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
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
	}

	static final List<Arguments> duplicateKeyErrorCodes = List.of(
			arguments("Oracle", "23505", 0),
			arguments("Oracle", "23000", 1),
			arguments("SAP HANA", "23000", 301),
			arguments("MySQL/MariaDB", "23000", 1062),
			arguments("MS SQL Server", "23000", 2601),
			arguments("MS SQL Server", "23000", 2627),
			arguments("Informix", "23000", -239),
			arguments("Informix", "23000", -268)
		);

	@ParameterizedTest
	@FieldSource("duplicateKeyErrorCodes")
	void shouldTranslateIntegrityViolationExceptionToDuplicateKeyException(String db, String sqlState, int errorCode) {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("", "",
				new R2dbcDataIntegrityViolationException("reason", sqlState, errorCode));
		assertThat(exception).as(db).isExactlyInstanceOf(DuplicateKeyException.class);
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
		assertThat(exception)
				.isExactlyInstanceOf(TransientDataAccessResourceException.class)
				.hasMessage("TASK; SQL [SOME-SQL]; MESSAGE");
	}

	@Test
	void messageGenerationNullSQL() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK", null,
				new R2dbcTransientResourceException("MESSAGE"));
		assertThat(exception)
				.isExactlyInstanceOf(TransientDataAccessResourceException.class)
				.hasMessage("TASK; MESSAGE");
	}

	@Test
	void messageGenerationNullMessage() {
		Exception exception = ConnectionFactoryUtils.convertR2dbcException("TASK",
				"SOME-SQL", new R2dbcTransientResourceException());
		assertThat(exception)
				.isExactlyInstanceOf(TransientDataAccessResourceException.class)
				.hasMessage("TASK; SQL [SOME-SQL]; null");
	}


	@SuppressWarnings("serial")
	private static class MyTransientException extends R2dbcException {
	}

}
