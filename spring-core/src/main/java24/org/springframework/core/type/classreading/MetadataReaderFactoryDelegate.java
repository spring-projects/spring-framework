/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ResourceLoader;

/**
 * Internal delegate for instantiating {@link MetadataReaderFactory} implementations.
 * For JDK >= 24, the {@link ClassFileMetadataReaderFactory} is used.
 *
 * @author Brian Clozel
 * @since 7.0
 * @see MetadataReaderFactory
 */
abstract class MetadataReaderFactoryDelegate {

	static MetadataReaderFactory create(@Nullable ResourceLoader resourceLoader) {
		return new ClassFileMetadataReaderFactory(resourceLoader);
	}

	static MetadataReaderFactory create(@Nullable ClassLoader classLoader) {
		return new ClassFileMetadataReaderFactory(classLoader);
	}

}
