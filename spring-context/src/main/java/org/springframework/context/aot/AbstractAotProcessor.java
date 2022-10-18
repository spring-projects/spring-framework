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
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

/**
 * Abstract base class for filesystem-based ahead-of-time (AOT) processing.
 *
 * <p>Concrete implementations should override {@link #doProcess()} that kicks
 * off the optimization of the target, usually an application.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.0
 * @param <T> the type of the processing result
 * @see FileSystemGeneratedFiles
 * @see FileNativeConfigurationWriter
 * @see org.springframework.context.aot.ContextAotProcessor
 * @see org.springframework.test.context.aot.TestAotProcessor
 */
public abstract class AbstractAotProcessor<T> {

	/**
	 * The name of a system property that is made available when the processor
	 * runs.
	 * @see #doProcess()
	 */
	private static final String AOT_PROCESSING = "spring.aot.processing";

	private final Settings settings;


	/**
	 * Create a new processor instance with the supplied {@linkplain Settings settings}.
	 * @see Settings#builder()
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
	 * Run AOT processing.
	 * @return the result of the processing.
	 */
	public final T process() {
		try {
			System.setProperty(AOT_PROCESSING, "true");
			return doProcess();
		}
		finally {
			System.clearProperty(AOT_PROCESSING);
		}
	}

	protected abstract T doProcess();

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
	public static final class Settings {

		private final Path sourceOutput;

		private final Path resourceOutput;

		private final Path classOutput;

		private final String groupId;

		private final String artifactId;


		private Settings(Path sourceOutput, Path resourceOutput, Path classOutput, String groupId, String artifactId) {
			this.sourceOutput = sourceOutput;
			this.resourceOutput = resourceOutput;
			this.classOutput = classOutput;
			this.groupId = groupId;
			this.artifactId = artifactId;
		}


		/**
		 * Create a new {@link Builder} for {@link Settings}.
		 */
		public static Builder builder() {
			return new Builder();
		}


		/**
		 * Get the output directory for generated sources.
		 */
		public Path getSourceOutput() {
			return this.sourceOutput;
		}

		/**
		 * Get the output directory for generated resources.
		 */
		public Path getResourceOutput() {
			return this.resourceOutput;
		}

		/**
		 * Get the output directory for generated classes.
		 */
		public Path getClassOutput() {
			return this.classOutput;
		}

		/**
		 * Get the group ID of the application.
		 */
		public String getGroupId() {
			return this.groupId;
		}

		/**
		 * Get the artifact ID of the application.
		 */
		public String getArtifactId() {
			return this.artifactId;
		}


		/**
		 * Fluent builder API for {@link Settings}.
		 */
		public static final class Builder {

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


			private Builder() {
				// internal constructor
			}


			/**
			 * Set the output directory for generated sources.
			 * @param sourceOutput the location of generated sources
			 * @return this builder for method chaining
			 */
			public Builder sourceOutput(Path sourceOutput) {
				this.sourceOutput = sourceOutput;
				return this;
			}

			/**
			 * Set the output directory for generated resources.
			 * @param resourceOutput the location of generated resources
			 * @return this builder for method chaining
			 */
			public Builder resourceOutput(Path resourceOutput) {
				this.resourceOutput = resourceOutput;
				return this;
			}

			/**
			 * Set the output directory for generated classes.
			 * @param classOutput the location of generated classes
			 * @return this builder for method chaining
			 */
			public Builder classOutput(Path classOutput) {
				this.classOutput = classOutput;
				return this;
			}

			/**
			 * Set the group ID of the application.
			 * @param groupId the group ID of the application, used to locate
			 * {@code native-image.properties}
			 * @return this builder for method chaining
			 */
			public Builder groupId(String groupId) {
				this.groupId = groupId;
				return this;
			}

			/**
			 * Set the artifact ID of the application.
			 * @param artifactId the artifact ID of the application, used to locate
			 * {@code native-image.properties}
			 * @return this builder for method chaining
			 */
			public Builder artifactId(String artifactId) {
				this.artifactId = artifactId;
				return this;
			}

			/**
			 * Build the {@link Settings} configured in this {@code Builder}.
			 */
			public Settings build() {
				Assert.notNull(this.sourceOutput, "'sourceOutput' must not be null");
				Assert.notNull(this.resourceOutput, "'resourceOutput' must not be null");
				Assert.notNull(this.classOutput, "'classOutput' must not be null");
				Assert.hasText(this.groupId, "'groupId' must not be null or empty");
				Assert.hasText(this.artifactId, "'artifactId' must not be null or empty");
				return new Settings(this.sourceOutput, this.resourceOutput, this.classOutput,
						this.groupId, this.artifactId);
			}

		}

	}

}
