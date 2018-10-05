/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.core.io.support;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Memoized Implementation for {@link PathMatchingResourcePatternResolver} that caches the jar entries to avoid lookup costs
 * for subsequent lookups in the same jar.
 *
 * This comes in handy for uber jars where every lookup incurs fixed costs thereby contributing to Spring:refresh and hence
 * higher startup costs.
 *
 * @author Rahul Shinde
 */
public class MemoizedPathMatchingResourcePatternResolver extends PathMatchingResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(MemoizedPathMatchingResourcePatternResolver.class);

	/**
	 * Maintains a cache of the entries in the jar to avoid I/O costs for subsequent costs.
	 * Key = JarFileName, Values = Entries in the Jar
	 * Entries are kept in sorted order to allow binary search for a given prefix.
 	 */
	private final Map<String, List<String>> jarEntriesCache = new ConcurrentHashMap<>();

	/**
	 * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
	 * <p>ClassLoader access will happen via the thread context class loader.
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public MemoizedPathMatchingResourcePatternResolver() {
		super();
	}

	/**
	 * Create a new PathMatchingResourcePatternResolver.
	 * <p>ClassLoader access will happen via the thread context class loader.
	 * @param resourceLoader the ResourceLoader to load root directories and
	 * actual resources with
	 */
	public MemoizedPathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	/**
	 * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
	 * <p>ClassLoader access will happen via the thread context class loader.
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public MemoizedPathMatchingResourcePatternResolver(@Nullable ClassLoader classLoader) {
		super(classLoader);
	}

	/**
	 * Find all resources in jar files that match the given location pattern while building the cache for
	 * first lookup and using it for subsequent lookups.
	 *
	 * This could have been shortened if the parent implementation wraps parts of doFindPathMatchingJarResources
	 * in protected method.
	 *
	 * @param rootDirResource the root directory as Resource
	 * @param rootDirURL the pre-resolved root directory URL
	 * @param subPattern the sub pattern to match (below the root directory)
	 * @return a mutable Set of matching Resource instances
	 * @throws IOException in case of I/O errors
	 */
	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirURL, String subPattern)
			throws IOException {

		URLConnection con = rootDirURL.openConnection();
		JarFile jarFile;
		String jarFileUrl;
		String rootEntryPath;
		boolean closeJarFile;

		if (con instanceof JarURLConnection) {
			// Should usually be the case for traditional JAR files.
			JarURLConnection jarCon = (JarURLConnection) con;
			ResourceUtils.useCachesIfNecessary(jarCon);
			jarFile = jarCon.getJarFile();
			jarFileUrl = jarCon.getJarFileURL().toExternalForm();
			JarEntry jarEntry = jarCon.getJarEntry();
			rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
			closeJarFile = !jarCon.getUseCaches();
		}
		else {
			// No JarURLConnection -> need to resort to URL file parsing.
			// We'll assume URLs of the format "jar:path!/entry", with the protocol
			// being arbitrary as long as following the entry format.
			// We'll also handle paths with and without leading "file:" prefix.
			String urlFile = rootDirURL.getFile();
			try {
				int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
				if (separatorIndex == -1) {
					separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
				}
				if (separatorIndex != -1) {
					jarFileUrl = urlFile.substring(0, separatorIndex);
					rootEntryPath = urlFile.substring(separatorIndex + 2);  // both separators are 2 chars
					jarFile = getJarFile(jarFileUrl);
				}
				else {
					jarFile = new JarFile(urlFile);
					jarFileUrl = urlFile;
					rootEntryPath = "";
				}
				closeJarFile = true;
			}
			catch (ZipException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping invalid jar classpath entry [" + urlFile + "]");
				}
				return Collections.emptySet();
			}
		}

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Looking for matching resources in jar file [" + jarFileUrl + "]");
			}
			if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// Root entry path must end with slash to allow for proper matching.
				// The Sun JRE does not return a slash here, but BEA JRockit does.
				rootEntryPath = rootEntryPath + "/";
			}
			Set<Resource> result = new LinkedHashSet<>(8);
			// Check if cache already exist for this jarFile.
			List<String> jarCache = this.jarEntriesCache.computeIfAbsent(jarFile.getName(), v -> new ArrayList<>());
			if (jarCache.isEmpty()) {
				for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
					JarEntry entry = entries.nextElement();
					String entryPath = entry.getName();
					jarCache.add(entryPath);
					if (entryPath.startsWith(rootEntryPath)) {
						String relativePath = entryPath.substring(rootEntryPath.length());
						if (getPathMatcher().match(subPattern, relativePath)) {
							result.add(rootDirResource.createRelative(relativePath));
						}
					}
				}
				// Add entries in sorted order to allow binary search
				Collections.sort(jarCache);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Looking for matching resources using cache [" + jarFileUrl + "]");
				}
				// Binary search over the cache contents to see if the entry exists.
				int index = Collections.binarySearch(jarCache, rootEntryPath);
				if (index < 0) {
					index = -(index + 1);
				}
				while (index < jarCache.size()) {
					String entryPath = jarCache.get(index);
					if (entryPath.startsWith(rootEntryPath)) {
						String relativePath = entryPath.substring(rootEntryPath.length());
						if (getPathMatcher().match(subPattern, relativePath)) {
							result.add(rootDirResource.createRelative(relativePath));
						}
					}
					else {
						// Break as the prefix doesnt match anymore
						break;
					}
					index++;
				}
			}
			return result;
		}
		finally {
			if (closeJarFile) {
				jarFile.close();
			}
		}
	}

	/**
	 * Gets the jar file names in the cache. Useful for testing purpose.
	 *
	 * @return set of jar file names
	 */
	Set<String> getCachedKeys() {
		return this.jarEntriesCache.keySet();
	}

	/**
	 * Returns the cached entries if any. Useful for testing purpose.
	 *
	 * @param jarFileName jar filename
	 * @return cached entries or null if cache was not built
	 */
	Collection<String> getCachedEntries(String jarFileName) {
		return this.jarEntriesCache.get(jarFileName);
	}

	/**
	 * Application is responsible for calling this method after {@code ApplicationContext} is initialized.
	 */
	public void clear() {
		this.jarEntriesCache.clear();
	}
}
