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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.nativex.FileNativeConfigurationWriter;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

/**
 * Command-line application that scans the provided classpath roots for Spring
 * integration test classes and then generates AOT artifacts for those test
 * classes in the provided output directories.
 *
 * <p><strong>For internal use only.</strong>
 *
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 6.0
 * @see TestClassScanner
 * @see TestContextAotGenerator
 * @see FileNativeConfigurationWriter
 * @see org.springframework.boot.AotProcessor
 */
public class TestAotProcessor {

	private final Path[] classpathRoots;

	private final Path sourceOutput;

	private final Path resourceOutput;

	private final Path classOutput;

	private final String groupId;

	private final String artifactId;


	/**
	 * Create a new processor for the specified test classpath roots and
	 * general settings.
	 *
	 * @param classpathRoots the classpath roots to scan for test classes
	 * @param sourceOutput the location of generated sources
	 * @param resourceOutput the location of generated resources
	 * @param classOutput the location of generated classes
	 * @param groupId the group ID of the application, used to locate
	 * {@code native-image.properties}
	 * @param artifactId the artifact ID of the application, used to locate
	 * {@code native-image.properties}
	 */
	public TestAotProcessor(Path[] classpathRoots, Path sourceOutput, Path resourceOutput, Path classOutput,
			String groupId, String artifactId) {

		this.classpathRoots = classpathRoots;
		this.sourceOutput = sourceOutput;
		this.resourceOutput = resourceOutput;
		this.classOutput = classOutput;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}


	/**
	 * Trigger processing of the test classes in the configured classpath roots.
	 */
	public void process() {
		deleteExistingOutput();
		performAotProcessing();
	}

	private void deleteExistingOutput() {
		deleteExistingOutput(this.sourceOutput, this.resourceOutput, this.classOutput);
	}

	private void deleteExistingOutput(Path... paths) {
		for (Path path : paths) {
			try {
				FileSystemUtils.deleteRecursively(path);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to delete existing output in '%s'".formatted(path), ex);
			}
		}
	}

	private void performAotProcessing() {
		TestClassScanner scanner = new TestClassScanner(Set.of(this.classpathRoots));
		Stream<Class<?>> testClasses = scanner.scan();

		GeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this::getRoot);
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		generator.processAheadOfTime(testClasses);

		writeHints(generator.getRuntimeHints());
	}

	private Path getRoot(Kind kind) {
		return switch (kind) {
			case SOURCE -> this.sourceOutput;
			case RESOURCE -> this.resourceOutput;
			case CLASS -> this.classOutput;
		};
	}

	private void writeHints(RuntimeHints hints) {
		FileNativeConfigurationWriter writer =
				new FileNativeConfigurationWriter(this.resourceOutput, this.groupId, this.artifactId);
		writer.write(hints);
	}


	public static void main(String[] args) {
		int requiredArgs = 6;
		Assert.isTrue(args.length >= requiredArgs, () ->
				"Usage: %s <classpathRoots> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId>"
					.formatted(TestAotProcessor.class.getName()));
		Path[] classpathRoots = Arrays.stream(args[0].split(File.pathSeparator)).map(Paths::get).toArray(Path[]::new);
		Path sourceOutput = Paths.get(args[1]);
		Path resourceOutput = Paths.get(args[2]);
		Path classOutput = Paths.get(args[3]);
		String groupId = args[4];
		String artifactId = args[5];
		new TestAotProcessor(classpathRoots, sourceOutput, resourceOutput, classOutput, groupId, artifactId).process();
	}

}
