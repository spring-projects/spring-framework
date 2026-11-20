/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ui.freemarker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringTemplateLoader}.
 *
 * @author Sébastien Deleuze
 */
class SpringTemplateLoaderTests {

	@Test
	void findTemplateSourceResolvesTemplateInsidePath(@TempDir Path tempDir) throws Exception {
		Path templates = Files.createDirectory(tempDir.resolve("templates"));
		Files.writeString(templates.resolve("hello.ftl"), "Hello");
		SpringTemplateLoader loader = new SpringTemplateLoader(new DefaultResourceLoader(),
				"file:" + templates.toAbsolutePath() + File.separator);
		assertThat(loader.findTemplateSource("hello.ftl")).isNotNull();
	}

	@Test
	void findTemplateSourceRejectsBackslash(@TempDir Path tempDir) throws Exception {
		Path templates = Files.createDirectory(tempDir.resolve("templates"));
		Files.writeString(tempDir.resolve("other.txt"), "other");
		SpringTemplateLoader loader = new SpringTemplateLoader(new DefaultResourceLoader(),
				"file:" + templates.toAbsolutePath() + File.separator);
		assertThat(loader.findTemplateSource("..\\other.txt")).isNull();
	}

}
