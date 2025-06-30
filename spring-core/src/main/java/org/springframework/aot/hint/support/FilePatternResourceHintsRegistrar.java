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

package org.springframework.aot.hint.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 * Register the necessary resource hints for loading files from the classpath,
 * based on file name prefixes and file extensions with convenience to support
 * multiple classpath locations.
 *
 * <p>Only registers hints for matching classpath locations, which allows for
 * several locations to be provided without contributing unnecessary hints.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 6.0
 */
public final class FilePatternResourceHintsRegistrar {

	private final List<String> classpathLocations;

	private final List<String> filePrefixes;

	private final List<String> fileExtensions;


	private FilePatternResourceHintsRegistrar(List<String> filePrefixes, List<String> classpathLocations,
			List<String> fileExtensions) {

		this.classpathLocations = validateClassPathLocations(classpathLocations);
		this.filePrefixes = validateFilePrefixes(filePrefixes);
		this.fileExtensions = validateFileExtensions(fileExtensions);
	}


	private void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		if (classLoaderToUse != null) {
			List<String> includes = new ArrayList<>();
			for (String location : this.classpathLocations) {
				if (classLoaderToUse.getResource(location) != null) {
					for (String filePrefix : this.filePrefixes) {
						for (String fileExtension : this.fileExtensions) {
							includes.add(location + filePrefix + "*" + fileExtension);
						}
					}
				}
			}
			if (!includes.isEmpty()) {
				hints.registerPattern(hint -> hint.includes(includes.toArray(String[]::new)));
			}
		}
	}


	/**
	 * Configure the registrar with the specified
	 * {@linkplain Builder#withClassPathLocations(String...) classpath locations}.
	 * @param classpathLocations the classpath locations
	 * @return a {@link Builder} to further configure the registrar
	 * @since 6.0.12
	 * @see #forClassPathLocations(List)
	 */
	public static Builder forClassPathLocations(String... classpathLocations) {
		return forClassPathLocations(Arrays.asList(classpathLocations));
	}

	/**
	 * Configure the registrar with the specified
	 * {@linkplain Builder#withClassPathLocations(List) classpath locations}.
	 * @param classpathLocations the classpath locations
	 * @return a {@link Builder} to further configure the registrar
	 * @since 6.0.12
	 * @see #forClassPathLocations(String...)
	 */
	public static Builder forClassPathLocations(List<String> classpathLocations) {
		return new Builder().withClassPathLocations(classpathLocations);
	}

	private static List<String> validateClassPathLocations(List<String> classpathLocations) {
		Assert.notEmpty(classpathLocations, "At least one classpath location must be specified");
		List<String> parsedLocations = new ArrayList<>();
		for (String location : classpathLocations) {
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

	private static List<String> validateFilePrefixes(List<String> filePrefixes) {
		for (String filePrefix : filePrefixes) {
			if (filePrefix.contains("*")) {
				throw new IllegalArgumentException("File prefix '" + filePrefix + "' cannot contain '*'");
			}
		}
		return filePrefixes;
	}

	private static List<String> validateFileExtensions(List<String> fileExtensions) {
		for (String fileExtension : fileExtensions) {
			if (!fileExtension.startsWith(".")) {
				throw new IllegalArgumentException("Extension '" + fileExtension + "' must start with '.'");
			}
		}
		return fileExtensions;
	}


	/**
	 * Builder for {@link FilePatternResourceHintsRegistrar}.
	 * @since 6.0.12
	 */
	public static final class Builder {

		private final List<String> classpathLocations = new ArrayList<>();

		private final List<String> filePrefixes = new ArrayList<>();

		private final List<String> fileExtensions = new ArrayList<>();

		private Builder() {
			// no-op
		}

		/**
		 * Consider the specified classpath locations.
		 * @deprecated in favor of {@link #withClassPathLocations(String...)}
		 */
		@Deprecated(since = "7.0", forRemoval = true)
		public Builder withClasspathLocations(String... classpathLocations) {
			return withClassPathLocations(Arrays.asList(classpathLocations));
		}

		/**
		 * Consider the specified classpath locations.
		 * @deprecated in favor of {@link #withClassPathLocations(List)}
		 */
		@Deprecated(since = "7.0", forRemoval = true)
		public Builder withClasspathLocations(List<String> classpathLocations) {
			return withClassPathLocations(classpathLocations);
		}

		/**
		 * Consider the specified classpath locations.
		 * <p>A location can either be a special {@value ResourceUtils#CLASSPATH_URL_PREFIX}
		 * pseudo location or a standard location, such as {@code com/example/resources}.
		 * An empty String represents the root of the classpath.
		 * @param classpathLocations the classpath locations to consider
		 * @return this builder
		 * @since 7.0
		 * @see #withClassPathLocations(List)
		 */
		public Builder withClassPathLocations(String... classpathLocations) {
			return withClassPathLocations(Arrays.asList(classpathLocations));
		}

		/**
		 * Consider the specified classpath locations.
		 * <p>A location can either be a special {@value ResourceUtils#CLASSPATH_URL_PREFIX}
		 * pseudo location or a standard location, such as {@code com/example/resources}.
		 * An empty String represents the root of the classpath.
		 * @param classpathLocations the classpath locations to consider
		 * @return this builder
		 * @since 7.0
		 * @see #withClassPathLocations(String...)
		 */
		public Builder withClassPathLocations(List<String> classpathLocations) {
			this.classpathLocations.addAll(validateClassPathLocations(classpathLocations));
			return this;
		}

		/**
		 * Consider the specified file prefixes. Any file whose name starts with one
		 * of the specified prefixes is considered. A prefix cannot contain the {@code *}
		 * character.
		 * @param filePrefixes the file prefixes to consider
		 * @return this builder
		 * @see #withFilePrefixes(List)
		 */
		public Builder withFilePrefixes(String... filePrefixes) {
			return withFilePrefixes(Arrays.asList(filePrefixes));
		}

		/**
		 * Consider the specified file prefixes. Any file whose name starts with one
		 * of the specified prefixes is considered. A prefix cannot contain the {@code *}
		 * character.
		 * @param filePrefixes the file prefixes to consider
		 * @return this builder
		 * @see #withFilePrefixes(String...)
		 */
		public Builder withFilePrefixes(List<String> filePrefixes) {
			this.filePrefixes.addAll(validateFilePrefixes(filePrefixes));
			return this;
		}

		/**
		 * Consider the specified file extensions. A file extension must start with a
		 * {@code .} character.
		 * @param fileExtensions the file extensions to consider
		 * @return this builder
		 * @see #withFileExtensions(List)
		 */
		public Builder withFileExtensions(String... fileExtensions) {
			return withFileExtensions(Arrays.asList(fileExtensions));
		}

		/**
		 * Consider the specified file extensions. A file extension must start with a
		 * {@code .} character.
		 * @param fileExtensions the file extensions to consider
		 * @return this builder
		 * @see #withFileExtensions(String...)
		 */
		public Builder withFileExtensions(List<String> fileExtensions) {
			this.fileExtensions.addAll(validateFileExtensions(fileExtensions));
			return this;
		}

		private FilePatternResourceHintsRegistrar build() {
			return new FilePatternResourceHintsRegistrar(this.filePrefixes,
					this.classpathLocations, this.fileExtensions);
		}

		/**
		 * Register resource hints for the current state of this builder. For each
		 * classpath location that resolves against the {@code ClassLoader}, files
		 * with the configured file prefixes and extensions are registered.
		 * @param hints the hints contributed so far for the deployment unit
		 * @param classLoader the ClassLoader to use, or {@code null} for the default
		 */
		public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
			build().registerHints(hints, classLoader);
		}
	}

}
