/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.util;

import org.junit.jupiter.api.Test;

import org.springframework.test.util.ExceptionCollector.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for {@link ExceptionCollector}.
 *
 * @author Sam Brannen
 * @since 5.3.10
 */
public class ExceptionCollectorTests {

	private static final char EOL = '\n';

	private final ExceptionCollector collector = new ExceptionCollector();


	@Test
	void noExceptions() {
		this.collector.execute(() -> {});

		assertThat(this.collector.getExceptions()).isEmpty();
		assertThatNoException().isThrownBy(this.collector::assertEmpty);
	}

	@Test
	void oneError() {
		this.collector.execute(error());

		assertOneFailure(Error.class, "error");
	}

	@Test
	void oneAssertionError() {
		this.collector.execute(assertionError());

		assertOneFailure(AssertionError.class, "assertion");
	}

	@Test
	void oneCheckedException() {
		this.collector.execute(checkedException());

		assertOneFailure(Exception.class, "checked");
	}

	@Test
	void oneUncheckedException() {
		this.collector.execute(uncheckedException());

		assertOneFailure(RuntimeException.class, "unchecked");
	}

	@Test
	void oneThrowable() {
		this.collector.execute(throwable());

		assertThatExceptionOfType(AssertionError.class)
			.isThrownBy(this.collector::assertEmpty)
			.withMessage("throwable")
			.withCauseExactlyInstanceOf(Throwable.class)
			.satisfies(error -> assertThat(error.getCause()).hasMessage("throwable"))
			.satisfies(error -> assertThat(error).hasNoSuppressedExceptions());
	}

	private void assertOneFailure(Class<? extends Throwable> expectedType, String failureMessage) {
		assertThatExceptionOfType(expectedType)
			.isThrownBy(this.collector::assertEmpty)
			.satisfies(exception ->
				assertThat(exception)
					.isExactlyInstanceOf(expectedType)
					.hasNoSuppressedExceptions()
					.hasNoCause()
					.hasMessage(failureMessage));
	}

	@Test
	void multipleFailures() {
		this.collector.execute(assertionError());
		this.collector.execute(checkedException());
		this.collector.execute(uncheckedException());
		this.collector.execute(error());
		this.collector.execute(throwable());

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(this.collector::assertEmpty)
				.withMessage("Multiple Exceptions (5):" + EOL + //
					"assertion" + EOL + //
					"checked" + EOL + //
					"unchecked" + EOL + //
					"error" + EOL + //
					"throwable"//
				)
				.satisfies(exception ->
					assertThat(exception.getSuppressed()).extracting(Object::getClass).map(Class::getSimpleName)
						.containsExactly("AssertionError", "Exception", "RuntimeException", "Error", "Throwable"));
	}

	private Executable throwable() {
		return () -> {
			throw new Throwable("throwable");
		};
	}

	private Executable error() {
		return () -> {
			throw new Error("error");
		};
	}

	private Executable assertionError() {
		return () -> {
			throw new AssertionError("assertion");
		};
	}

	private Executable checkedException() {
		return () -> {
			throw new Exception("checked");
		};
	}

	private Executable uncheckedException() {
		return () -> {
			throw new RuntimeException("unchecked");
		};
	}

}
