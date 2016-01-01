/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with resources within the <em>Spring TestContext
 * Framework</em>. Mainly for internal use within the framework.
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 4.1
 * @see org.springframework.util.ResourceUtils
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ClassPathResource
 * @see org.springframework.core.io.FileSystemResource
 * @see org.springframework.core.io.UrlResource
 * @see org.springframework.core.io.ResourceLoader
 */
public abstract class TestContextResourceUtils {

	private static final String SLASH = "/";


	private TestContextResourceUtils() {
		/* prevent instantiation */
	}

	/**
	 * Convert the supplied paths to classpath resource paths.
	 *
	 * <p>For each of the supplied paths:
	 * <ul>
	 * <li>A plain path &mdash; for example, {@code "context.xml"} &mdash; will
	 * be treated as a classpath resource that is relative to the package in
	 * which the specified class is defined.
	 * <li>A path starting with a slash will be treated as an absolute path
	 * within the classpath, for example: {@code "/org/example/schema.sql"}.
	 * <li>A path which is prefixed with a URL protocol (e.g.,
	 * {@link ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link ResourceUtils#FILE_URL_PREFIX file:}, {@code http:}, etc.) will be
	 * {@link StringUtils#cleanPath cleaned} but otherwise unmodified.
	 *
	 * @param clazz the class with which the paths are associated
	 * @param paths the paths to be converted
	 * @return a new array of converted resource paths
	 * @see #convertToResources
	 */
	public static String[] convertToClasspathResourcePaths(Class<?> clazz, String... paths) {
		String[] convertedPaths = new String[paths.length];
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (path.startsWith(SLASH)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + path;
			}
			else if (!ResourcePatternUtils.isUrl(path)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + SLASH
						+ StringUtils.cleanPath(ClassUtils.classPackageAsResourcePath(clazz) + SLASH + path);
			}
			else {
				convertedPaths[i] = StringUtils.cleanPath(path);
			}
		}
		return convertedPaths;
	}

	/**
	 * Convert the supplied paths to an array of {@link Resource} handles using
	 * the given {@link ResourceLoader}.
	 *
	 * @param resourceLoader the {@code ResourceLoader} to use to convert the paths
	 * @param paths the paths to be converted
	 * @return a new array of resources
	 * @see #convertToResourceList(ResourceLoader, String...)
	 * @see #convertToClasspathResourcePaths
	 */
	public static Resource[] convertToResources(ResourceLoader resourceLoader, String... paths) {
		List<Resource> list = convertToResourceList(resourceLoader, paths);
		return list.toArray(new Resource[list.size()]);
	}

	/**
	 * Convert the supplied paths to a list of {@link Resource} handles using
	 * the given {@link ResourceLoader}.
	 *
	 * @param resourceLoader the {@code ResourceLoader} to use to convert the paths
	 * @param paths the paths to be converted
	 * @return a new list of resources
	 * @since 4.2
	 * @see #convertToResources(ResourceLoader, String...)
	 * @see #convertToClasspathResourcePaths
	 */
	public static List<Resource> convertToResourceList(ResourceLoader resourceLoader, String... paths) {
		List<Resource> list = new ArrayList<Resource>();
		for (String path : paths) {
			list.add(resourceLoader.getResource(path));
		}
		return list;
	}

}
