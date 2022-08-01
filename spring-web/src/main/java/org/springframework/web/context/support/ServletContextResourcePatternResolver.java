/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * ServletContext-aware subclass of {@link PathMatchingResourcePatternResolver},
 * able to find matching resources below the web application root directory
 * via {@link ServletContext#getResourcePaths}. Falls back to the superclass'
 * file system checking for other resources.
 *
 * @author Juergen Hoeller
 * @since 1.1.2
 */
public class ServletContextResourcePatternResolver extends PathMatchingResourcePatternResolver {

	private static final Log logger = LogFactory.getLog(ServletContextResourcePatternResolver.class);


	/**
	 * Create a new ServletContextResourcePatternResolver.
	 * @param servletContext the ServletContext to load resources with
	 * @see ServletContextResourceLoader#ServletContextResourceLoader(jakarta.servlet.ServletContext)
	 */
	public ServletContextResourcePatternResolver(ServletContext servletContext) {
		super(new ServletContextResourceLoader(servletContext));
	}

	/**
	 * Create a new ServletContextResourcePatternResolver.
	 * @param resourceLoader the ResourceLoader to load root directories and
	 * actual resources with
	 */
	public ServletContextResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}


	/**
	 * Overridden version which checks for ServletContextResource
	 * and uses {@code ServletContext.getResourcePaths} to find
	 * matching resources below the web application root directory.
	 * In case of other resources, delegates to the superclass version.
	 * @see #doRetrieveMatchingServletContextResources
	 * @see ServletContextResource
	 * @see jakarta.servlet.ServletContext#getResourcePaths
	 */
	@Override
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		if (rootDirResource instanceof ServletContextResource scResource) {
			ServletContext sc = scResource.getServletContext();
			String fullPattern = scResource.getPath() + subPattern;
			Set<Resource> result = new LinkedHashSet<>(8);
			doRetrieveMatchingServletContextResources(sc, fullPattern, scResource.getPath(), result);
			return result;
		}
		else {
			return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
		}
	}

	/**
	 * Recursively retrieve ServletContextResources that match the given pattern,
	 * adding them to the given result set.
	 * @param servletContext the ServletContext to work on
	 * @param fullPattern the pattern to match against,
	 * with prepended root directory path
	 * @param dir the current directory
	 * @param result the Set of matching Resources to add to
	 * @throws IOException if directory contents could not be retrieved
	 * @see ServletContextResource
	 * @see jakarta.servlet.ServletContext#getResourcePaths
	 */
	protected void doRetrieveMatchingServletContextResources(
			ServletContext servletContext, String fullPattern, String dir, Set<Resource> result)
			throws IOException {

		Set<String> candidates = servletContext.getResourcePaths(dir);
		if (candidates != null) {
			boolean dirDepthNotFixed = fullPattern.contains("**");
			int jarFileSep = fullPattern.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
			String jarFilePath = null;
			String pathInJarFile = null;
			if (jarFileSep > 0 && jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length() < fullPattern.length()) {
				jarFilePath = fullPattern.substring(0, jarFileSep);
				pathInJarFile = fullPattern.substring(jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length());
			}
			for (String currPath : candidates) {
				if (!currPath.startsWith(dir)) {
					// Returned resource path does not start with relative directory:
					// assuming absolute path returned -> strip absolute path.
					int dirIndex = currPath.indexOf(dir);
					if (dirIndex != -1) {
						currPath = currPath.substring(dirIndex);
					}
				}
				if (currPath.endsWith("/") && (dirDepthNotFixed || StringUtils.countOccurrencesOf(currPath, "/") <=
						StringUtils.countOccurrencesOf(fullPattern, "/"))) {
					// Search subdirectories recursively: ServletContext.getResourcePaths
					// only returns entries for one directory level.
					doRetrieveMatchingServletContextResources(servletContext, fullPattern, currPath, result);
				}
				if (jarFilePath != null && getPathMatcher().match(jarFilePath, currPath)) {
					// Base pattern matches a jar file - search for matching entries within.
					String absoluteJarPath = servletContext.getRealPath(currPath);
					if (absoluteJarPath != null) {
						doRetrieveMatchingJarEntries(absoluteJarPath, pathInJarFile, result);
					}
				}
				if (getPathMatcher().match(fullPattern, currPath)) {
					result.add(new ServletContextResource(servletContext, currPath));
				}
			}
		}
	}

	/**
	 * Extract entries from the given jar by pattern.
	 * @param jarFilePath the path to the jar file
	 * @param entryPattern the pattern for jar entries to match
	 * @param result the Set of matching Resources to add to
	 */
	private void doRetrieveMatchingJarEntries(String jarFilePath, String entryPattern, Set<Resource> result) {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching jar file [" + jarFilePath + "] for entries matching [" + entryPattern + "]");
		}
		try (JarFile jarFile = new JarFile(jarFilePath)) {
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (getPathMatcher().match(entryPattern, entryPath)) {
					result.add(new UrlResource(
							ResourceUtils.URL_PROTOCOL_JAR,
							ResourceUtils.FILE_URL_PREFIX + jarFilePath + ResourceUtils.JAR_URL_SEPARATOR + entryPath));
				}
			}
		}
		catch (IOException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching resources in jar file [" + jarFilePath +
						"] because the jar cannot be opened through the file system", ex);
			}
		}
	}

}
