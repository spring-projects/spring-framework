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

package org.springframework.aot.hint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ResourcePatternHint}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class ResourcePatternHintTests {

	@Test
	void patternWithLeadingSlashIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourcePatternHint("/file.properties", null))
				.withMessage("Resource pattern [/file.properties] must not start with a '/' unless it is the root directory");
	}

	@Test
	void rootDirectory() {
		ResourcePatternHint hint = new ResourcePatternHint("/", null);
		assertThat(hint.matches("/")).isTrue();
		assertThat(hint.matches("/com/example")).isFalse();
		assertThat(hint.matches("/file.txt")).isFalse();
	}

	@Test
	void fileAtRoot() {
		ResourcePatternHint hint = new ResourcePatternHint("file.properties", null);
		assertThat(hint.matches("file.properties")).isTrue();
		assertThat(hint.matches("com/example/file.properties")).isFalse();
		assertThat(hint.matches("file.prop")).isFalse();
		assertThat(hint.matches("another-file.properties")).isFalse();
	}

	@Test
	void fileInDirectory() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/file.properties", null);
		assertThat(hint.matches("com/example/file.properties")).isTrue();
		assertThat(hint.matches("file.properties")).isFalse();
		assertThat(hint.matches("com/file.properties")).isFalse();
		assertThat(hint.matches("com/example/another-file.properties")).isFalse();
	}

	@Test
	void extension() {
		ResourcePatternHint hint = new ResourcePatternHint("**/*.properties", null);
		assertThat(hint.matches("file.properties")).isTrue();
		assertThat(hint.matches("com/example/file.properties")).isTrue();
		assertThat(hint.matches("file.prop")).isFalse();
		assertThat(hint.matches("com/example/file.prop")).isFalse();
	}

	@Test
	void extensionInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/*.properties", null);
		assertThat(hint.matches("com/example/file.properties")).isTrue();
		assertThat(hint.matches("com/example/another/file.properties")).isFalse();
		assertThat(hint.matches("com/file.properties")).isFalse();
		assertThat(hint.matches("file.properties")).isFalse();
	}

	@Test
	void anyFileInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/**", null);
		assertThat(hint.matches("com/example/file.properties")).isTrue();
		assertThat(hint.matches("com/example/another/file.properties")).isTrue();
		assertThat(hint.matches("com/example/another")).isTrue();
		assertThat(hint.matches("file.properties")).isFalse();
		assertThat(hint.matches("com/file.properties")).isFalse();
	}

}
