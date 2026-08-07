/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.type.classreading;

import org.jspecify.annotations.Nullable;

import org.springframework.core.SpringProperties;
import org.springframework.core.io.ResourceLoader;

/**
 * Internal delegate for instantiating {@link MetadataReaderFactory} implementations.
 * For JDK &gt;= 24, the {@link ClassFileMetadataReaderFactory} is used by default.
 *
 * @author Brian Clozel
 * @since 7.0
 * @see MetadataReaderFactory
 */
abstract class MetadataReaderFactoryDelegate {

	/**
	 * Spring property that switches back to the ASM-based {@link SimpleMetadataReaderFactory}
	 * on Java 24+, for example {@code -Dspring.classformat.metadatareader.asm=true}.
	 * @since 7.0.x
	 */
	static final String ASM_METADATA_READER_PROPERTY_NAME = "spring.classformat.metadatareader.asm";


	static MetadataReaderFactory create(@Nullable ResourceLoader resourceLoader) {
		if (useAsmMetadataReader()) {
			return new SimpleMetadataReaderFactory(resourceLoader);
		}
		return new ClassFileMetadataReaderFactory(resourceLoader);
	}

	static MetadataReaderFactory create(@Nullable ClassLoader classLoader) {
		if (useAsmMetadataReader()) {
			return new SimpleMetadataReaderFactory(classLoader);
		}
		return new ClassFileMetadataReaderFactory(classLoader);
	}

	private static boolean useAsmMetadataReader() {
		return SpringProperties.getFlag(ASM_METADATA_READER_PROPERTY_NAME);
	}

}
