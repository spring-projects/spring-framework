/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.test.tools;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Fully compiled results provided from a {@link TestCompiler}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class Compiled {

	private final ClassLoader classLoader;

	private final SourceFiles sourceFiles;

	private final ResourceFiles resourceFiles;

	@Nullable
	private List<Class<?>> compiledClasses;


	Compiled(ClassLoader classLoader, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
		this.classLoader = classLoader;
		this.sourceFiles = sourceFiles;
		this.resourceFiles = resourceFiles;
	}


	/**
	 * Return the classloader containing the compiled content and access to the
	 * resources.
	 * @return the classLoader
	 */
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Return the single source file that was compiled.
	 * @return the single source file
	 * @throws IllegalStateException if the compiler wasn't passed exactly one
	 * file
	 */
	public SourceFile getSourceFile() {
		return this.sourceFiles.getSingle();
	}

	/**
	 * Return the single matching source file that was compiled.
	 * @param pattern the pattern used to find the file
	 * @return the single source file
	 * @throws IllegalStateException if the compiler wasn't passed exactly one
	 * file
	 */
	public SourceFile getSourceFile(String pattern) {
		return this.sourceFiles.getSingle(pattern);
	}

	/**
	 * Return the single source file that was compiled in the given package.
	 * @param packageName the package name to check
	 * @return the single source file
	 * @throws IllegalStateException if the compiler wasn't passed exactly one
	 * file
	 */
	public SourceFile getSourceFileFromPackage(String packageName) {
		return this.sourceFiles.getSingleFromPackage(packageName);
	}

	/**
	 * Return all source files that were compiled.
	 * @return the source files used by the compiler
	 */
	public SourceFiles getSourceFiles() {
		return this.sourceFiles;
	}

	/**
	 * Return the single resource file that was used when compiled.
	 * @return the single resource file
	 * @throws IllegalStateException if the compiler wasn't passed exactly one
	 * file
	 */
	public ResourceFile getResourceFile() {
		return this.resourceFiles.getSingle();
	}

	/**
	 * Return all resource files that were compiled.
	 * @return the resource files used by the compiler
	 */
	public ResourceFiles getResourceFiles() {
		return this.resourceFiles;
	}

	/**
	 * Return a new instance of a compiled class of the given type. There must
	 * be only a single instance and it must have a default constructor.
	 * @param <T> the required type
	 * @param type the required type
	 * @return an instance of type created from the compiled classes
	 * @throws IllegalStateException if no instance can be found or instantiated
	 */
	public <T> T getInstance(Class<T> type) {
		List<Class<?>> matching = getAllCompiledClasses().stream().filter(type::isAssignableFrom).toList();
		Assert.state(!matching.isEmpty(), () -> "No instance found of type " + type.getName());
		Assert.state(matching.size() == 1, () -> "Multiple instances found of type " + type.getName());
		return type.cast(newInstance(matching.get(0)));
	}

	/**
	 * Return an instance of a compiled class identified by its class name. The
	 * class must have a default constructor.
	 * @param <T> the type to return
	 * @param type the type to return
	 * @param className the class name to load
	 * @return an instance of the class
	 * @throws IllegalStateException if no instance can be found or instantiated
	 */
	public <T> T getInstance(Class<T> type, String className) {
		Class<?> loaded = loadClass(className);
		return type.cast(newInstance(loaded));
	}

	/**
	 * Return all compiled classes.
	 * @return a list of all compiled classes
	 */
	public List<Class<?>> getAllCompiledClasses() {
		List<Class<?>> compiledClasses = this.compiledClasses;
		if (compiledClasses == null) {
			compiledClasses = new ArrayList<>();
			this.sourceFiles.stream().map(this::loadClass).forEach(compiledClasses::add);
			this.compiledClasses = Collections.unmodifiableList(compiledClasses);
		}
		return compiledClasses;
	}

	private Object newInstance(Class<?> loaded) {
		try {
			Constructor<?> constructor = loaded.getDeclaredConstructor();
			return constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Class<?> loadClass(SourceFile sourceFile) {
		return loadClass(sourceFile.getClassName());
	}

	private Class<?> loadClass(String className) {
		try {
			return this.classLoader.loadClass(className);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
