/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

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
		assertThat(none).isEmpty();
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
		assertThat(sourceFiles).isEmpty();
	}

	@Test
	void isEmptyWhenNotEmptyReturnsFalse() {
		SourceFiles sourceFiles = SourceFiles.of(SOURCE_FILE_1);
		assertThat(sourceFiles).isNotEmpty();
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
