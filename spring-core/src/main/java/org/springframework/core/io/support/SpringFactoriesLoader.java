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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.OrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * General purpose factory loading mechanism for internal use within the framework.
 *
 * <p>The {@code SpringFactoriesLoader} loads and instantiates factories of a given type
 * from "META-INF/spring.factories" files. The file should be in {@link Properties} format,
 * where the key is the fully qualified interface or abstract class name, and the value
 * is a comma-separated list of implementation class names. For instance:
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 *
 * where {@code MyService} is the name of the interface, and {@code MyServiceImpl1} and
 * {@code MyServiceImpl2} are the two implementations.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class SpringFactoriesLoader {

	/** The location to look for the factories. Can be present in multiple JAR files. */
	private static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

	private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);


	/**
	 * Load the factory implementations of the given type from the default location,
	 * using the given class loader.
	 * <p>The returned factories are ordered in accordance with the {@link OrderComparator}.
	 * @param factoryClass the interface or abstract class representing the factory
	 * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
	 */
	public static <T> List<T> loadFactories(Class<T> factoryClass, ClassLoader classLoader) {
		Assert.notNull(factoryClass, "'factoryClass' must not be null");
		if (classLoader == null) {
			classLoader = SpringFactoriesLoader.class.getClassLoader();
		}
		List<String> factoryNames = loadFactoryNames(factoryClass, classLoader);
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded [" + factoryClass.getName() + "] names: " + factoryNames);
		}
		List<T> result = new ArrayList<T>(factoryNames.size());
		for (String factoryName : factoryNames) {
			result.add(instantiateFactory(factoryName, factoryClass, classLoader));
		}
		OrderComparator.sort(result);
		return result;
	}

	public static List<String> loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader) {
		String factoryClassName = factoryClass.getName();
		try {
			List<String> result = new ArrayList<String>();
			Enumeration<URL> urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));
				String factoryClassNames = properties.getProperty(factoryClassName);
				result.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(factoryClassNames)));
			}
			return result;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load [" + factoryClass.getName() +
					"] factories from location [" + FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T instantiateFactory(String instanceClassName, Class<T> factoryClass, ClassLoader classLoader) {
		try {
			Class<?> instanceClass = ClassUtils.forName(instanceClassName, classLoader);
			if (!factoryClass.isAssignableFrom(instanceClass)) {
				throw new IllegalArgumentException(
						"Class [" + instanceClassName + "] is not assignable to [" + factoryClass.getName() + "]");
			}
			return (T) instanceClass.newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot instantiate factory class: " + factoryClass.getName(), ex);
		}
	}

}
