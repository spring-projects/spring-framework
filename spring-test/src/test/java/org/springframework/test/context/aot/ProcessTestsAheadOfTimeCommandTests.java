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

package org.springframework.test.context.aot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProcessTestsAheadOfTimeCommand}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class ProcessTestsAheadOfTimeCommandTests extends AbstractAotTests {

	@Test
	void execute(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
		Path sourcesOutputPath = tempDir.resolve("src/test/java").toAbsolutePath();
		Path resourcesOutputPath = tempDir.resolve("src/test/resources").toAbsolutePath();
		String testPackage = "org.springframework.test.context.aot.samples.basic";
		String[] args = {
				"--sources-out=" + sourcesOutputPath,
				"--resources-out=" + resourcesOutputPath,
				"--packages=" + testPackage,
				classpathRoot().toString()
			};
		int exitCode = ProcessTestsAheadOfTimeCommand.execute(args);
		assertThat(exitCode).as("exit code").isZero();

		assertThat(findFiles(sourcesOutputPath)).containsExactlyInAnyOrder(
				expectedSourceFilesForBasicSpringTests);

		assertThat(findFiles(resourcesOutputPath)).contains(
				"META-INF/native-image/reflect-config.json",
				"META-INF/native-image/resource-config.json",
				"META-INF/native-image/proxy-config.json");
	}

	private static List<String> findFiles(Path outputPath) throws IOException {
		int lengthOfOutputPath = outputPath.toFile().getAbsolutePath().length() + 1;
		return Files.find(outputPath, Integer.MAX_VALUE,
					(path, attributes) -> attributes.isRegularFile())
				.map(Path::toAbsolutePath)
				.map(Path::toString)
				.map(path -> path.substring(lengthOfOutputPath))
				.toList();
	}

}
