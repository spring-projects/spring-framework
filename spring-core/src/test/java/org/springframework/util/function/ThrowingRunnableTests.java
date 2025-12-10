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

package org.springframework.util.function;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ThrowingRunnable}.
 *
 * @author Hosam Aly
 */
class ThrowingRunnableTests {

	@Test
	void applyWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingRunnable runnable = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(() -> runnable.run());
	}

	@Test
	void applyWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
		ThrowingRunnable runnable = this::throwIOException;
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				() -> runnable.run()).withCauseInstanceOf(IOException.class);
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingRunnable runnable = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(
				() -> runnable.run(IllegalStateException::new));
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
		ThrowingRunnable runnable = this::throwIOException;
		assertThatIllegalStateException().isThrownBy(() -> runnable.run(
				IllegalStateException::new)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void throwingModifiesThrownException() {
		ThrowingRunnable runnable = this::throwIOException;
		ThrowingRunnable modified = runnable.throwing(
				IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> modified.run()).withCauseInstanceOf(IOException.class);
	}

	@Test
	void ofModifiesThrownException() {
		ThrowingRunnable runnable = ThrowingRunnable.of(this::throwIOException,
				IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> runnable.run()).withCauseInstanceOf(IOException.class);
	}

	private void throwIOException() throws IOException {
		throw new IOException();
	}

	private void throwIllegalArgumentException() {
		throw new IllegalArgumentException();
	}

}
