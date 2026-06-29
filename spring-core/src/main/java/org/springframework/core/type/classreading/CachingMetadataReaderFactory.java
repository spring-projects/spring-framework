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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 * Caching implementation of the {@link MetadataReaderFactory} interface,
 * caching a {@link MetadataReader} instance per class name.
 * <p>Metadata is also associated with the originating {@link Resource} handle
 * in the shared {@link DefaultResourceLoader} resource cache when available.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Brian Clozel
 * @since 2.5
 */
public class CachingMetadataReaderFactory extends AbstractMetadataReaderFactory {

	/** Default maximum number of entries for a local MetadataReader cache: 256. */
	public static final int DEFAULT_CACHE_LIMIT = 256;

	private static final String CLASSES_DIR = "/classes/";

	private static final String TEST_CLASSES_DIR = "/test-classes/";

	private final MetadataReaderFactory delegate;

	/** MetadataReader cache keyed by class name. */
	private @Nullable Map<String, MetadataReader> classNameMetadataReaderCache;

	/** MetadataReader cache: either local or shared at the ResourceLoader level. */
	private @Nullable Map<Resource, MetadataReader> metadataReaderCache;


	/**
	 * Create a new CachingMetadataReaderFactory for the default class loader,
	 * using a local resource cache.
	 */
	public CachingMetadataReaderFactory() {
		this(MetadataReaderFactory.create((ClassLoader) null));
	}

	/**
	 * Create a new CachingMetadataReaderFactory for the given {@link ClassLoader},
	 * using a local resource cache.
	 * @param classLoader the ClassLoader to use
	 */
	public CachingMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		this(MetadataReaderFactory.create(classLoader));
	}

	/**
	 * Create a new CachingMetadataReaderFactory for the given {@link ResourceLoader},
	 * using a shared resource cache if supported or a local resource cache otherwise.
	 * @param resourceLoader the Spring ResourceLoader to use
	 * (also determines the ClassLoader to use)
	 * @see DefaultResourceLoader#getResourceCache
	 */
	public CachingMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		this(MetadataReaderFactory.create(resourceLoader));
	}

	CachingMetadataReaderFactory(MetadataReaderFactory delegate) {
		super(delegate.getResourceLoader());
		this.delegate = delegate;
		if (getResourceLoader() instanceof DefaultResourceLoader defaultResourceLoader) {
			this.metadataReaderCache = defaultResourceLoader.getResourceCache(MetadataReader.class);
			this.classNameMetadataReaderCache = new ConcurrentHashMap<>();
		}
		else {
			setCacheLimit(DEFAULT_CACHE_LIMIT);
		}
	}


	/**
	 * Specify the maximum number of entries for the MetadataReader cache.
	 * <p>Default is 256 for a local cache, whereas a shared cache is
	 * typically unbounded. This method enforces a local resource cache,
	 * even if the {@link ResourceLoader} supports a shared resource cache.
	 */
	public void setCacheLimit(int cacheLimit) {
		if (cacheLimit <= 0) {
			this.metadataReaderCache = null;
			this.classNameMetadataReaderCache = null;
		}
		else if (this.metadataReaderCache instanceof LocalResourceCache localResourceCache) {
			localResourceCache.setCacheLimit(cacheLimit);
			if (this.classNameMetadataReaderCache instanceof LocalClassNameCache localClassNameCache) {
				localClassNameCache.setCacheLimit(cacheLimit);
			}
		}
		else {
			this.metadataReaderCache = new LocalResourceCache(cacheLimit);
			this.classNameMetadataReaderCache = new LocalClassNameCache(cacheLimit);
		}
	}

	/**
	 * Return the maximum number of entries for the MetadataReader cache.
	 */
	public int getCacheLimit() {
		if (this.metadataReaderCache instanceof LocalResourceCache localResourceCache) {
			return localResourceCache.getCacheLimit();
		}
		else {
			return (this.metadataReaderCache != null ? Integer.MAX_VALUE : 0);
		}
	}

	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		@Nullable String className = resolveClassName(resource);
		if (className != null) {
			MetadataReader metadataReader = getFromClassNameCache(className);
			if (metadataReader != null) {
				cacheMetadataReader(resource, metadataReader);
				return metadataReader;
			}
		}

		MetadataReader metadataReader = getFromResourceCache(resource);
		if (metadataReader != null) {
			if (className != null) {
				cacheMetadataReaderByClassName(className, metadataReader);
			}
			return metadataReader;
		}

		metadataReader = this.delegate.getMetadataReader(resource);
		if (className == null) {
			className = metadataReader.getClassMetadata().getClassName();
		}
		MetadataReader existing = getFromClassNameCache(className);
		if (existing != null) {
			cacheMetadataReader(resource, existing);
			return existing;
		}
		cacheMetadataReader(resource, metadataReader);
		cacheMetadataReaderByClassName(className, metadataReader);
		return metadataReader;
	}

	/**
	 * Clear the local MetadataReader cache, if any, removing all cached class metadata.
	 */
	public void clearCache() {
		if (this.classNameMetadataReaderCache instanceof LocalClassNameCache) {
			synchronized (this.classNameMetadataReaderCache) {
				this.classNameMetadataReaderCache.clear();
			}
		}
		else if (this.classNameMetadataReaderCache != null) {
			this.classNameMetadataReaderCache.clear();
		}
		if (this.metadataReaderCache instanceof LocalResourceCache) {
			synchronized (this.metadataReaderCache) {
				this.metadataReaderCache.clear();
			}
		}
		else if (this.metadataReaderCache != null) {
			// Shared resource cache -> reset to local cache.
			setCacheLimit(DEFAULT_CACHE_LIMIT);
		}
	}

	private @Nullable MetadataReader getFromClassNameCache(String className) {
		if (this.classNameMetadataReaderCache instanceof ConcurrentMap<String, MetadataReader> concurrentMap) {
			return concurrentMap.get(className);
		}
		else if (this.classNameMetadataReaderCache != null) {
			synchronized (this.classNameMetadataReaderCache) {
				return this.classNameMetadataReaderCache.get(className);
			}
		}
		return null;
	}

	private @Nullable MetadataReader getFromResourceCache(Resource resource) {
		if (this.metadataReaderCache instanceof ConcurrentMap<Resource, MetadataReader> concurrentMap) {
			return concurrentMap.get(resource);
		}
		else if (this.metadataReaderCache != null) {
			synchronized (this.metadataReaderCache) {
				return this.metadataReaderCache.get(resource);
			}
		}
		return null;
	}

	private void cacheMetadataReader(Resource resource, MetadataReader metadataReader) {
		if (this.metadataReaderCache instanceof ConcurrentMap<Resource, MetadataReader> concurrentMap) {
			concurrentMap.putIfAbsent(resource, metadataReader);
		}
		else if (this.metadataReaderCache != null) {
			synchronized (this.metadataReaderCache) {
				this.metadataReaderCache.putIfAbsent(resource, metadataReader);
			}
		}
	}

	private void cacheMetadataReaderByClassName(MetadataReader metadataReader) {
		cacheMetadataReaderByClassName(metadataReader.getClassMetadata().getClassName(), metadataReader);
	}

	private void cacheMetadataReaderByClassName(String className, MetadataReader metadataReader) {
		if (this.classNameMetadataReaderCache instanceof ConcurrentMap<String, MetadataReader> concurrentMap) {
			concurrentMap.putIfAbsent(className, metadataReader);
		}
		else if (this.classNameMetadataReaderCache != null) {
			synchronized (this.classNameMetadataReaderCache) {
				this.classNameMetadataReaderCache.putIfAbsent(className, metadataReader);
			}
		}
	}

	static @Nullable String resolveClassName(Resource resource) {
		if (resource instanceof ClassPathResource classPathResource) {
			return toClassName(classPathResource.getPath());
		}
		try {
			String urlString = resource.getURL().toString();
			int separatorIndex = urlString.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
			String path = (separatorIndex >= 0 ?
					urlString.substring(separatorIndex + ResourceUtils.JAR_URL_SEPARATOR.length()) :
					resource.getURL().getPath());
			path = URLDecoder.decode(path, StandardCharsets.UTF_8);
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			return toClassName(extractClassFileResourcePath(path));
		}
		catch (IOException ex) {
			return null;
		}
	}

	private static final String[] CLASS_FILE_ROOT_MARKERS = {
			"/classes/java/test/", "/classes/java/main/",
			"/classes/kotlin/test/", "/classes/kotlin/main/",
			CLASSES_DIR, TEST_CLASSES_DIR
	};

	private static String extractClassFileResourcePath(String path) {
		int classFileIndex = path.indexOf(ClassUtils.CLASS_FILE_SUFFIX);
		if (classFileIndex < 0) {
			return path;
		}
		String beforeSuffix = path.substring(0, classFileIndex);
		int bestStart = -1;
		for (String marker : CLASS_FILE_ROOT_MARKERS) {
			int index = beforeSuffix.indexOf(marker);
			if (index >= 0) {
				int start = index + marker.length();
				if (start > bestStart) {
					bestStart = start;
				}
			}
		}
		if (bestStart >= 0) {
			return path.substring(bestStart, classFileIndex + ClassUtils.CLASS_FILE_SUFFIX.length());
		}
		return path;
	}

	private static @Nullable String toClassName(String resourcePath) {
		if (!resourcePath.endsWith(ClassUtils.CLASS_FILE_SUFFIX)) {
			return null;
		}
		String pathWithoutSuffix = resourcePath.substring(0,
				resourcePath.length() - ClassUtils.CLASS_FILE_SUFFIX.length());
		return ClassUtils.convertResourcePathToClassName(pathWithoutSuffix);
	}


	@SuppressWarnings("serial")
	private static class LocalResourceCache extends LinkedHashMap<Resource, MetadataReader> {

		private volatile int cacheLimit;

		public LocalResourceCache(int cacheLimit) {
			super(cacheLimit, 0.75f, true);
			this.cacheLimit = cacheLimit;
		}

		public void setCacheLimit(int cacheLimit) {
			this.cacheLimit = cacheLimit;
		}

		public int getCacheLimit() {
			return this.cacheLimit;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<Resource, MetadataReader> eldest) {
			return size() > this.cacheLimit;
		}
	}

	@SuppressWarnings("serial")
	private static class LocalClassNameCache extends LinkedHashMap<String, MetadataReader> {

		private volatile int cacheLimit;

		public LocalClassNameCache(int cacheLimit) {
			super(cacheLimit, 0.75f, true);
			this.cacheLimit = cacheLimit;
		}

		public void setCacheLimit(int cacheLimit) {
			this.cacheLimit = cacheLimit;
		}

		public int getCacheLimit() {
			return this.cacheLimit;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, MetadataReader> eldest) {
			return size() > this.cacheLimit;
		}
	}

}
