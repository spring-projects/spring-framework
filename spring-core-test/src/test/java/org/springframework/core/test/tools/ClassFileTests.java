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

package org.springframework.core.test.tools;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassFile}.
 *
 * @author Stephane Nicoll
 */
class ClassFileTests {

	private static final byte[] TEST_CONTENT = new byte[]{'a'};

	@Test
	void ofNameAndByteArrayCreatesClass() {
		ClassFile classFile = ClassFile.of("com.example.Test", TEST_CONTENT);
		assertThat(classFile.getName()).isEqualTo("com.example.Test");
		assertThat(classFile.getContent()).isEqualTo(TEST_CONTENT);
	}

	@Test
	void ofNameAndInputStreamResourceCreatesClass() {
		ClassFile classFile = ClassFile.of("com.example.Test",
				new ByteArrayResource(TEST_CONTENT));
		assertThat(classFile.getName()).isEqualTo("com.example.Test");
		assertThat(classFile.getContent()).isEqualTo(TEST_CONTENT);
	}

	@Test
	void toClassNameWithPathToClassFile() {
		assertThat(ClassFile.toClassName("com/example/Test.class")).isEqualTo("com.example.Test");
	}

	@Test
	void toClassNameWithPathToTextFile() {
		assertThatIllegalArgumentException().isThrownBy(() -> ClassFile.toClassName("com/example/Test.txt"));
	}

}
