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

// TODO 3 promote import prefix support and classloader setting to the interface?
public class StandardTypeLocator implements TypeLocator {

	private ClassLoader loader;
	private final List<String> knownPackagePrefixes = new ArrayList<String>();

	public StandardTypeLocator() {
		loader = Thread.currentThread().getContextClassLoader();
		registerImport("java.lang");
		registerImport("java.util");
	}

	// OPTIMIZE I'm sure this *could* be more inefficient if I tried really hard...
	public Class<?> findType(String type) throws EvaluationException {
		String nameToLookup = type;
		try {
			Class<?> c = loader.loadClass(nameToLookup);
			return c;
		} catch (ClassNotFoundException e) {
			// might need a prefix...
		}
		// try prefixes
		for (String prefix : knownPackagePrefixes) {
			try {
				nameToLookup = new StringBuilder().append(prefix).append(".").append(type).toString();
				Class<?> c = loader.loadClass(nameToLookup);
				return c;
			} catch (ClassNotFoundException e) {
			}
		}
		// TODO should some of these common messages be promoted to top level exception types?
		throw new SpelException(SpelMessages.TYPE_NOT_FOUND, type);
	}

	/**
	 * Register a new import prefix that will be used when searching for unqualified types. Expected format is something
	 * like "java.lang"
	 */
	public void registerImport(String prefix) {
		knownPackagePrefixes.add(prefix);
	}

	public void unregisterImport(String prefix) {
		knownPackagePrefixes.add(prefix);
	}

	public List<String> getImports() {
		return knownPackagePrefixes;
	}

	public void setClassLoader(ClassLoader loader) {
		this.loader = loader;
	}

}
