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

package org.springframework.core;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * General purpose factory loading mechanism.
 *
 * <p>The {@code SpringFactoriesLoader} loads and instantiates factories of a given type
 * from a given file location. If a location is not given, the {@linkplain
 * #DEFAULT_FACTORIES_LOCATION default location} is used.
 *
 * <p>The file should be in {@link Properties} format, where the key is the fully
 * qualified interface or abstract class name, and the value is a comma separated list of
 * implementations. For instance:
 * <pre class="code">
 * example.MyService=example.MyServiceImpl1,example.MyServiceImpl2
 * </pre>
 * where {@code MyService} is the name of the interface, and {@code MyServiceImpl1} and
 * {@code MyServiceImpl2} are the two implementations.
 *
 * @author Arjen Poutsma
 * @since 3.2
 */
public abstract class SpringFactoriesLoader {

	private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

	/** The location to look for the factories. Can be present in multiple JAR files. */
	public static final String DEFAULT_FACTORIES_LOCATION = "META-INF/spring.factories";

	/**
	 * Loads the factory implementations of the given type from the default location, using
	 * the given class loader.
	 *
	 * <p>The returned factories are ordered in accordance with the {@link OrderComparator}.
	 *
	 * @param factoryClass the interface or abstract class representing the factory
	 * @param classLoader  the ClassLoader to use for loading, can be {@code null} to use the
	 *                     default
	 * @throws IllegalArgumentException in case of errors
	 */
	public static <T> List<T> loadFactories(Class<T> factoryClass,
	                                        ClassLoader classLoader) {
		return loadFactories(factoryClass, classLoader, null);
	}

	/**
	 * Loads the factory implementations of the given type from the given location, using the
	 * given class loader.
	 *
	 * <p>The returned factories are ordered in accordance with the {@link OrderComparator}.
	 *
	 * @param factoryClass      the interface or abstract class representing the factory
	 * @param classLoader       the ClassLoader to use for loading, can be {@code null} to
	 *                          use the default
	 * @param factoriesLocation the factories file location, can be {@code null} to use the
	 *                          {@linkplain #DEFAULT_FACTORIES_LOCATION default}
	 * @throws IllegalArgumentException in case of errors
	 */
	public static <T> List<T> loadFactories(Class<T> factoryClass,
	                                        ClassLoader classLoader,
	                                        String factoriesLocation) {
		Assert.notNull(factoryClass, "'factoryClass' must not be null");

		if (classLoader == null) {
			classLoader = ClassUtils.getDefaultClassLoader();
		}
		if (factoriesLocation == null) {
			factoriesLocation = DEFAULT_FACTORIES_LOCATION;
		}

		List<String> factoryNames =
				loadFactoryNames(factoryClass, classLoader, factoriesLocation);

		if (logger.isTraceEnabled()) {
			logger.trace(
					"Loaded [" + factoryClass.getName() + "] names: " + factoryNames);
		}

		List<T> result = new ArrayList<T>(factoryNames.size());
		for (String factoryName : factoryNames) {
			result.add(instantiateFactory(factoryName, factoryClass, classLoader));
		}

		Collections.sort(result, new OrderComparator());

		return result;
	}

	private static List<String> loadFactoryNames(Class<?> factoryClass,
	                                             ClassLoader classLoader,
	                                             String factoriesLocation) {

		String factoryClassName = factoryClass.getName();

		try {
			List<String> result = new ArrayList<String>();
			Enumeration<URL> urls = classLoader.getResources(factoriesLocation);
			while (urls.hasMoreElements()) {
				URL url = (URL) urls.nextElement();
				Properties properties =
						PropertiesLoaderUtils.loadProperties(new UrlResource(url));
				String factoryClassNames = properties.getProperty(factoryClassName);
				result.addAll(Arrays.asList(
						StringUtils.commaDelimitedListToStringArray(factoryClassNames)));
			}
			return result;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Unable to load [" + factoryClass.getName() +
							"] factories from location [" +
							factoriesLocation + "]", ex);
		}


	}

	@SuppressWarnings("unchecked")
	private static <T> T instantiateFactory(String instanceClassName,
	                                        Class<T> factoryClass,
	                                        ClassLoader classLoader) {
		try {
			Class<?> instanceClass = ClassUtils.forName(instanceClassName, classLoader);
			if (!factoryClass.isAssignableFrom(instanceClass)) {
				throw new IllegalArgumentException(
						"Class [" + instanceClassName + "] is not assignable to [" +
								factoryClass.getName() + "]");
			}
			return (T) instanceClass.newInstance();
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(
					factoryClass.getName() + " class [" + instanceClassName +
							"] not found", ex);
		}
		catch (LinkageError err) {
			throw new IllegalArgumentException(
					"Invalid " + factoryClass.getName() + " class [" + instanceClassName +
							"]: problem with handler class file or dependent class", err);
		}
		catch (InstantiationException ex) {
			throw new IllegalArgumentException(
					"Could not instantiate bean class [" + instanceClassName +
							"]: Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalArgumentException(
					"Could not instantiate bean class [" + instanceClassName +
							"Is the constructor accessible?", ex);
		}
	}

}