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

package org.springframework.aot.test.generate.compile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.springframework.lang.Nullable;

/**
 * {@link ClassLoader} implementation to support
 * {@link CompileWithTargetClassAccess @CompileWithTargetClassAccess}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
final class CompileWithTargetClassAccessClassLoader extends ClassLoader {

	private final ClassLoader testClassLoader;


	public CompileWithTargetClassAccessClassLoader(ClassLoader testClassLoader) {
		super(testClassLoader.getParent());
		this.testClassLoader = testClassLoader;
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
		String resourceName = name.replace(".", "/") + ".class";
		InputStream stream = this.testClassLoader.getResourceAsStream(resourceName);
		if (stream != null) {
			try (stream) {
				byte[] bytes = stream.readAllBytes();
				return defineClass(name, bytes, 0, bytes.length, null);
			}
			catch (IOException ex) {
			}
		}
		return super.findClass(name);
	}


	// Invoked reflectively by DynamicClassLoader.findDefineClassMethod(ClassLoader)
	Class<?> defineClassWithTargetAccess(String name, byte[] b, int off, int len) {
		return super.defineClass(name, b, off, len);
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
