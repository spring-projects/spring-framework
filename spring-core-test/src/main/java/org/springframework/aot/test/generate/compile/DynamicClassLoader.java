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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Function;

import org.springframework.aot.test.generate.file.ClassFile;
import org.springframework.aot.test.generate.file.ClassFiles;
import org.springframework.aot.test.generate.file.ResourceFile;
import org.springframework.aot.test.generate.file.ResourceFiles;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ClassLoader} used to expose dynamically generated content.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DynamicClassLoader extends ClassLoader {

	private final ResourceFiles resourceFiles;

	private final ClassFiles classFiles;

	private final Map<String, DynamicClassFileObject> compiledClasses;

	@Nullable
	private final Method defineClassMethod;


	public DynamicClassLoader(ClassLoader parent, ResourceFiles resourceFiles,
			ClassFiles classFiles, Map<String, DynamicClassFileObject> compiledClasses) {

		super(parent);
		this.resourceFiles = resourceFiles;
		this.classFiles = classFiles;
		this.compiledClasses = compiledClasses;
		Class<? extends ClassLoader> parentClass = parent.getClass();
		if (parentClass.getName().equals(CompileWithTargetClassAccessClassLoader.class.getName())) {
			Method setClassResourceLookupMethod = ReflectionUtils.findMethod(parentClass,
					"setClassResourceLookup", Function.class);
			ReflectionUtils.makeAccessible(setClassResourceLookupMethod);
			ReflectionUtils.invokeMethod(setClassResourceLookupMethod,
					getParent(), (Function<String, byte[]>) this::findClassBytes);
			this.defineClassMethod = ReflectionUtils.findMethod(parentClass,
					"defineClassWithTargetAccess", String.class, byte[].class, int.class, int.class);
			ReflectionUtils.makeAccessible(this.defineClassMethod);
			this.compiledClasses.forEach((name, file) -> defineClass(name, file.getBytes()));
		}
		else {
			this.defineClassMethod = null;
		}
	}


	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytes = findClassBytes(name);
		if (bytes != null) {
			return defineClass(name, bytes);
		}
		return super.findClass(name);
	}

	@Nullable
	private byte[] findClassBytes(String name) {
		DynamicClassFileObject compiledClass = this.compiledClasses.get(name);
		if(compiledClass != null) {
			return compiledClass.getBytes();
		}
		return findClassFileBytes(name);
	}

	@Nullable
	private byte[] findClassFileBytes(String name) {
		ClassFile classFile = this.classFiles.get(name);
		if (classFile != null) {
			return classFile.getContent();
		}
		return null;
	}

	private Class<?> defineClass(String name, byte[] bytes) {
		if (this.defineClassMethod != null) {
			return (Class<?>) ReflectionUtils.invokeMethod(this.defineClassMethod,
					getParent(), name, bytes, 0, bytes.length);
		}
		return defineClass(name, bytes, 0, bytes.length);
	}


	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		URL resource = findResource(name);
		if (resource != null) {
			return new SingletonEnumeration<>(resource);
		}
		return super.findResources(name);
	}

	@Override
	@Nullable
	protected URL findResource(String name) {
		ResourceFile file = this.resourceFiles.get(name);
		if (file != null) {
			try {
				return new URL(null, "resource:///" + file.getPath(),
						new ResourceFileHandler(file));
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return super.findResource(name);
	}


	private static class SingletonEnumeration<E> implements Enumeration<E> {

		@Nullable
		private E element;


		SingletonEnumeration(@Nullable E element) {
			this.element = element;
		}


		@Override
		public boolean hasMoreElements() {
			return this.element != null;
		}

		@Override
		@Nullable
		public E nextElement() {
			E next = this.element;
			this.element = null;
			return next;
		}

	}


	private static class ResourceFileHandler extends URLStreamHandler {

		private final ResourceFile file;


		ResourceFileHandler(ResourceFile file) {
			this.file = file;
		}


		@Override
		protected URLConnection openConnection(URL url) {
			return new ResourceFileConnection(url, this.file);
		}

	}


	private static class ResourceFileConnection extends URLConnection {

		private final ResourceFile file;


		protected ResourceFileConnection(URL url, ResourceFile file) {
			super(url);
			this.file = file;
		}


		@Override
		public void connect() {
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(
					this.file.getContent().getBytes(StandardCharsets.UTF_8));
		}

	}

}
