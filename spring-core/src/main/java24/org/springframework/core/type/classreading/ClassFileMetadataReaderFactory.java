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
public class ClassFileMetadataReaderFactory extends AbstractMetadataReaderFactory {


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
