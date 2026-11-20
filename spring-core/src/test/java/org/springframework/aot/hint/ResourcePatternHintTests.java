/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
