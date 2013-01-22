/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Subclass of {@link PropertiesPropertySource} that loads a {@link Properties}
 * object from a given {@link org.springframework.core.io.Resource} or resource location such as
 * {@code "classpath:/com/myco/foo.properties"} or {@code "file:/path/to/file.xml"}.
 * Both traditional and XML-based properties file formats are supported, however in order
 * for XML processing to take effect, the underlying {@code Resource}'s
 * {@link org.springframework.core.io.Resource#getFilename() getFilename()} method must
 * return non-{@code null} and end in ".xml".
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ResourcePropertySource extends PropertiesPropertySource {

	/**
	 * Create a PropertySource having the given name based on Properties
	 * loaded from the given resource.
	 */
	public ResourcePropertySource(String name, Resource resource) throws IOException {
		super(name, loadPropertiesForResource(resource));
	}

	/**
	 * Create a PropertySource based on Properties loaded from the given resource.
	 * The name of the PropertySource will be generated based on the
	 * {@link Resource#getDescription() description} of the given resource.
	 */
	public ResourcePropertySource(Resource resource) throws IOException {
		this(getNameForResource(resource), resource);
	}

	/**
	 * Create a PropertySource having the given name based on Properties loaded from
	 * the given resource location and using the given class loader to load the
	 * resource (assuming it is prefixed with {@code classpath:}).
	 */
	public ResourcePropertySource(String name, String location, ClassLoader classLoader) throws IOException {
		this(name, getResourceForLocation(location, classLoader));
	}

	/**
	 * Create a PropertySource having the given name based on Properties loaded from
	 * the given resource location. The default thread context class loader will be
	 * used to load the resource (assuming the location string is prefixed with
	 * {@code classpath:}.
	 */
	public ResourcePropertySource(String name, String location) throws IOException {
		this(name, location, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a PropertySource based on Properties loaded from the given resource
	 * location and use the given class loader to load the resource, assuming it is
	 * prefixed with {@code classpath:}. The name of the PropertySource will be
	 * generated based on the {@link Resource#getDescription() description} of the
	 * resource.
	 */
	public ResourcePropertySource(String location, ClassLoader classLoader) throws IOException {
		this(getResourceForLocation(location, classLoader));
	}

	/**
	 * Create a PropertySource based on Properties loaded from the given resource
	 * location. The name of the PropertySource will be generated based on the
	 * {@link Resource#getDescription() description} of the resource.
	 */
	public ResourcePropertySource(String location) throws IOException {
		this(getResourceForLocation(location, ClassUtils.getDefaultClassLoader()));
	}


	private static Resource getResourceForLocation(String location, ClassLoader classLoader) {
		return new PathMatchingResourcePatternResolver(classLoader).getResource(location);
	}

	private static Properties loadPropertiesForResource(Resource resource) throws IOException {
		Properties props = new Properties();
		InputStream is = resource.getInputStream();
		String filename = resource.getFilename();
		if (filename != null && filename.endsWith(".xml")) {
			props.loadFromXML(is);
		}
		else {
			props.load(is);
		}
		try {
			is.close();
		} catch (IOException ex) {
			// ignore
		}
		return props;
	}

	/**
	 * Returns the description string for the resource, and if empty returns
	 * the class name of the resource plus its identity hash code.
	 */
	private static String getNameForResource(Resource resource) {
		String name = resource.getDescription();
		if (!StringUtils.hasText(name)) {
			name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
		}
		return name;
	}

}
