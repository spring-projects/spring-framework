/*
 * Copyright 2002-2012 the original author or authors.
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
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 * Convenient utility methods for loading of {@code java.util.Properties},
 * performing standard handling of input streams.
 *
 * <p>For more configurable properties loading, including the option of a
 * customized encoding, consider using the PropertiesLoaderSupport class.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see PropertiesLoaderSupport
 */
public abstract class PropertiesLoaderUtils {

	/**
	 * Load properties from the given resource.
	 * @param resource the resource to load from
	 * @return the populated Properties instance
	 * @throws IOException if loading failed
	 */
	public static Properties loadProperties(Resource resource) throws IOException {
		Properties props = new Properties();
		fillProperties(props, resource);
		return props;
	}

	/**
	 * Fill the given properties from the given resource.
	 * @param props the Properties instance to fill
	 * @param resource the resource to load from
	 * @throws IOException if loading failed
	 */
	public static void fillProperties(Properties props, Resource resource) throws IOException {
		InputStream is = resource.getInputStream();
		try {
			props.load(is);
		}
		finally {
			is.close();
		}
	}

	/**
	 * Load all properties from the given class path resource,
	 * using the default class loader.
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
	 * Load all properties from the given class path resource,
	 * using the given class loader.
	 * <p>Merges properties if more than one resource of the same name
	 * found in the class path.
	 * @param resourceName the name of the class path resource
	 * @param classLoader the ClassLoader to use for loading
	 * (or {@code null} to use the default class loader)
	 * @return the populated Properties instance
	 * @throws IOException if loading failed
	 */
	public static Properties loadAllProperties(String resourceName, ClassLoader classLoader) throws IOException {
		Assert.notNull(resourceName, "Resource name must not be null");
		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = ClassUtils.getDefaultClassLoader();
		}
		Properties properties = new Properties();
		Enumeration urls = clToUse.getResources(resourceName);
		while (urls.hasMoreElements()) {
			URL url = (URL) urls.nextElement();
			InputStream is = null;
			try {
				URLConnection con = url.openConnection();
				ResourceUtils.useCachesIfNecessary(con);
				is = con.getInputStream();
				properties.load(is);
			}
			finally {
				if (is != null) {
					is.close();
				}
			}
		}
		return properties;
	}

}
