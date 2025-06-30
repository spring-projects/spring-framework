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
