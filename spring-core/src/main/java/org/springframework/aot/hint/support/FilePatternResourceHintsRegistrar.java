/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.hint.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * Register the necessary resource hints for loading files from the classpath,
 * based on a file name prefix and an extension with convenience to support
 * multiple classpath locations.
 *
 * <p>Only registers hints for matching classpath locations, which allows for
 * several locations to be provided without contributing unnecessary hints.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class FilePatternResourceHintsRegistrar {

	private final List<String> names;

	private final List<String> locations;

	private final List<String> extensions;

	/**
	 * Create a new instance for the specified file names, locations, and file
	 * extensions.
	 * @param names the file names
	 * @param locations the classpath locations
	 * @param extensions the file extensions (starting with a dot)
	 * @deprecated as of 6.0.12 in favor of {@linkplain #forClassPathLocations(String...) the builder}
	 */
	@Deprecated(since = "6.0.12", forRemoval = true)
	public FilePatternResourceHintsRegistrar(List<String> names, List<String> locations,
			List<String> extensions) {
		this.names = Builder.validateFilePrefixes(names.toArray(String[]::new));
		this.locations = Builder.validateClasspathLocations(locations.toArray(String[]::new));
		this.extensions = Builder.validateFileExtensions(extensions.toArray(String[]::new));
	}

	/**
	 * Configure the registrar with the specified
	 * {@linkplain Builder#withClasspathLocations(String...) classpath locations}.
	 * @param locations the classpath locations
	 * @return a {@link Builder} to further configure the registrar
	 */
	public static Builder forClassPathLocations(String... locations) {
		Assert.notEmpty(locations, "At least one classpath location should be specified");
		return new Builder().withClasspathLocations(locations);
	}

	@Deprecated(since = "6.0.12", forRemoval = true)
	public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = (classLoader != null ? classLoader : getClass().getClassLoader());
		List<String> includes = new ArrayList<>();
		for (String location : this.locations) {
			if (classLoaderToUse.getResource(location) != null) {
				for (String extension : this.extensions) {
					for (String name : this.names) {
						includes.add(location + name + "*" + extension);
					}
				}
			}
		}
		if (!includes.isEmpty()) {
			hints.registerPattern(hint -> hint.includes(includes.toArray(String[]::new)));
		}
	}

	/**
	 * Builder for {@link FilePatternResourceHintsRegistrar}.
	 */
	public static final class Builder {

		private final List<String> classpathLocations = new ArrayList<>();

		private final List<String> filePrefixes = new ArrayList<>();

		private final List<String> fileExtensions = new ArrayList<>();


		/**
		 * Consider the specified classpath locations. A location can either be
		 * a special "classpath" pseudo location or a standard location, such as
		 * {@code com/example/resources}. An empty String represents the root of
		 * the classpath.
		 * @param classpathLocations the classpath locations to consider
		 * @return this builder
		 */
		public Builder withClasspathLocations(String... classpathLocations) {
			this.classpathLocations.addAll(validateClasspathLocations(classpathLocations));
			return this;
		}

		/**
		 * Consider the specified file prefixes. Any file whose name starts with one
		 * of the specified prefixes is considered. A prefix cannot contain the {@code *}
		 * character.
		 * @param filePrefixes the file prefixes to consider
		 * @return this builder
		 */
		public Builder withFilePrefixes(String... filePrefixes) {
			this.filePrefixes.addAll(validateFilePrefixes(filePrefixes));
			return this;
		}

		/**
		 * Consider the specified file extensions. A file extension must start with a
		 * {@code .} character.
		 * @param fileExtensions the file extensions to consider
		 * @return this builder
		 */
		public Builder withFileExtensions(String... fileExtensions) {
			this.fileExtensions.addAll(validateFileExtensions(fileExtensions));
			return this;
		}

		FilePatternResourceHintsRegistrar build() {
			Assert.notEmpty(this.classpathLocations, "At least one location should be specified");
			return new FilePatternResourceHintsRegistrar(this.filePrefixes,
					this.classpathLocations, this.fileExtensions);
		}

		/**
		 * Register resource hints for the current state of this builder. For each
		 * classpath location that resolves against the {@code ClassLoader}, files
		 * with the configured file prefixes and extensions are registered.
		 * @param hints the hints contributed so far for the deployment unit
		 * @param classLoader the classloader, or {@code null} if even the system ClassLoader isn't accessible
		 */
		public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
			build().registerHints(hints, classLoader);
		}

		private static List<String> validateClasspathLocations(String... locations) {
			List<String> parsedLocations = new ArrayList<>();
			for (String location : locations) {
				if (location.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
					location = location.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
				}
				if (location.startsWith("/")) {
					location = location.substring(1);
				}
				if (!location.isEmpty() && !location.endsWith("/")) {
					location = location + "/";
				}
				parsedLocations.add(location);
			}
			return parsedLocations;
		}

		private static List<String> validateFilePrefixes(String... filePrefixes) {
			for (String filePrefix : filePrefixes) {
				if (filePrefix.contains("*")) {
					throw new IllegalArgumentException("File prefix '" + filePrefix + "' cannot contain '*'");
				}
			}
			return Arrays.asList(filePrefixes);
		}

		private static List<String> validateFileExtensions(String... fileExtensions) {
			for (String fileExtension : fileExtensions) {
				if (!fileExtension.startsWith(".")) {
					throw new IllegalArgumentException("Extension '" + fileExtension + "' should start with '.'");
				}
			}
			return Arrays.asList(fileExtensions);
		}

	}

}
