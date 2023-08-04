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

package org.springframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Resource} implementation for class path resources. Uses either a
 * given {@link ClassLoader} or a given {@link Class} for loading resources.
 *
 * <p>Supports resolution as {@code java.io.File} if the class path
 * resource resides in the file system, but not for resources in a JAR.
 * Always supports resolution as {@code java.net.URL}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * @see ClassLoader#getResourceAsStream(String)
 * @see ClassLoader#getResource(String)
 * @see Class#getResourceAsStream(String)
 * @see Class#getResource(String)
 */
public class ClassPathResource extends AbstractFileResolvingResource {

	/**
	 * Internal representation of the original path supplied by the user,
	 * used for creating relative paths and resolving URLs and InputStreams.
	 */
	private final String path;

	private final String absolutePath;

	@Nullable
	private final ClassLoader classLoader;

	@Nullable
	private final Class<?> clazz;


	/**
	 * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
	 * <p>A leading slash will be removed, as the {@code ClassLoader} resource
	 * access methods will not accept it.
	 * <p>The default class loader will be used for loading the resource.
	 * @param path the absolute path within the class path
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	public ClassPathResource(String path) {
		this(path, (ClassLoader) null);
	}

	/**
	 * Create a new {@code ClassPathResource} for {@code ClassLoader} usage.
	 * <p>A leading slash will be removed, as the {@code ClassLoader} resource
	 * access methods will not accept it.
	 * <p>If the supplied {@code ClassLoader} is {@code null}, the default class
	 * loader will be used for loading the resource.
	 * @param path the absolute path within the class path
	 * @param classLoader the class loader to load the resource with
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	public ClassPathResource(String path, @Nullable ClassLoader classLoader) {
		Assert.notNull(path, "Path must not be null");
		String pathToUse = StringUtils.cleanPath(path);
		if (pathToUse.startsWith("/")) {
			pathToUse = pathToUse.substring(1);
		}
		this.path = pathToUse;
		this.absolutePath = pathToUse;
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.clazz = null;
	}

	/**
	 * Create a new {@code ClassPathResource} for {@code Class} usage.
	 * <p>The path can be relative to the given class, or absolute within
	 * the class path via a leading slash.
	 * <p>If the supplied {@code Class} is {@code null}, the default class
	 * loader will be used for loading the resource.
	 * <p>This is also useful for resource access within the module system,
	 * loading a resource from the containing module of a given {@code Class}.
	 * See {@link ModuleResource} and its javadoc.
	 * @param path relative or absolute path within the class path
	 * @param clazz the class to load resources with
	 * @see ClassUtils#getDefaultClassLoader()
	 * @see ModuleResource
	 */
	public ClassPathResource(String path, @Nullable Class<?> clazz) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);

		String absolutePath = this.path;
		if (clazz != null && !absolutePath.startsWith("/")) {
			absolutePath = ClassUtils.classPackageAsResourcePath(clazz) + "/" + absolutePath;
		}
		else if (absolutePath.startsWith("/")) {
			absolutePath = absolutePath.substring(1);
		}
		this.absolutePath = absolutePath;

		this.classLoader = null;
		this.clazz = clazz;
	}


	/**
	 * Return the <em>absolute path</em> for this resource, as a
	 * {@linkplain StringUtils#cleanPath(String) cleaned} resource path within
	 * the class path.
	 * <p>The path returned by this method does not have a leading slash and is
	 * suitable for use with {@link ClassLoader#getResource(String)}.
	 */
	public final String getPath() {
		return this.absolutePath;
	}

	/**
	 * Return the {@link ClassLoader} that this resource will be obtained from.
	 */
	@Nullable
	public final ClassLoader getClassLoader() {
		return (this.clazz != null ? this.clazz.getClassLoader() : this.classLoader);
	}


	/**
	 * This implementation checks for the resolution of a resource URL.
	 * @see ClassLoader#getResource(String)
	 * @see Class#getResource(String)
	 */
	@Override
	public boolean exists() {
		return (resolveURL() != null);
	}

	/**
	 * This implementation checks for the resolution of a resource URL upfront,
	 * then proceeding with {@link AbstractFileResolvingResource}'s length check.
	 * @see ClassLoader#getResource(String)
	 * @see Class#getResource(String)
	 */
	@Override
	public boolean isReadable() {
		URL url = resolveURL();
		return (url != null && checkReadable(url));
	}

	/**
	 * Resolves a {@link URL} for the underlying class path resource.
	 * @return the resolved URL, or {@code null} if not resolvable
	 */
	@Nullable
	protected URL resolveURL() {
		try {
			if (this.clazz != null) {
				return this.clazz.getResource(this.path);
			}
			else if (this.classLoader != null) {
				return this.classLoader.getResource(this.absolutePath);
			}
			else {
				return ClassLoader.getSystemResource(this.absolutePath);
			}
		}
		catch (IllegalArgumentException ex) {
			// Should not happen according to the JDK's contract:
			// see https://github.com/openjdk/jdk/pull/2662
			return null;
		}
	}

	/**
	 * This implementation opens an {@link InputStream} for the underlying class
	 * path resource, if available.
	 * @see ClassLoader#getResourceAsStream(String)
	 * @see Class#getResourceAsStream(String)
	 * @see ClassLoader#getSystemResourceAsStream(String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is;
		if (this.clazz != null) {
			is = this.clazz.getResourceAsStream(this.path);
		}
		else if (this.classLoader != null) {
			is = this.classLoader.getResourceAsStream(this.absolutePath);
		}
		else {
			is = ClassLoader.getSystemResourceAsStream(this.absolutePath);
		}
		if (is == null) {
			throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
		}
		return is;
	}

	/**
	 * This implementation returns a URL for the underlying class path resource,
	 * if available.
	 * @see ClassLoader#getResource(String)
	 * @see Class#getResource(String)
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = resolveURL();
		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * This implementation creates a {@code ClassPathResource}, applying the given
	 * path relative to the path used to create this descriptor.
	 * @see StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return (this.clazz != null ? new ClassPathResource(pathToUse, this.clazz) :
				new ClassPathResource(pathToUse, this.classLoader));
	}

	/**
	 * This implementation returns the name of the file that this class path
	 * resource refers to.
	 * @see StringUtils#getFilename(String)
	 */
	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.absolutePath);
	}

	/**
	 * This implementation returns a description that includes the absolute
	 * class path location.
	 */
	@Override
	public String getDescription() {
		return "class path resource [" + this.absolutePath + "]";
	}


	/**
	 * This implementation compares the underlying class path locations and
	 * associated class loaders.
	 * @see #getPath()
	 * @see #getClassLoader()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ClassPathResource that &&
				this.absolutePath.equals(that.absolutePath) &&
				ObjectUtils.nullSafeEquals(getClassLoader(), that.getClassLoader())));
	}

	/**
	 * This implementation returns the hash code of the underlying class path location.
	 * @see #getPath()
	 */
	@Override
	public int hashCode() {
		return this.absolutePath.hashCode();
	}

}
