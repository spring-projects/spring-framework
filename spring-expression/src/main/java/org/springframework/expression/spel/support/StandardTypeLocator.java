/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.SmartClassLoader;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * A simple implementation of {@link TypeLocator} that uses the default
 * {@link #StandardTypeLocator() ClassLoader} or a supplied
 * {@link #StandardTypeLocator(ClassLoader) ClassLoader} to locate types.
 *
 * <p>Supports <em>well-known</em> packages, registered as
 * {@linkplain #registerImport(String) import prefixes}. If a type cannot be found,
 * this class will attempt to locate it using the registered import prefixes.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class StandardTypeLocator implements TypeLocator {

	@Nullable
	private final ClassLoader classLoader;

	private final List<String> importPrefixes = new ArrayList<>(1);

	private final Map<String, Class<?>> typeCache = new ConcurrentHashMap<>();


	/**
	 * Create a {@code StandardTypeLocator} for the default {@link ClassLoader}
	 * (typically, the thread context {@code ClassLoader}).
	 * <p>Favor {@link #StandardTypeLocator(ClassLoader)} over this constructor
	 * in order to provide a specific {@link ClassLoader} that is able to reliably
	 * locate user types.
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	public StandardTypeLocator() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Create a {@code StandardTypeLocator} for the given {@link ClassLoader}.
	 * <p>Favor this constructor over {@link #StandardTypeLocator()} in order
	 * to provide a specific {@link ClassLoader} that is able to reliably locate
	 * user types.
	 * @param classLoader the {@code ClassLoader} to delegate to
	 */
	public StandardTypeLocator(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
		// Similar to when writing regular Java code, it only knows about java.lang by default
		registerImport("java.lang");
	}


	/**
	 * Register a new import prefix that will be used when searching for unqualified types.
	 * <p>Expected format is something like {@code "java.lang"}.
	 * @param prefix the import prefix to register
	 */
	public void registerImport(String prefix) {
		this.importPrefixes.add(prefix);
	}

	/**
	 * Remove the specified prefix from this locator's list of imports.
	 * @param prefix the import prefix to remove
	 */
	public void removeImport(String prefix) {
		this.importPrefixes.remove(prefix);
	}

	/**
	 * Get the list of import prefixes registered with this {@code StandardTypeLocator}.
	 * @return the list of registered import prefixes
	 */
	public List<String> getImportPrefixes() {
		return Collections.unmodifiableList(this.importPrefixes);
	}


	/**
	 * Find a (possibly unqualified) type reference, first using the type name as-is,
	 * and then trying any registered import prefixes if the type name cannot be found.
	 * @param typeName the type to locate
	 * @return the class object for the type
	 * @throws EvaluationException if the type cannot be found
	 */
	@Override
	public Class<?> findType(String typeName) throws EvaluationException {
		Class<?> cachedType = this.typeCache.get(typeName);
		if (cachedType != null) {
			return cachedType;
		}
		Class<?> loadedType = loadType(typeName);
		if (loadedType != null) {
			if (!(this.classLoader instanceof SmartClassLoader scl && scl.isClassReloadable(loadedType))) {
				this.typeCache.put(typeName, loadedType);
			}
			return loadedType;
		}
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
	}

	@Nullable
	private Class<?> loadType(String typeName) {
		try {
			return ClassUtils.forName(typeName, this.classLoader);
		}
		catch (ClassNotFoundException ex) {
			// try any registered prefixes before giving up
		}
		for (String prefix : this.importPrefixes) {
			try {
				String nameToLookup = prefix + '.' + typeName;
				return ClassUtils.forName(nameToLookup, this.classLoader);
			}
			catch (ClassNotFoundException ex) {
				// might be a different prefix
			}
		}
		return null;
	}

}
