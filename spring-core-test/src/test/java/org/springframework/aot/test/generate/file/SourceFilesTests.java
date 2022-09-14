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

package org.springframework.aot.test.generate.file;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatObject;

/**
 * Tests for {@link SourceFiles}.
 *
 * @author Phillip Webb
 */
class SourceFilesTests {

	private static final SourceFile SOURCE_FILE_1 = SourceFile.of(
			"public class Test1 {}");

	private static final SourceFile SOURCE_FILE_2 = SourceFile.of(
			"public class Test2 {}");

	@Test
	void noneReturnsNone() {
		SourceFiles none = SourceFiles.none();
		assertThat(none).isNotNull();
		assertThat(none.isEmpty()).isTrue();
	}

	@Test
	void ofCreatesSourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
		assertThat(sourceFiles).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
	}

	@Test
	void andAddsSourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
		SourceFiles added = sourceFiles.and(SOURCE_FILE_2);
		assertThat(sourceFiles).containsExactly(SOURCE_FILE_1);
		assertThat(added).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
	}

	@Test
	void andSourceFilesAddsSourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
		SourceFiles added = sourceFiles.and(SourceFiles.of(SOURCE_FILE_2));
		assertThat(sourceFiles).containsExactly(SOURCE_FILE_1);
		assertThat(added).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
	}

	@Test
	void iteratorIteratesSourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
		Iterator<SourceFile> iterator = sourceFiles.iterator();
		assertThat(iterator.next()).isEqualTo(SOURCE_FILE_1);
		assertThat(iterator.next()).isEqualTo(SOURCE_FILE_2);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void streamStreamsSourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
		assertThat(sourceFiles.stream()).containsExactly(SOURCE_FILE_1, SOURCE_FILE_2);
	}

	@Test
	void isEmptyWhenEmptyReturnsTrue() {
		SourceFiles sourceFiles = SourceFiles.of();
		assertThat(sourceFiles.isEmpty()).isTrue();
	}

	@Test
	void isEmptyWhenNotEmptyReturnsFalse() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
		assertThat(sourceFiles.isEmpty()).isFalse();
	}

	@Test
	void getWhenHasFileReturnsFile() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
		assertThat(sourceFiles.get("Test1.java")).isNotNull();
	}

	@Test
	void getWhenMissingFileReturnsNull() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_2);
		assertThatObject(sourceFiles.get("Test1.java")).isNull();
	}

	@Test
	void getSingleWhenHasNoFilesThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> SourceFiles.none().getSingle());
	}

	@Test
	void getSingleWhenHasMultipleFilesThrowsException() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
		assertThatIllegalStateException().isThrownBy(sourceFiles::getSingle);
	}

	@Test
	void getSingleWhenHasSingleFileReturnsFile() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
		assertThat(sourceFiles.getSingle()).isEqualTo(SOURCE_FILE_1);
	}

	@Test
	void equalsAndHashCode() {
		SourceFiles s1 = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
		SourceFiles s2 = SourceFiles.of(SOURCE_FILE_1, SOURCE_FILE_2);
		SourceFiles s3 = SourceFiles.of(SOURCE_FILE_1);
		assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
		assertThatObject(s1).isEqualTo(s2).isNotEqualTo(s3);
	}

}
