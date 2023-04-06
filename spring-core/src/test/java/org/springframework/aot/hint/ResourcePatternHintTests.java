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
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("/")
				.rejects("/com/example", "/file.txt");
	}

	@Test
	void fileAtRoot() {
		ResourcePatternHint hint = new ResourcePatternHint("file.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("file.properties")
				.rejects("com/example/file.properties", "file.prop", "another-file.properties");
	}

	@Test
	void fileInDirectory() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/file.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties")
				.rejects("file.properties", "com/file.properties", "com/example/another-file.properties");
	}

	@Test
	void extension() {
		ResourcePatternHint hint = new ResourcePatternHint("*.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("file.properties", "com/example/file.properties")
				.rejects("file.prop", "com/example/file.prop");
	}

	@Test
	void extensionInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/*.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties", "com/example/another/file.properties")
				.rejects("file.properties", "com/file.properties");
	}

	@Test
	void anyFileInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/*", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties", "com/example/another/file.properties", "com/example/another")
				.rejects("file.properties", "com/file.properties");
	}

}
