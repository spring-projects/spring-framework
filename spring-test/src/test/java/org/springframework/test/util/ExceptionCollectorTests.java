/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util;

import org.junit.jupiter.api.Test;

import org.springframework.test.util.ExceptionCollector.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link ExceptionCollector}.
 *
 * @author Sam Brannen
 * @since 5.3.10
 */
class ExceptionCollectorTests {

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
