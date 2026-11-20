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
 * Tests for {@link ResourceFilesTests}.
 *
 * @author Phillip Webb
 */
class ResourceFilesTests {

	private static final ResourceFile RESOURCE_FILE_1 = ResourceFile.of("path1",
			"resource1");

	private static final ResourceFile RESOURCE_FILE_2 = ResourceFile.of("path2",
			"resource2");

	@Test
	void noneReturnsNone() {
		ResourceFiles none = ResourceFiles.none();
		assertThat(none).isNotNull();
		assertThat(none).isEmpty();
	}

	@Test
	void ofCreatesResourceFiles() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
		assertThat(resourceFiles).containsExactly(RESOURCE_FILE_1, RESOURCE_FILE_2);
	}

	@Test
	void andAddsResourceFiles() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
		ResourceFiles added = resourceFiles.and(RESOURCE_FILE_2);
		assertThat(resourceFiles).containsExactly(RESOURCE_FILE_1);
		assertThat(added).containsExactly(RESOURCE_FILE_1, RESOURCE_FILE_2);
	}

	@Test
	void andResourceFilesAddsResourceFiles() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
		ResourceFiles added = resourceFiles.and(ResourceFiles.of(RESOURCE_FILE_2));
		assertThat(resourceFiles).containsExactly(RESOURCE_FILE_1);
		assertThat(added).containsExactly(RESOURCE_FILE_1, RESOURCE_FILE_2);
	}

	@Test
	void iteratorIteratesResourceFiles() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
		Iterator<ResourceFile> iterator = resourceFiles.iterator();
		assertThat(iterator.next()).isEqualTo(RESOURCE_FILE_1);
		assertThat(iterator.next()).isEqualTo(RESOURCE_FILE_2);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void streamStreamsResourceFiles() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
		assertThat(resourceFiles.stream()).containsExactly(RESOURCE_FILE_1,
				RESOURCE_FILE_2);
	}

	@Test
	void isEmptyWhenEmptyReturnsTrue() {
		ResourceFiles resourceFiles = ResourceFiles.of();
		assertThat(resourceFiles).isEmpty();
	}

	@Test
	void isEmptyWhenNotEmptyReturnsFalse() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
		assertThat(resourceFiles).isNotEmpty();
	}

	@Test
	void getWhenHasFileReturnsFile() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
		assertThat(resourceFiles.get("path1")).isNotNull();
	}

	@Test
	void getWhenMissingFileReturnsNull() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_2);
		assertThatObject(resourceFiles.get("path1")).isNull();
	}

	@Test
	void getSingleWhenHasNoFilesThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> ResourceFiles.none().getSingle());
	}

	@Test
	void getSingleWhenHasMultipleFilesThrowsException() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
		assertThatIllegalStateException().isThrownBy(resourceFiles::getSingle);
	}

	@Test
	void getSingleWhenHasSingleFileReturnsFile() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
		assertThat(resourceFiles.getSingle()).isEqualTo(RESOURCE_FILE_1);
	}

	@Test
	void equalsAndHashCode() {
		ResourceFiles s1 = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
		ResourceFiles s2 = ResourceFiles.of(RESOURCE_FILE_1, RESOURCE_FILE_2);
		ResourceFiles s3 = ResourceFiles.of(RESOURCE_FILE_1);
		assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
		assertThatObject(s1).isEqualTo(s2).isNotEqualTo(s3);
	}

}
