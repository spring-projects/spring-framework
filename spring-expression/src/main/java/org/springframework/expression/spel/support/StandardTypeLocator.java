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

package org.springframework.expression.spel.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.ClassUtils;

/**
 * A default implementation of a TypeLocator that uses the context classloader (or any classloader set upon it). It
 * supports 'well known' packages so if a type cannot be found it will try the registered imports to locate it.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StandardTypeLocator implements TypeLocator {

	private ClassLoader loader;

	private final List<String> knownPackagePrefixes = new ArrayList<String>();


	public StandardTypeLocator() {
		this(ClassUtils.getDefaultClassLoader());
	}

	public StandardTypeLocator(ClassLoader loader) {
		this.loader = loader;
		// Similar to when writing Java, it only knows about java.lang by default
		registerImport("java.lang");
	}


	/**
	 * Find a (possibly unqualified) type reference - first using the typename as is, then trying any registered
	 * prefixes if the typename cannot be found.
	 * @param typename the type to locate
	 * @return the class object for the type
	 * @throws EvaluationException if the type cannot be found
	 */
	@Override
	public Class<?> findType(String typename) throws EvaluationException {
		String nameToLookup = typename;
		try {
			return this.loader.loadClass(nameToLookup);
		}
		catch (ClassNotFoundException ey) {
			// try any registered prefixes before giving up
		}
		for (String prefix : this.knownPackagePrefixes) {
			try {
				nameToLookup = new StringBuilder().append(prefix).append(".").append(typename).toString();
				return this.loader.loadClass(nameToLookup);
			}
			catch (ClassNotFoundException ex) {
				// might be a different prefix
			}
		}
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typename);
	}

	/**
	 * Register a new import prefix that will be used when searching for unqualified types.
	 * Expected format is something like "java.lang".
	 * @param prefix the prefix to register
	 */
	public void registerImport(String prefix) {
		this.knownPackagePrefixes.add(prefix);
	}

	/**
	 * Return a list of all the import prefixes registered with this StandardTypeLocator.
	 * @return list of registered import prefixes
	 */
	public List<String> getImportPrefixes() {
		return Collections.unmodifiableList(this.knownPackagePrefixes);
	}

	public void removeImport(String prefix) {
		this.knownPackagePrefixes.remove(prefix);
	}

}
