/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.standard;

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * A default implementation of a TypeLocator that uses the context classloader (or any classloader set upon it). It
 * supports 'well known' packages so if a type cannot be found it will try the registered imports to locate it.
 * 
 * @author Andy Clement
 */
public class StandardTypeLocator implements TypeLocator {

	private ClassLoader loader;
	private final List<String> knownPackagePrefixes = new ArrayList<String>();

	public StandardTypeLocator() {
		loader = Thread.currentThread().getContextClassLoader();
		registerImport("java.lang");
		registerImport("java.util");
	}

	/**
	 * Find a (possibly unqualified) type reference - first using the typename as is, then trying any registered
	 * prefixes if the typename cannot be found.
	 * 
	 * @param typename the type to locate
	 * @return the class object for the type
	 * @throws EvaluationException if the type cannot be found
	 */
	public Class<?> findType(String typename) throws EvaluationException {
		String nameToLookup = typename;
		try {
			Class<?> c = loader.loadClass(nameToLookup);
			return c;
		} catch (ClassNotFoundException e) {
			// try any registered prefixes before giving up
		}
		for (String prefix : knownPackagePrefixes) {
			try {
				nameToLookup = new StringBuilder().append(prefix).append(".").append(typename).toString();
				Class<?> clazz = loader.loadClass(nameToLookup);
				return clazz;
			} catch (ClassNotFoundException e) {
				// might be a different prefix
			}
		}
		throw new SpelException(SpelMessages.TYPE_NOT_FOUND, typename);
	}

	/**
	 * Register a new import prefix that will be used when searching for unqualified types. Expected format is something
	 * like "java.lang".
	 * 
	 * @param prefix the prefix to register
	 */
	public void registerImport(String prefix) {
		knownPackagePrefixes.add(prefix);
	}

	/**
	 * Unregister an import prefix.
	 * 
	 * @param prefix the prefix to unregister
	 */
	public void unregisterImport(String prefix) {
		knownPackagePrefixes.add(prefix);
	}

	/**
	 * Return a list of all the import prefixes registered with this StandardTypeLocator.
	 * 
	 * @return list of registered import prefixes
	 */
	public List<String> getImportPrefixes() {
		return knownPackagePrefixes;
	}

	/**
	 * Set the classloader that should be used (otherwise the context class loader will be used).
	 * 
	 * @param loader the classloader to use from now on
	 */
	public void setClassLoader(ClassLoader loader) {
		this.loader = loader;
	}

}
