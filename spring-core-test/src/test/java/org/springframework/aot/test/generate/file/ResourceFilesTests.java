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

class ReResourceFilesTests {

	private static final ResourceFile RESOURCE_FILE_1 = ResourceFile.of("path1",
			"resource1");

	private static final ResourceFile RESOURCE_FILE_2 = ResourceFile.of("path2",
			"resource2");

	@Test
	void noneReturnsNone() {
		ResourceFiles none = ResourceFiles.none();
		assertThat(none).isNotNull();
		assertThat(none.isEmpty()).isTrue();
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
		assertThat(resourceFiles.isEmpty()).isTrue();
	}

	@Test
	void isEmptyWhenNotEmptyReturnsFalse() {
		ResourceFiles resourceFiles = ResourceFiles.of(RESOURCE_FILE_1);
		assertThat(resourceFiles.isEmpty()).isFalse();
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
