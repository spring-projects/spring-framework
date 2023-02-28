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
 * Tests for {@link ThrowingConsumer}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ThrowingConsumerTests {

	@Test
	void applyWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingConsumer<Object> consumer = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(() -> consumer.accept(this));
	}

	@Test
	void applyWhenThrowingCheckedExceptionThrowsWrapperRuntimeException() {
		ThrowingConsumer<Object> consumer = this::throwIOException;
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				() -> consumer.accept(this)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingUncheckedExceptionThrowsOriginal() {
		ThrowingConsumer<Object> consumer = this::throwIllegalArgumentException;
		assertThatIllegalArgumentException().isThrownBy(
				() -> consumer.accept(this, IllegalStateException::new));
	}

	@Test
	void applyWithExceptionWrapperWhenThrowingCheckedExceptionThrowsWrapper() {
		ThrowingConsumer<Object> consumer = this::throwIOException;
		assertThatIllegalStateException().isThrownBy(() -> consumer.accept(this,
				IllegalStateException::new)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void throwingModifiesThrownException() {
		ThrowingConsumer<Object> consumer = this::throwIOException;
		ThrowingConsumer<Object> modified = consumer.throwing(
				IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> modified.accept(this)).withCauseInstanceOf(IOException.class);
	}

	@Test
	void ofModifiesThrownException() {
		ThrowingConsumer<Object> consumer = ThrowingConsumer.of(this::throwIOException,
				IllegalStateException::new);
		assertThatIllegalStateException().isThrownBy(
				() -> consumer.accept(this)).withCauseInstanceOf(IOException.class);
	}

	private void throwIOException(Object o) throws IOException {
		throw new IOException();
	}

	private void throwIllegalArgumentException(Object o) throws IOException {
		throw new IllegalArgumentException();
	}

}
