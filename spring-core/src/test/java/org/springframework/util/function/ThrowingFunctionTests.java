/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.function;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ThrowingFunction}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ThrowingFunctionTests {

	@Test
	void applyWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingFunction<Object, Object> function = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(() -> function.apply(this));
	}

	@Test
	void applyWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
		ThrowingFunction<Object, Object> function = this::throwIOException;
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				() -> function.apply(this)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingFunction<Object, Object> function = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(
				() -> function.apply(this, IllegalStateException::new));
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
		ThrowingFunction<Object, Object> function = this::throwIOException;
		assertThatIllegalStateException().isThrownBy(() -> function.apply(this,
				IllegalStateException::new)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void throwingModifiesThrownException() {
		ThrowingFunction<Object, Object> function = this::throwIOException;
		ThrowingFunction<Object, Object> modified = function.throwing(
				IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> modified.apply(this)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void ofModifiesThrowException() {
		ThrowingFunction<Object, Object> function = ThrowingFunction.of(
				this::throwIOException, IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> function.apply(this)).withCauseInstanceOf(IOException.class);
	}

	private Object throwIOException(Object o) throws IOException {
		throw new IOException();
	}

	private Object throwIllegalArgumentException(Object o) {
		throw new IllegalArgumentException();
	}

}
