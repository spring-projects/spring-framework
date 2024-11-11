/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.util;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Brian Clozel
 * @author Juergen Hoeller
 */
class ResizableByteArrayOutputStreamTests {

	private static final int INITIAL_CAPACITY = 256;

	private ResizableByteArrayOutputStream baos;

	private byte[] helloBytes;


	@BeforeEach
	void setUp() throws Exception {
		this.baos = new ResizableByteArrayOutputStream(INITIAL_CAPACITY);
		this.helloBytes = "Hello World".getBytes(StandardCharsets.UTF_8);
	}


	@Test
	void resize() throws Exception {
		assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
		this.baos.write(helloBytes);
		int size = 64;
		this.baos.resize(size);
		assertThat(this.baos.capacity()).isEqualTo(size);
		assertByteArrayEqualsString(this.baos);
	}

	@Test
	void autoGrow() {
		assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
		for (int i = 0; i < 129; i++) {
			this.baos.write(0);
		}
		assertThat(this.baos.capacity()).isEqualTo(256);
	}

	@Test
	void grow() throws Exception {
		assertThat(this.baos.capacity()).isEqualTo(INITIAL_CAPACITY);
		this.baos.write(helloBytes);
		this.baos.grow(1000);
		assertThat(this.baos.capacity()).isEqualTo((this.helloBytes.length + 1000));
		assertByteArrayEqualsString(this.baos);
	}

	@Test
	void write() throws Exception{
		this.baos.write(helloBytes);
		assertByteArrayEqualsString(this.baos);
	}

	@Test
	void failResize() throws Exception{
		this.baos.write(helloBytes);
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.baos.resize(5));
	}


	private void assertByteArrayEqualsString(ResizableByteArrayOutputStream actual) {
		assertThat(actual.toByteArray()).isEqualTo(helloBytes);
	}

}
