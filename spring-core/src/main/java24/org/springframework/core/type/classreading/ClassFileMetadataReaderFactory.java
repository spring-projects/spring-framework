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
 * Implementation of the {@link MetadataReaderFactory} interface,
 * using the {@link java.lang.classfile.ClassFile} API for parsing the bytecode.
 *
 * @author Brian Clozel
 * @since 7.0
 */
final class ClassFileMetadataReaderFactory extends AbstractMetadataReaderFactory {

	/**
	 * Create a new ClassFileMetadataReaderFactory for the default class loader.
	 */
	public ClassFileMetadataReaderFactory() {
		super();
	}

	/**
	 * Create a new ClassFileMetadataReaderFactory for the given resource loader.
	 * @param resourceLoader the Spring ResourceLoader to use
	 * (also determines the ClassLoader to use)
	 */
	public ClassFileMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	/**
	 * Create a new ClassFileMetadataReaderFactory for the given class loader.
	 * @param classLoader the ClassLoader to use
	 */
	public ClassFileMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		super(classLoader);
	}


	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		return new ClassFileMetadataReader(resource, getResourceLoader().getClassLoader());
	}

}
