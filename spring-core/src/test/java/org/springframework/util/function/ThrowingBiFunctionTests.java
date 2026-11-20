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
 * Tests for {@link ThrowingBiFunction}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ThrowingBiFunctionTests {

	@Test
	void applyWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingBiFunction<Object, Object, Object> function = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(() -> function.apply(this, this));
	}

	@Test
	void applyWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
		ThrowingBiFunction<Object, Object, Object> function = this::throwIOException;
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				() -> function.apply(this, this)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingBiFunction<Object, Object, Object> function = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(
				() -> function.apply(this, this, IllegalStateException::new));
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
		ThrowingBiFunction<Object, Object, Object> function = this::throwIOException;
		assertThatIllegalStateException().isThrownBy(() -> function.apply(this, this,
				IllegalStateException::new)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void throwingModifiesThrownException() {
		ThrowingBiFunction<Object, Object, Object> function = this::throwIOException;
		ThrowingBiFunction<Object, Object, Object> modified = function.throwing(
				IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> modified.apply(this, this)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void ofModifiesThrowException() {
		ThrowingBiFunction<Object, Object, Object> function = ThrowingBiFunction.of(
				this::throwIOException, IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> function.apply(this, this)).withCauseInstanceOf(IOException.class);
	}

	private Object throwIOException(Object o, Object u) throws IOException {
		throw new IOException();
	}

	private Object throwIllegalArgumentException(Object o, Object u) {
		throw new IllegalArgumentException();
	}

}
