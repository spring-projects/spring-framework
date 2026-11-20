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
 * Tests for {@link ThrowingSupplier}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ThrowingSupplierTests {

	@Test
	void getWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingSupplier<Object> supplier = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(supplier::get);
	}

	@Test
	void getWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
		ThrowingSupplier<Object> supplier = this::throwIOException;
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				supplier::get).withCauseInstanceOf(IOException.class);
	}

	@Test
	void getWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingSupplier<Object> supplier = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(
				() -> supplier.get(IllegalStateException::new));
	}

	@Test
	void getWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
		ThrowingSupplier<Object> supplier = this::throwIOException;
		assertThatIllegalStateException().isThrownBy(
				() -> supplier.get(IllegalStateException::new)).withCauseInstanceOf(
						IOException.class);
	}

	@Test
	void throwingModifiesThrownException() {
		ThrowingSupplier<Object> supplier = this::throwIOException;
		ThrowingSupplier<Object> modified = supplier.throwing(
				IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				modified::get).withCauseInstanceOf(IOException.class);
	}

	@Test
	void ofModifiesThrowException() {
		ThrowingSupplier<Object> supplier = ThrowingSupplier.of(
				this::throwIOException, IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				supplier::get).withCauseInstanceOf(IOException.class);
	}

	private Object throwIOException() throws IOException {
		throw new IOException();
	}

	private Object throwIllegalArgumentException() {
		throw new IllegalArgumentException();
	}

}
