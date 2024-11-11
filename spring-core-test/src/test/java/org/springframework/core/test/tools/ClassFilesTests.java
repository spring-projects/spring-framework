/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

/**
 * Tests for {@link ClassFiles}.
 *
 * @author Stephane Nicoll
 */
class ClassFilesTests {

	private static final ClassFile CLASS_FILE_1 = ClassFile.of(
			"com.example.Test1", new byte[] { 'a' });

	private static final ClassFile CLASS_FILE_2 = ClassFile.of(
			"com.example.Test2", new byte[] { 'b' });

	@Test
	void noneReturnsNone() {
		ClassFiles none = ClassFiles.none();
		assertThat(none).isNotNull();
		assertThat(none).isEmpty();
	}

	@Test
	void ofCreatesClassFiles() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
		assertThat(classFiles).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
	}

	@Test
	void andAddsClassFiles() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
		ClassFiles added = classFiles.and(CLASS_FILE_2);
		assertThat(classFiles).containsExactly(CLASS_FILE_1);
		assertThat(added).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
	}

	@Test
	void andClassFilesAddsClassFiles() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
		ClassFiles added = classFiles.and(ClassFiles.of(CLASS_FILE_2));
		assertThat(classFiles).containsExactly(CLASS_FILE_1);
		assertThat(added).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
	}

	@Test
	void iteratorIteratesClassFiles() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
		Iterator<ClassFile> iterator = classFiles.iterator();
		assertThat(iterator.next()).isEqualTo(CLASS_FILE_1);
		assertThat(iterator.next()).isEqualTo(CLASS_FILE_2);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void streamStreamsClassFiles() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
		assertThat(classFiles.stream()).containsExactly(CLASS_FILE_1, CLASS_FILE_2);
	}

	@Test
	void isEmptyWhenEmptyReturnsTrue() {
		ClassFiles classFiles = ClassFiles.of();
		assertThat(classFiles).isEmpty();
	}

	@Test
	void isEmptyWhenNotEmptyReturnsFalse() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
		assertThat(classFiles).isNotEmpty();
	}

	@Test
	void getWhenHasFileReturnsFile() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_1);
		assertThat(classFiles.get("com.example.Test1")).isNotNull();
	}

	@Test
	void getWhenMissingFileReturnsNull() {
		ClassFiles classFiles = ClassFiles.of(CLASS_FILE_2);
		assertThatObject(classFiles.get("com.example.another.Test2")).isNull();
	}

	@Test
	void equalsAndHashCode() {
		ClassFiles s1 = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
		ClassFiles s2 = ClassFiles.of(CLASS_FILE_1, CLASS_FILE_2);
		ClassFiles s3 = ClassFiles.of(CLASS_FILE_1);
		assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
		assertThatObject(s1).isEqualTo(s2).isNotEqualTo(s3);
	}

}
