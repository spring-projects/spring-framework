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

package org.springframework.build.multirelease;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultiReleaseJarPlugin}
 */
public class MultiReleaseJarPluginTests {

	private File projectDir;

	private File buildFile;

	@BeforeEach
	void setup(@TempDir File projectDir) {
		this.projectDir = projectDir;
		this.buildFile = new File(this.projectDir, "build.gradle");
	}

	@Test
	void configureSourceSets() throws IOException {
		writeBuildFile("""
				plugins {
					id 'java'
					id 'org.springframework.build.multiReleaseJar'
				}
				multiRelease { releaseVersions 21, 24 }
				task printSourceSets {
					doLast {
						sourceSets.all { println it.name }
					}
				}
				""");
		BuildResult buildResult = runGradle("printSourceSets");
		assertThat(buildResult.getOutput()).contains("main", "test", "java21", "java21Test", "java24", "java24Test");
	}

	@Test
	void configureToolchainReleaseVersion() throws IOException {
		writeBuildFile("""
				plugins {
					id 'java'
					id 'org.springframework.build.multiReleaseJar'
				}
				multiRelease { releaseVersions 21 }
				task printReleaseVersion {
					doLast {
						tasks.all { println it.name }
						tasks.named("compileJava21Java") {
							println "compileJava21Java releaseVersion: ${it.options.release.get()}"
						}
						tasks.named("compileJava21TestJava") {
							println "compileJava21TestJava releaseVersion: ${it.options.release.get()}"
						}
					}
				}
				""");

		BuildResult buildResult = runGradle("printReleaseVersion");
		assertThat(buildResult.getOutput()).contains("compileJava21Java releaseVersion: 21")
				.contains("compileJava21TestJava releaseVersion: 21");
	}

	@Test
	void packageInJar() throws IOException {
		writeBuildFile("""
				plugins {
					id 'java'
					id 'org.springframework.build.multiReleaseJar'
				}
				version = '1.2.3'
				multiRelease { releaseVersions 17 }
				""");
		writeClass("src/main/java17", "Main.java", """
				public class Main {}
				""");
		BuildResult buildResult = runGradle("assemble");
		File file = new File(this.projectDir, "/build/libs/" + this.projectDir.getName() + "-1.2.3.jar");
		assertThat(file).exists();
		try (JarFile jar = new JarFile(file)) {
			Attributes mainAttributes = jar.getManifest().getMainAttributes();
			assertThat(mainAttributes.getValue("Multi-Release")).isEqualTo("true");

			assertThat(jar.entries().asIterator()).toIterable()
					.anyMatch(entry -> entry.getName().equals("META-INF/versions/17/Main.class"));
		}
	}

	private void writeBuildFile(String buildContent) throws IOException {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.print(buildContent);
		}
	}

	private void writeClass(String path, String fileName, String fileContent) throws IOException {
		Path folder = this.projectDir.toPath().resolve(path);
		Files.createDirectories(folder);
		Path filePath = folder.resolve(fileName);
		Files.createFile(filePath);
		Files.writeString(filePath, fileContent);
	}

	private BuildResult runGradle(String... args) {
		return GradleRunner.create().withProjectDir(this.projectDir).withArguments(args).withPluginClasspath().build();
	}

}
