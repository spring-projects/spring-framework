/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceFile}.
 *
 * @author Phillip Webb
 */
class ResourceFileTests {

	@Test
	void ofPathAndCharSequenceCreatesResource() {
		ResourceFile file = ResourceFile.of("path", "test");
		assertThat(file.getPath()).isEqualTo("path");
		assertThat(file.getContent()).isEqualTo("test");
	}

	@Test
	void ofPathAndWritableContentCreatesResource() {
		ResourceFile file = ResourceFile.of("path", appendable -> appendable.append("test"));
		assertThat(file.getPath()).isEqualTo("path");
		assertThat(file.getContent()).isEqualTo("test");
	}

	@Test
	void assertThatUsesResourceFileAssert() {
		ResourceFile file = ResourceFile.of("path", appendable -> appendable.append("test"));
		assertThat(file).hasContent("test");
	}

}
