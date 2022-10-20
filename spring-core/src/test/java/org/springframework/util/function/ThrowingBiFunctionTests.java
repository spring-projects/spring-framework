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

	private Object throwIllegalArgumentException(Object o, Object u) throws IOException {
		throw new IllegalArgumentException();
	}

}
