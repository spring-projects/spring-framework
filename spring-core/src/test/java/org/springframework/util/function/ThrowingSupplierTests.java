/*
 * Copyright 2002-2022 the original author or authors.
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
				() -> modified.get()).withCauseInstanceOf(IOException.class);
	}

	@Test
	void ofModifiesThrowException() {
		ThrowingSupplier<Object> supplier = ThrowingSupplier.of(
				this::throwIOException, IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> supplier.get()).withCauseInstanceOf(IOException.class);
	}

	private Object throwIOException() throws IOException {
		throw new IOException();
	}

	private Object throwIllegalArgumentException() throws IOException {
		throw new IllegalArgumentException();
	}

}
