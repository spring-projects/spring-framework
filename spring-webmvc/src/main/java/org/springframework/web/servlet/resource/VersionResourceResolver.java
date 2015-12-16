/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * Resolves request paths containing a version string that can be used as part
 * of an HTTP caching strategy in which a resource is cached with a date in the
 * distant future (e.g. 1 year) and cached until the version, and therefore the
 * URL, is changed.
 *
 * <p>Different versioning strategies exist, and this resolver must be configured
 * with one or more such strategies along with path mappings to indicate which
 * strategy applies to which resources.
 *
 * <p>{@code ContentVersionStrategy} is a good default choice except in cases
 * where it cannot be used. Most notably the {@code ContentVersionStrategy}
 * cannot be combined with JavaScript module loaders. For such cases the
 * {@code FixedVersionStrategy} is a better choice.
 *
 * <p>Note that using this resolver to serve CSS files means that the
 * {@link CssLinkResourceTransformer} should also be used in order to modify
 * links within CSS files to also contain the appropriate versions generated
 * by this resolver.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see VersionStrategy
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	public static final String RESOURCE_VERSION_ATTRIBUTE = VersionResourceResolver.class.getName() + ".resourceVersion";

	private AntPathMatcher pathMatcher = new AntPathMatcher();

	/** Map from path pattern -> VersionStrategy */
	private final Map<String, VersionStrategy> versionStrategyMap = new LinkedHashMap<String, VersionStrategy>();


	/**
	 * Set a Map with URL paths as keys and {@code VersionStrategy} as values.
	 * <p>Supports direct URL matches and Ant-style pattern matches. For syntax
	 * details, see the {@link org.springframework.util.AntPathMatcher} javadoc.
	 * @param map map with URLs as keys and version strategies as values
	 */
	public void setStrategyMap(Map<String, VersionStrategy> map) {
		this.versionStrategyMap.clear();
		this.versionStrategyMap.putAll(map);
	}

	/**
	 * Return the map with version strategies keyed by path pattern.
	 */
	public Map<String, VersionStrategy> getStrategyMap() {
		return this.versionStrategyMap;
	}

	/**
	 * Insert a content-based version in resource URLs that match the given path
	 * patterns. The version is computed from the content of the file, e.g.
	 * {@code "css/main-e36d2e05253c6c7085a91522ce43a0b4.css"}. This is a good
	 * default strategy to use except when it cannot be, for example when using
	 * JavaScript module loaders, use {@link #addFixedVersionStrategy} instead
	 * for serving JavaScript files.
	 * @param pathPatterns one or more resource URL path patterns
	 * @return the current instance for chained method invocation
	 * @see ContentVersionStrategy
	 */
	public VersionResourceResolver addContentVersionStrategy(String... pathPatterns) {
		addVersionStrategy(new ContentVersionStrategy(), pathPatterns);
		return this;
	}

	/**
	 * Insert a fixed, prefix-based version in resource URLs that match the given
	 * path patterns, for example: <code>"{version}/js/main.js"</code>. This is useful (vs.
	 * content-based versions) when using JavaScript module loaders.
	 * <p>The version may be a random number, the current date, or a value
	 * fetched from a git commit sha, a property file, or environment variable
	 * and set with SpEL expressions in the configuration (e.g. see {@code @Value}
	 * in Java config).
	 * @param version a version string
	 * @param pathPatterns one or more resource URL path patterns
	 * @return the current instance for chained method invocation
	 * @see FixedVersionStrategy
	 */
	public VersionResourceResolver addFixedVersionStrategy(String version, String... pathPatterns) {
		addVersionStrategy(new FixedVersionStrategy(version), pathPatterns);
		return this;
	}

	/**
	 * Register a custom VersionStrategy to apply to resource URLs that match the
	 * given path patterns.
	 * @param strategy the custom strategy
	 * @param pathPatterns one or more resource URL path patterns
	 * @return the current instance for chained method invocation
	 * @see VersionStrategy
	 */
	public VersionResourceResolver addVersionStrategy(VersionStrategy strategy, String... pathPatterns) {
		for (String pattern : pathPatterns) {
			getStrategyMap().put(pattern, strategy);
		}
		return this;
	}


	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved != null) {
			return resolved;
		}

		VersionStrategy versionStrategy = getStrategyForPath(requestPath);
		if (versionStrategy == null) {
			return null;
		}

		String candidateVersion = versionStrategy.extractVersion(requestPath);
		if (StringUtils.isEmpty(candidateVersion)) {
			if (logger.isTraceEnabled()) {
				logger.trace("No version found in path=\"" + requestPath + "\"");
			}
			return null;
		}

		String simplePath = versionStrategy.removeVersion(requestPath, candidateVersion);
		if (logger.isTraceEnabled()) {
			logger.trace("Extracted version from path, re-resolving without version, path=\"" + simplePath + "\"");
		}

		Resource baseResource = chain.resolveResource(request, simplePath, locations);
		if (baseResource == null) {
			return null;
		}

		String actualVersion = versionStrategy.getResourceVersion(baseResource);
		if (candidateVersion.equals(actualVersion)) {
			if (logger.isTraceEnabled()) {
				logger.trace("resource matches extracted version");
			}
			if (request != null) {
				request.setAttribute(VersionResourceResolver.RESOURCE_VERSION_ATTRIBUTE, candidateVersion);
			}
			return baseResource;
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Potential resource found for [" + requestPath + "], but version [" +
						candidateVersion + "] doesn't match.");
			}
			return null;
		}
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
		String baseUrl = chain.resolveUrlPath(resourceUrlPath, locations);
		if (StringUtils.hasText(baseUrl)) {
			VersionStrategy versionStrategy = getStrategyForPath(resourceUrlPath);
			if (versionStrategy == null) {
				return null;
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Getting the original resource to determine version");
			}
			Resource resource = chain.resolveResource(null, baseUrl, locations);
			String version = versionStrategy.getResourceVersion(resource);
			if (logger.isTraceEnabled()) {
				logger.trace("Version=" + version);
			}
			return versionStrategy.addVersion(baseUrl, version);
		}
		return baseUrl;
	}

	/**
	 * Find a {@code VersionStrategy} for the request path of the requested resource.
	 * @return an instance of a {@code VersionStrategy} or null if none matches that request path
	 */
	protected VersionStrategy getStrategyForPath(String requestPath) {
		String path = "/".concat(requestPath);
		List<String> matchingPatterns = new ArrayList<String>();
		for (String pattern : this.versionStrategyMap.keySet()) {
			if (this.pathMatcher.match(pattern, path)) {
				matchingPatterns.add(pattern);
			}
		}
		if (!matchingPatterns.isEmpty()) {
			Comparator<String> comparator = this.pathMatcher.getPatternComparator(path);
			Collections.sort(matchingPatterns, comparator);
			return this.versionStrategyMap.get(matchingPatterns.get(0));
		}
		return null;
	}

}
