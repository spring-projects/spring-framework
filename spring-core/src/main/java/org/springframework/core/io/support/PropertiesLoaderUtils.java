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

package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Convenient utility methods for loading of {@code java.util.Properties},
 * performing standard handling of input streams.
 *
 * <p>For more configurable properties loading, including the option of a
 * customized encoding, consider using the PropertiesLoaderSupport class.
 *
 * <p>As of 7.1, YAML resources (identified by a {@code .yml} or {@code .yaml}
 * filename extension) are supported as well, provided that SnakeYAML is
 * present on the classpath. SnakeYAML is an optional dependency of this
 * module: if it is not present, attempting to load a YAML resource results
 * in an {@link IllegalStateException}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sebastien Deleuze
 * @since 2.0
 * @see PropertiesLoaderSupport
 */
public abstract class PropertiesLoaderUtils {

	private static final String XML_FILE_EXTENSION = ".xml";

	private static final String YAML_FILE_EXTENSION = ".yaml";

	private static final String YML_FILE_EXTENSION = ".yml";

	private static final boolean snakeYamlPresent = ClassUtils.isPresent(
			"org.yaml.snakeyaml.Yaml", PropertiesLoaderUtils.class.getClassLoader());


	/**
	 * Load properties from the given EncodedResource,
	 * potentially defining a specific encoding for the properties file.
	 * @see #fillProperties(java.util.Properties, EncodedResource)
	 */
	public static Properties loadProperties(EncodedResource resource) throws IOException {
		Properties props = new Properties();
		fillProperties(props, resource);
		return props;
	}

	/**
	 * Fill the given properties from the given EncodedResource,
	 * potentially defining a specific encoding for the properties file.
	 * @param props the Properties instance to load into
	 * @param resource the resource to load from
	 * @throws IOException in case of I/O errors
	 */
	public static void fillProperties(Properties props, EncodedResource resource)
			throws IOException {

		fillProperties(props, resource, DefaultPropertiesPersister.INSTANCE);
	}

	/**
	 * Actually load properties from the given EncodedResource into the given Properties instance.
	 * @param props the Properties instance to load into
	 * @param resource the resource to load from
	 * @param persister the PropertiesPersister to use
	 * @throws IOException in case of I/O errors
	 */
	static void fillProperties(Properties props, EncodedResource resource, PropertiesPersister persister)
			throws IOException {

		InputStream stream = null;
		Reader reader = null;
		try {
			String filename = resource.getResource().getFilename();
			if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
				stream = resource.getInputStream();
				persister.loadFromXml(props, stream);
			}
			else if (isYamlFile(filename)) {
				stream = resource.getInputStream();
				fillYamlProperties(props, stream, resource);
			}
			else if (resource.requiresReader()) {
				reader = resource.getReader();
				persister.load(props, reader);
			}
			else {
				stream = resource.getInputStream();
				persister.load(props, stream);
			}
		}
		finally {
			if (stream != null) {
				stream.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * Load properties from the given resource (in ISO-8859-1 encoding).
	 * @param resource the resource to load from
	 * @return the populated Properties instance
	 * @throws IOException if loading failed
	 * @see #fillProperties(java.util.Properties, Resource)
	 */
	public static Properties loadProperties(Resource resource) throws IOException {
		Properties props = new Properties();
		fillProperties(props, resource);
		return props;
	}

	/**
	 * Fill the given properties from the given resource (in ISO-8859-1 encoding).
	 * @param props the Properties instance to fill
	 * @param resource the resource to load from
	 * @throws IOException if loading failed
	 */
	public static void fillProperties(Properties props, Resource resource) throws IOException {
		try (InputStream is = resource.getInputStream()) {
			String filename = resource.getFilename();
			if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
				props.loadFromXML(is);
			}
			else if (isYamlFile(filename)) {
				fillYamlProperties(props, is, resource);
			}
			else {
				props.load(is);
			}
		}
	}

	/**
	 * Load all properties from the specified class path resource
	 * (in ISO-8859-1 encoding), using the default class loader.
	 * <p>Merges properties if more than one resource of the same name
	 * found in the class path.
	 * @param resourceName the name of the class path resource
	 * @return the populated Properties instance
	 * @throws IOException if loading failed
	 */
	public static Properties loadAllProperties(String resourceName) throws IOException {
		return loadAllProperties(resourceName, null);
	}

	/**
	 * Load all properties from the specified class path resource
	 * (in ISO-8859-1 encoding), using the given class loader.
	 * <p>Merges properties if more than one resource of the same name
	 * found in the class path.
	 * @param resourceName the name of the class path resource
	 * @param classLoader the ClassLoader to use for loading
	 * (or {@code null} to use the default class loader)
	 * @return the populated Properties instance
	 * @throws IOException if loading failed
	 */
	public static Properties loadAllProperties(String resourceName, @Nullable ClassLoader classLoader) throws IOException {
		Assert.notNull(resourceName, "Resource name must not be null");
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = ClassUtils.getDefaultClassLoader();
		}
		Enumeration<URL> urls = (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) :
				ClassLoader.getSystemResources(resourceName));
		Properties props = new Properties();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			URLConnection con = url.openConnection();
			ResourceUtils.useCachesIfNecessary(con);
			try (InputStream is = con.getInputStream()) {
				if (resourceName.endsWith(XML_FILE_EXTENSION)) {
					props.loadFromXML(is);
				}
				else if (isYamlFile(resourceName)) {
					fillYamlProperties(props, is, url);
				}
				else {
					props.load(is);
				}
			}
		}
		return props;
	}

	private static boolean isYamlFile(@Nullable String filename) {
		return (filename != null &&
				(filename.endsWith(YAML_FILE_EXTENSION) || filename.endsWith(YML_FILE_EXTENSION)));
	}

	private static void fillYamlProperties(Properties props, InputStream is, Object source) throws IOException {
		if (!snakeYamlPresent) {
			throw new IllegalStateException(
					"Could not detect SnakeYAML library on the classpath - it is required to parse YAML resource: " + source);
		}
		SnakeYamlPropertiesLoader.fillProperties(props, is);
	}


	/**
	 * Inner class to avoid a hard dependency on SnakeYAML at runtime.
	 */
	private static class SnakeYamlPropertiesLoader {

		static void fillProperties(Properties props, InputStream is) throws IOException {
			Yaml yaml = createYaml();
			Map<String, Object> flattened = new LinkedHashMap<>();
			try (Reader reader = new UnicodeReader(is)) {
				for (Object document : yaml.loadAll(reader)) {
					if (document != null) {
						buildFlattenedMap(flattened, asMap(document), null);
					}
				}
			}
			flattened.forEach((key, value) -> props.setProperty(key, String.valueOf(value)));
		}

		private static Yaml createYaml() {
			LoaderOptions loaderOptions = new LoaderOptions();
			loaderOptions.setAllowDuplicateKeys(false);
			loaderOptions.setTagInspector(tag -> false);
			DumperOptions dumperOptions = new DumperOptions();
			return new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private static Map<String, Object> asMap(Object object) {
			// YAML can have numbers as keys
			Map<String, Object> result = new LinkedHashMap<>();
			if (!(object instanceof Map map)) {
				// A document can be a text literal
				result.put("document", object);
				return result;
			}
			map.forEach((key, value) -> {
				if (value instanceof Map) {
					value = asMap(value);
				}
				if (key instanceof CharSequence) {
					result.put(key.toString(), value);
				}
				else {
					// It has to be a map key in this case
					result.put("[" + key + "]", value);
				}
			});
			return result;
		}

		@SuppressWarnings("unchecked")
		private static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
			source.forEach((key, value) -> {
				if (StringUtils.hasText(path)) {
					key = (key.startsWith("[") ? path + key : path + '.' + key);
				}
				if (value instanceof String) {
					result.put(key, value);
				}
				else if (value instanceof Map<?, ?> map) {
					// Need a compound key
					buildFlattenedMap(result, (Map<String, Object>) map, key);
				}
				else if (value instanceof Collection<?> collection) {
					// Need a compound key
					if (collection.isEmpty()) {
						result.put(key, "");
					}
					else {
						int count = 0;
						for (Object object : collection) {
							buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
						}
					}
				}
				else {
					result.put(key, (value != null ? value : ""));
				}
			});
		}
	}

}
