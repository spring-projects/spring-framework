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

package org.springframework.aot.nativex.feature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * {@link ClassLoader} used to load classes without causing build-time
 * initialization.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ThrowawayClassLoader extends ClassLoader {

	static {
		registerAsParallelCapable();
	}

	private final ClassLoader resourceLoader;


	ThrowawayClassLoader(ClassLoader parent) {
		super(parent.getParent());
		this.resourceLoader = parent;
	}


	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> loaded = findLoadedClass(name);
			if (loaded != null) {
				return loaded;
			}
			try {
				return super.loadClass(name, true);
			}
			catch (ClassNotFoundException ex) {
				return loadClassFromResource(name);
			}
		}
	}

	private Class<?> loadClassFromResource(String name) throws ClassNotFoundException, ClassFormatError {
		String resourceName = name.replace('.', '/') + ".class";
		InputStream inputStream = this.resourceLoader.getResourceAsStream(resourceName);
		if (inputStream == null) {
			return null;
		}
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			inputStream.transferTo(outputStream);
			byte[] bytes = outputStream.toByteArray();
			return defineClass(name, bytes, 0, bytes.length);

		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	@Override
	protected URL findResource(String name) {
		return this.resourceLoader.getResource(name);
	}

}
