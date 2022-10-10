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

package org.springframework.context.aot;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.nativex.FileNativeConfigurationWriter;
import org.springframework.util.FileSystemUtils;

/**
 * Abstract base class for filesystem-based ahead-of-time (AOT) processing.
 *
 * <p>Concrete implementations are typically used to kick off optimization of an
 * application or test suite in a build tool.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.0
 * @see FileSystemGeneratedFiles
 * @see FileNativeConfigurationWriter
 * @see org.springframework.context.aot.ContextAotProcessor
 * @see org.springframework.test.context.aot.TestAotProcessor
 */
public abstract class AbstractAotProcessor {

	private final Path sourceOutput;

	private final Path resourceOutput;

	private final Path classOutput;

	private final String groupId;

	private final String artifactId;


	/**
	 * Create a new processor instance.
	 * @param sourceOutput the location of generated sources
	 * @param resourceOutput the location of generated resources
	 * @param classOutput the location of generated classes
	 * @param groupId the group ID of the application, used to locate
	 * {@code native-image.properties}
	 * @param artifactId the artifact ID of the application, used to locate
	 * {@code native-image.properties}
	 */
	protected AbstractAotProcessor(Path sourceOutput, Path resourceOutput,
			Path classOutput, String groupId, String artifactId) {

		this.sourceOutput = sourceOutput;
		this.resourceOutput = resourceOutput;
		this.classOutput = classOutput;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	/**
	 * Get the output directory for generated sources.
	 */
	protected Path getSourceOutput() {
		return this.sourceOutput;
	}

	/**
	 * Get the output directory for generated resources.
	 */
	protected Path getResourceOutput() {
		return this.resourceOutput;
	}

	/**
	 * Get the output directory for generated classes.
	 */
	protected Path getClassOutput() {
		return this.classOutput;
	}

	/**
	 * Get the group ID of the application.
	 */
	protected String getGroupId() {
		return this.groupId;
	}

	/**
	 * Get the artifact ID of the application.
	 */
	protected String getArtifactId() {
		return this.artifactId;
	}

	/**
	 * Delete the source, resource, and class output directories.
	 */
	protected void deleteExistingOutput() {
		deleteExistingOutput(getSourceOutput(), getResourceOutput(), getClassOutput());
	}

	private void deleteExistingOutput(Path... paths) {
		for (Path path : paths) {
			try {
				FileSystemUtils.deleteRecursively(path);
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to delete existing output in '" + path + "'");
			}
		}
	}

	protected FileSystemGeneratedFiles createFileSystemGeneratedFiles() {
		return new FileSystemGeneratedFiles(this::getRoot);
	}

	private Path getRoot(Kind kind) {
		return switch (kind) {
			case SOURCE -> getSourceOutput();
			case RESOURCE -> getResourceOutput();
			case CLASS -> getClassOutput();
		};
	}

	protected void writeHints(RuntimeHints hints) {
		FileNativeConfigurationWriter writer =
				new FileNativeConfigurationWriter(getResourceOutput(), getGroupId(), getArtifactId());
		writer.write(hints);
	}

}
