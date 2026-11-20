/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.aot;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.aot.AbstractAotProcessor.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AbstractAotProcessor}.
 *
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 6.0
 */
class AotProcessorTests {

	@Test
	void springAotProcessingIsAvailableInDoProcess(@TempDir Path tempDir) {
		Settings settings = createTestSettings(tempDir);
		assertThat(new AbstractAotProcessor<String>(settings) {
			@Override
			protected String doProcess() {
				assertThat(System.getProperty("spring.aot.processing")).isEqualTo("true");
				return "Hello";
			}
		}.process()).isEqualTo("Hello");
	}

	@Test
	void builderRejectsMissingSourceOutput() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Settings.builder().build())
			.withMessageContaining("'sourceOutput'");
	}

	@Test
	void builderRejectsMissingResourceOutput(@TempDir Path tempDir) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Settings.builder().sourceOutput(tempDir).build())
			.withMessageContaining("'resourceOutput'");
	}

	@Test
	void builderRejectsMissingClassOutput(@TempDir Path tempDir) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Settings.builder()
				.sourceOutput(tempDir)
				.resourceOutput(tempDir)
				.build())
			.withMessageContaining("'classOutput'");
	}

	@Test
	void builderRejectsMissingGroupdId(@TempDir Path tempDir) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Settings.builder()
				.sourceOutput(tempDir)
				.resourceOutput(tempDir)
				.classOutput(tempDir)
				.build())
			.withMessageContaining("'groupId'");
	}

	@Test
	void builderRejectsEmptyGroupdId(@TempDir Path tempDir) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Settings.builder()
				.sourceOutput(tempDir)
				.resourceOutput(tempDir)
				.classOutput(tempDir)
				.groupId("           ")
				.build())
			.withMessageContaining("'groupId'");
	}

	@Test
	void builderRejectsMissingArtifactId(@TempDir Path tempDir) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Settings.builder()
				.sourceOutput(tempDir)
				.resourceOutput(tempDir)
				.classOutput(tempDir)
				.groupId("my-group")
				.build())
			.withMessageContaining("'artifactId'");
	}

	@Test
	void builderRejectsEmptyArtifactId(@TempDir Path tempDir) {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Settings.builder()
				.sourceOutput(tempDir)
				.resourceOutput(tempDir)
				.classOutput(tempDir)
				.groupId("my-group")
				.artifactId("           ")
				.build())
			.withMessageContaining("'artifactId'");
	}

	@Test
	void builderAcceptsRequiredSettings(@TempDir Path tempDir) {
		Settings settings = Settings.builder()
				.sourceOutput(tempDir)
				.resourceOutput(tempDir)
				.classOutput(tempDir)
				.groupId("my-group")
				.artifactId("my-artifact")
				.build();
		assertThat(settings).isNotNull();
		assertThat(settings.getSourceOutput()).isEqualTo(tempDir);
		assertThat(settings.getResourceOutput()).isEqualTo(tempDir);
		assertThat(settings.getClassOutput()).isEqualTo(tempDir);
		assertThat(settings.getGroupId()).isEqualTo("my-group");
		assertThat(settings.getArtifactId()).isEqualTo("my-artifact");
	}

	private static Settings createTestSettings(Path tempDir) {
		return Settings.builder()
				.sourceOutput(tempDir)
				.resourceOutput(tempDir)
				.classOutput(tempDir)
				.groupId("my-group")
				.artifactId("my-artifact")
				.build();
	}

}
