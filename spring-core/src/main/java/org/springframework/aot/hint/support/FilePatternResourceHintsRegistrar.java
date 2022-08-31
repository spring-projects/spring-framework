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

package org.springframework.aot.hint.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aot.hint.ResourceHints;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * Register the necessary resource hints for loading files from the classpath.
 *
 * <p>Candidates are identified by a file name, a location, and an extension.
 * The location can be the empty string to refer to the root of the classpath.
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
	 * @param extensions the file extensions (starts with a dot)
	 */
	public FilePatternResourceHintsRegistrar(List<String> names, List<String> locations,
			List<String> extensions) {
		this.names = validateNames(names);
		this.locations = validateLocations(locations);
		this.extensions = validateExtensions(extensions);
	}

	private static List<String> validateNames(List<String> names) {
		for (String name : names) {
			if (name.contains("*")) {
				throw new IllegalArgumentException("File name '" + name + "' cannot contain '*'");
			}
		}
		return names;
	}

	private static List<String> validateLocations(List<String> locations) {
		Assert.notEmpty(locations, "At least one location should be specified");
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

	private static List<String> validateExtensions(List<String> extensions) {
		for (String extension : extensions) {
			if (!extension.startsWith(".")) {
				throw new IllegalArgumentException("Extension '" + extension + "' should start with '.'");
			}
		}
		return extensions;
	}

	public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = (classLoader != null) ? classLoader : getClass().getClassLoader();
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
}
