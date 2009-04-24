/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.classreading;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Caching implementation of the {@link MetadataReaderFactory} interface,
 * caching an ASM {@link org.springframework.asm.ClassReader} per Spring Resource handle
 * (i.e. per ".class" file).
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class CachingMetadataReaderFactory extends SimpleMetadataReaderFactory {

	private final Map<Resource, MetadataReader> classReaderCache = new HashMap<Resource, MetadataReader>();


	/**
	 * Create a new CachingMetadataReaderFactory for the default class loader.
	 */
	public CachingMetadataReaderFactory() {
		super();
	}

	/**
	 * Create a new CachingMetadataReaderFactory for the given resource loader.
	 * @param resourceLoader the Spring ResourceLoader to use
	 * (also determines the ClassLoader to use)
	 */
	public CachingMetadataReaderFactory(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	/**
	 * Create a new CachingMetadataReaderFactory for the given class loader.
	 * @param classLoader the ClassLoader to use
	 */
	public CachingMetadataReaderFactory(ClassLoader classLoader) {
		super(classLoader);
	}


	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		synchronized (this.classReaderCache) {
			MetadataReader metadataReader = this.classReaderCache.get(resource);
			if (metadataReader == null) {
				metadataReader = super.getMetadataReader(resource);
				this.classReaderCache.put(resource, metadataReader);
			}
			return metadataReader;
		}
	}

}
