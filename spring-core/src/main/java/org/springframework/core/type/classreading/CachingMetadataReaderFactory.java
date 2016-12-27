/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Caching implementation of the {@link MetadataReaderFactory} interface,
 * caching a {@link MetadataReader} instance per Spring {@link Resource} handle
 * (i.e. per ".class" file).
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.5
 */
public class CachingMetadataReaderFactory extends SimpleMetadataReaderFactory {

	/** Default maximum number of entries for the MetadataReader cache: 256 */
	public static final int DEFAULT_CACHE_LIMIT = 256;


	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	@SuppressWarnings("serial")
	private final Map<Resource, MetadataReader> metadataReaderCache =
			new LinkedHashMap<Resource, MetadataReader>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Resource, MetadataReader> eldest) {
					return size() > getCacheLimit();
				}
			};


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


	/**
	 * Specify the maximum number of entries for the MetadataReader cache.
	 * <p>Default is 256.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * Return the maximum number of entries for the MetadataReader cache.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}


	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		if (getCacheLimit() <= 0) {
			return super.getMetadataReader(resource);
		}
		synchronized (this.metadataReaderCache) {
			MetadataReader metadataReader = this.metadataReaderCache.get(resource);
			if (metadataReader == null) {
				metadataReader = super.getMetadataReader(resource);
				this.metadataReaderCache.put(resource, metadataReader);
			}
			return metadataReader;
		}
	}

	/**
	 * Clear the entire MetadataReader cache, removing all cached class metadata.
	 */
	public void clearCache() {
		synchronized (this.metadataReaderCache) {
			this.metadataReaderCache.clear();
		}
	}

}
