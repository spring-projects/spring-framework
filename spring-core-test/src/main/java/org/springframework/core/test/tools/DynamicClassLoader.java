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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ClassLoader} used to expose dynamically generated content.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 6.0
 */
public class DynamicClassLoader extends ClassLoader {

	private final ClassFiles classFiles;

	private final ResourceFiles resourceFiles;

	private final Map<String, DynamicClassFileObject> dynamicClassFiles;

	private final Map<String, DynamicResourceFileObject> dynamicResourceFiles;

	@Nullable
	private final Method defineClassMethod;


	public DynamicClassLoader(ClassLoader parent, ClassFiles classFiles, ResourceFiles resourceFiles,
			Map<String, DynamicClassFileObject> dynamicClassFiles,
			Map<String, DynamicResourceFileObject> dynamicResourceFiles) {

		super(parent);
		this.classFiles = classFiles;
		this.resourceFiles = resourceFiles;
		this.dynamicClassFiles = dynamicClassFiles;
		this.dynamicResourceFiles = dynamicResourceFiles;
		Class<? extends ClassLoader> parentClass = parent.getClass();
		if (parentClass.getName().equals(CompileWithForkedClassLoaderClassLoader.class.getName())) {
			Method setClassResourceLookupMethod = lookupMethod(parentClass,
					"setClassResourceLookup", Function.class);
			ReflectionUtils.makeAccessible(setClassResourceLookupMethod);
			ReflectionUtils.invokeMethod(setClassResourceLookupMethod,
					getParent(), (Function<String, byte[]>) this::findClassBytes);
			this.defineClassMethod = lookupMethod(parentClass,
					"defineDynamicClass", String.class, byte[].class, int.class, int.class);
			ReflectionUtils.makeAccessible(this.defineClassMethod);
			this.dynamicClassFiles.forEach((name, file) -> defineClass(name, file.getBytes()));
		}
		else {
			this.defineClassMethod = null;
		}
	}


	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> clazz = defineClass(name, findClassBytes(name));
		return (clazz != null ? clazz : super.findClass(name));
	}

	@Nullable
	private Class<?> defineClass(String name, @Nullable byte[] bytes) {
		if (bytes == null) {
			return null;
		}
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
		if (name.endsWith(ClassUtils.CLASS_FILE_SUFFIX)) {
			String className = ClassUtils.convertResourcePathToClassName(name.substring(0,
					name.length() - ClassUtils.CLASS_FILE_SUFFIX.length()));
			byte[] classBytes = findClassBytes(className);
			if (classBytes != null) {
				return createResourceUrl(name, () -> classBytes);
			}
		}
		ResourceFile resourceFile = this.resourceFiles.get(name);
		if (resourceFile != null) {
			return createResourceUrl(resourceFile.getPath(), resourceFile::getBytes);
		}
		DynamicResourceFileObject dynamicResourceFile = this.dynamicResourceFiles.get(name);
		if (dynamicResourceFile != null && dynamicResourceFile.getBytes() != null) {
			return createResourceUrl(dynamicResourceFile.getName(), dynamicResourceFile::getBytes);
		}
		return super.findResource(name);
	}

	@Nullable
	private byte[] findClassBytes(String name) {
		ClassFile classFile = this.classFiles.get(name);
		if (classFile != null) {
			return classFile.getContent();
		}
		DynamicClassFileObject dynamicClassFile = this.dynamicClassFiles.get(name);
		return (dynamicClassFile != null ? dynamicClassFile.getBytes() : null);
	}

	@SuppressWarnings("deprecation")  // on JDK 20
	private URL createResourceUrl(String name, Supplier<byte[]> bytesSupplier) {
		try {
			return new URL(null, "resource:///" + name,
					new ResourceFileHandler(bytesSupplier));
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static Method lookupMethod(Class<?> target, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(target, name, parameterTypes);
		Assert.notNull(method, () -> "Could not find method '%s' on '%s'".formatted(name, target.getName()));
		return method;
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

		private final Supplier<byte[]> bytesSupplier;


		ResourceFileHandler(Supplier<byte[]> bytesSupplier) {
			this.bytesSupplier = bytesSupplier;
		}


		@Override
		protected URLConnection openConnection(URL url) {
			return new ResourceFileConnection(url, this.bytesSupplier);
		}

	}


	private static class ResourceFileConnection extends URLConnection {

		private final Supplier<byte[]> bytesSupplier;


		protected ResourceFileConnection(URL url, Supplier<byte[]> bytesSupplier) {
			super(url);
			this.bytesSupplier = bytesSupplier;
		}


		@Override
		public void connect() {
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(this.bytesSupplier.get());
		}

	}

}
