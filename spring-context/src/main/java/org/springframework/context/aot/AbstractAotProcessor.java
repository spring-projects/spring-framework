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
import org.springframework.lang.Nullable;
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

	private final Settings settings;


	/**
	 * Create a new processor instance with the supplied {@linkplain Settings settings}.
	 */
	protected AbstractAotProcessor(Settings settings) {
		this.settings = settings;
	}


	/**
	 * Get the {@linkplain Settings settings} for this AOT processor.
	 */
	protected Settings getSettings() {
		return this.settings;
	}

	/**
	 * Delete the source, resource, and class output directories.
	 */
	protected void deleteExistingOutput() {
		deleteExistingOutput(getSettings().getSourceOutput(),
				getSettings().getResourceOutput(), getSettings().getClassOutput());
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
			case SOURCE -> getSettings().getSourceOutput();
			case RESOURCE -> getSettings().getResourceOutput();
			case CLASS -> getSettings().getClassOutput();
		};
	}

	protected void writeHints(RuntimeHints hints) {
		FileNativeConfigurationWriter writer = new FileNativeConfigurationWriter(
				getSettings().getResourceOutput(), getSettings().getGroupId(), getSettings().getArtifactId());
		writer.write(hints);
	}

	/**
	 * Common settings for AOT processors.
	 */
	public static class Settings {

		@Nullable
		private Path sourceOutput;

		@Nullable
		private Path resourceOutput;

		@Nullable
		private Path classOutput;

		@Nullable
		private String groupId;

		@Nullable
		private String artifactId;


		/**
		 * Set the output directory for generated sources.
		 * @param sourceOutput the location of generated sources
		 * @return this settings object for method chaining
		 */
		public Settings setSourceOutput(Path sourceOutput) {
			this.sourceOutput = sourceOutput;
			return this;
		}

		/**
		 * Get the output directory for generated sources.
		 */
		@Nullable
		public Path getSourceOutput() {
			return this.sourceOutput;
		}

		/**
		 * Set the output directory for generated resources.
		 * @param resourceOutput the location of generated resources
		 * @return this settings object for method chaining
		 */
		public Settings setResourceOutput(Path resourceOutput) {
			this.resourceOutput = resourceOutput;
			return this;
		}

		/**
		 * Get the output directory for generated resources.
		 */
		@Nullable
		public Path getResourceOutput() {
			return this.resourceOutput;
		}

		/**
		 * Set the output directory for generated classes.
		 * @param classOutput the location of generated classes
		 * @return this settings object for method chaining
		 */
		public Settings setClassOutput(Path classOutput) {
			this.classOutput = classOutput;
			return this;
		}

		/**
		 * Get the output directory for generated classes.
		 */
		@Nullable
		public Path getClassOutput() {
			return this.classOutput;
		}

		/**
		 * Set the group ID of the application.
		 * @param groupId the group ID of the application, used to locate
		 * {@code native-image.properties}
		 * @return this settings object for method chaining
		 */
		public Settings setGroupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		/**
		 * Get the group ID of the application.
		 */
		@Nullable
		public String getGroupId() {
			return this.groupId;
		}

		/**
		 * Set the artifact ID of the application.
		 * @param artifactId the artifact ID of the application, used to locate
		 * {@code native-image.properties}
		 * @return this settings object for method chaining
		 */
		public Settings setArtifactId(String artifactId) {
			this.artifactId = artifactId;
			return this;
		}

		/**
		 * Get the artifact ID of the application.
		 */
		@Nullable
		public String getArtifactId() {
			return this.artifactId;
		}

	}

}
