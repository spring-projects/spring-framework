/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Factory interface for {@link MetadataReader} instances.
 * Allows for caching a MetadataReader per original resource.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 2.5
 * @see SimpleMetadataReaderFactory
 * @see CachingMetadataReaderFactory
 */
public interface MetadataReaderFactory {

	/**
	 * Obtain a MetadataReader for the given class name.
	 * @param className the class name (to be resolved to a ".class" file)
	 * @return a holder for the ClassReader instance (never {@code null})
	 * @throws ClassFormatException in case of an incompatible class format
	 * @throws IOException in case of I/O failure
	 */
	MetadataReader getMetadataReader(String className) throws IOException;

	/**
	 * Obtain a MetadataReader for the given resource.
	 * @param resource the resource (pointing to a ".class" file)
	 * @return a holder for the ClassReader instance (never {@code null})
	 * @throws ClassFormatException in case of an incompatible class format
	 * @throws IOException in case of I/O failure
	 */
	MetadataReader getMetadataReader(Resource resource) throws IOException;

	/**
	 * Return the ResourceLoader that this MetadataReaderFactory has been
	 * constructed with.
	 * @since 7.0
	 */
	ResourceLoader getResourceLoader();

	/**
	 * Create a default {@link MetadataReaderFactory} implementation that's suitable
	 * for the current JVM.
	 * @return a new factory instance
	 * @since 7.0
	 */
	static MetadataReaderFactory create(@Nullable ResourceLoader resourceLoader) {
		return MetadataReaderFactoryDelegate.create(resourceLoader);
	}

	/**
	 * Create a default {@link MetadataReaderFactory} implementation that's suitable
	 * for the current JVM.
	 * @return a new factory instance
	 * @since 7.0
	 */
	static MetadataReaderFactory create(@Nullable ClassLoader classLoader) {
		return MetadataReaderFactoryDelegate.create(classLoader);
	}
}
