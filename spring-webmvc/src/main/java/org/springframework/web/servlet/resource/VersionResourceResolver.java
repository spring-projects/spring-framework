/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.servlet.resource;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * A {@code ResourceResolver} that resolves request paths containing a version
 * string, i.e. version information about the resource being requested.
 * This resolver can be useful to set up HTTP caching strategies by changing
 * resources' URLs as they are updated.
 *
 * <p>Because resource versioning depends on the resource types, this {@code ResourceResolver}
 * needs to be configured with at least one {@link VersionStrategy}. The process of matching
 * and generating version strings is delegated to the {@code VersionStrategy}.
 *
 * <p>When resolving resources, this resolver will first delegate to the chain to locate
 * an existing resource and then attempt to extract a version string from the request path
 * and then find a resource that matches that version.
 *
 * <p>When resolving URLs, this resolver will, if necessary, add a version string in the
 * request path.
 *
 * @author Brian Clozel
 * @since 4.1
 * @see VersionStrategy
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	private AntPathMatcher pathMatcher = new AntPathMatcher();

	private Map<String, VersionStrategy> versionStrategyMap = Collections.emptyMap();

	/**
	 * Set a Map with URL paths as keys and {@code VersionStrategy}
	 * as values.
	 *
	 * <p>Supports direct URL matches and Ant-style pattern matches. For syntax
	 * details, see the {@link org.springframework.util.AntPathMatcher} javadoc.
	 *
	 * @param versionStrategyMap map with URLs as keys and version strategies as values
	 */
	public void setVersionStrategyMap(Map<String, VersionStrategy> versionStrategyMap) {
		this.versionStrategyMap = versionStrategyMap;
	}

	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved != null) {
			return resolved;
		}

		VersionStrategy versionStrategy = findStrategy(requestPath);
		if(versionStrategy == null) {
			return null;
		}

		String candidateVersion = versionStrategy.extractVersionFromPath(requestPath);
		if (StringUtils.isEmpty(candidateVersion)) {
			if (logger.isTraceEnabled()) {
				logger.trace("No version found in path=\"" + requestPath + "\"");
			}
			return null;
		}

		String simplePath = versionStrategy.deleteVersionFromPath(requestPath, candidateVersion);

		if (logger.isTraceEnabled()) {
			logger.trace("Extracted version from path, re-resolving without version, path=\"" + simplePath + "\"");
		}

		Resource baseResource = chain.resolveResource(request, simplePath, locations);
		if (baseResource == null) {
			return null;
		}

		if (versionStrategy.resourceVersionMatches(baseResource, candidateVersion)) {
			if (logger.isTraceEnabled()) {
				logger.trace("resource matches extracted version");
			}
			return baseResource;
		}
		else {
			logger.trace("Potential resource found for [" + requestPath + "], but version ["
					+ candidateVersion + "] doesn't match.");
			return null;
		}
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
		String baseUrl = chain.resolveUrlPath(resourceUrlPath, locations);
		if (StringUtils.hasText(baseUrl)) {
			VersionStrategy versionStrategy = findStrategy(resourceUrlPath);
			if(versionStrategy == null) {
				return null;
			}
			return versionStrategy.addVersionToUrl(baseUrl, locations, chain);
		}
		return baseUrl;
	}

	/**
	 * Finds a {@code VersionStrategy} for the request path of the requested resource.
	 * @return an instance of a {@code VersionStrategy} or null if none matches that request path
	 */
	protected VersionStrategy findStrategy(String requestPath) {
		String path = "/".concat(requestPath);
        List<String> matchingPatterns = new ArrayList<String>();
		for(String pattern : this.versionStrategyMap.keySet()) {
			if(this.pathMatcher.match(pattern, path)) {
                matchingPatterns.add(pattern);
			}
		}
        if(!matchingPatterns.isEmpty()) {
            Comparator<String> comparator = this.pathMatcher.getPatternComparator(path);
            Collections.sort(matchingPatterns, comparator);
            return this.versionStrategyMap.get(matchingPatterns.get(0));
        }

		return null;
	}

}
