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

package org.springframework.core.test.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Function;

import org.springframework.lang.Nullable;

/**
 * {@link ClassLoader} implementation to support
 * {@link CompileWithForkedClassLoader @CompileWithForkedClassLoader}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 6.0
 */
final class CompileWithForkedClassLoaderClassLoader extends ClassLoader {

	private final ClassLoader testClassLoader;

	private Function<String, byte[]> classResourceLookup = name -> null;


	public CompileWithForkedClassLoaderClassLoader(ClassLoader testClassLoader) {
		super(testClassLoader.getParent());
		this.testClassLoader = testClassLoader;
	}


	// Invoked reflectively by DynamicClassLoader
	@SuppressWarnings("unused")
	void setClassResourceLookup(Function<String, byte[]> classResourceLookup) {
		this.classResourceLookup = classResourceLookup;
	}

	// Invoked reflectively by DynamicClassLoader
	@SuppressWarnings("unused")
	Class<?> defineDynamicClass(String name, byte[] b, int off, int len) {
		return super.defineClass(name, b, off, len);
	}


	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (name.startsWith("org.junit") || name.startsWith("org.testng")) {
			return Class.forName(name, false, this.testClassLoader);
		}
		return super.loadClass(name);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytes = findClassBytes(name);
		return (bytes != null) ? defineClass(name, bytes, 0, bytes.length, null) : super.findClass(name);
	}

	@Nullable
	private byte[] findClassBytes(String name) {
		byte[] bytes = this.classResourceLookup.apply(name);
		if (bytes != null) {
			return bytes;
		}
		String resourceName = name.replace(".", "/") + ".class";
		InputStream stream = this.testClassLoader.getResourceAsStream(resourceName);
		if (stream != null) {
			try (stream) {
				return stream.readAllBytes();
			}
			catch (IOException ex) {
				// ignore
			}
		}
		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		return this.testClassLoader.getResources(name);
	}

	@Override
	@Nullable
	protected URL findResource(String name) {
		return this.testClassLoader.getResource(name);
	}

}
