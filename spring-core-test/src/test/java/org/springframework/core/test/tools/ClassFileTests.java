/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
