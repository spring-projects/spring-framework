/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.cors;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@code CorsConfigurationSource} that uses URL path patterns to select the
 * {@code CorsConfiguration} for a request.
 *
 * <p>Pattern matching can be done with a {@link PathMatcher} or with pre-parsed
 * {@link PathPattern}s. The syntax is largely the same with the latter being more
 * tailored for web usage and more efficient. The choice depends on the presence of a
 * {@link UrlPathHelper#resolveAndCacheLookupPath resolved} String lookupPath or a
 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
 * with a fallback on {@link PathMatcher} but the fallback can be disabled.
 * For more details, please see {@link #setAllowInitLookupPath(boolean)}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see PathPattern
 * @see AntPathMatcher
 */
public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource {

	private static PathMatcher defaultPathMatcher = new AntPathMatcher();


	private final PathPatternParser patternParser;

	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;

	private PathMatcher pathMatcher = defaultPathMatcher;

	@Nullable
	private String lookupPathAttributeName;

	private boolean allowInitLookupPath = true;

	private final Map<PathPattern, CorsConfiguration> corsConfigurations = new LinkedHashMap<>();


	/**
	 * Default constructor with {@link PathPatternParser#defaultInstance}.
	 */
	public UrlBasedCorsConfigurationSource() {
		this(PathPatternParser.defaultInstance);
	}

	/**
	 * Constructor with a {@link PathPatternParser} to parse patterns with.
	 * @param parser the parser to use
	 * @since 5.3
	 */
	public UrlBasedCorsConfigurationSource(PathPatternParser parser) {
		Assert.notNull(parser, "PathPatternParser must not be null");
		this.patternParser = parser;
	}


	/**
	 * Shortcut to the
	 * {@link org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 * same property} on the configured {@code UrlPathHelper}.
	 * @deprecated as of 5.3 in favor of using
	 * {@link #setUrlPathHelper(UrlPathHelper)}, if at all. For further details,
	 * please see {@link #setAllowInitLookupPath(boolean)}.
	 */
	@Deprecated
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		initUrlPathHelper();
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Shortcut to the
	 * {@link org.springframework.web.util.UrlPathHelper#setUrlDecode same property}
	 * on the configured {@code UrlPathHelper}.
	 * @deprecated as of 5.3 in favor of using
	 * {@link #setUrlPathHelper(UrlPathHelper)}, if at all. For further details,
	 * please see {@link #setAllowInitLookupPath(boolean)}.
	 */
	@Deprecated
	public void setUrlDecode(boolean urlDecode) {
		initUrlPathHelper();
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Shortcut to the
	 * {@link org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent
	 * same property} on the configured {@code UrlPathHelper}.
	 * @deprecated as of 5.3 in favor of using
	 * {@link #setUrlPathHelper(UrlPathHelper)}, if at all. For further details,
	 * please see {@link #setAllowInitLookupPath(boolean)}.
	 */
	@Deprecated
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		initUrlPathHelper();
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	private void initUrlPathHelper() {
		if (this.urlPathHelper == UrlPathHelper.defaultInstance) {
			this.urlPathHelper = new UrlPathHelper();
		}
	}

	/**
	 * Configure the {@code UrlPathHelper} to resolve the lookupPath. This may
	 * not be necessary if the lookupPath is expected to be pre-resolved or if
	 * parsed {@code PathPatterns} are used instead.
	 * For further details on that, see {@link #setAllowInitLookupPath(boolean)}.
	 * <p>By default this is {@link UrlPathHelper#defaultInstance}.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * When enabled, if there is neither a
	 * {@link UrlPathHelper#resolveAndCacheLookupPath esolved} String lookupPath nor a
	 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
	 * then use the {@link #setUrlPathHelper configured} {@code UrlPathHelper}
	 * to resolve a String lookupPath. This in turn determines use of URL
	 * pattern matching with {@link PathMatcher} or with parsed {@link PathPattern}s.
	 * <p>In Spring MVC, either a resolved String lookupPath or a parsed
	 * {@code RequestPath} is always available within {@code DispatcherServlet}
	 * processing. However in a Servlet {@code Filter} such as {@code CorsFilter}
	 * that may or may not be the case.
	 * <p>By default this is set to {@code true} in which case lazy lookupPath
	 * initialization is allowed. Set this to {@code false} when an
	 * application is using parsed {@code PathPatterns} in which case the
	 * {@code RequestPath} can be parsed earlier via
	 * {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter}.
	 * @param allowInitLookupPath whether to disable lazy initialization
	 * and fail if not already resolved
	 * @since 5.3
	 */
	public void setAllowInitLookupPath(boolean allowInitLookupPath) {
		this.allowInitLookupPath = allowInitLookupPath;
	}

	/**
	 * Configure the name of the attribute that holds the lookupPath extracted
	 * via {@link UrlPathHelper#getLookupPathForRequest(HttpServletRequest)}.
	 * <p>By default this is {@link UrlPathHelper#PATH_ATTRIBUTE}.
	 * @param name the request attribute to check
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of {@link UrlPathHelper#PATH_ATTRIBUTE}.
	 */
	@Deprecated
	public void setLookupPathAttributeName(String name) {
		this.lookupPathAttributeName = name;
	}

	/**
	 * Configure a {@code PathMatcher} to use for pattern matching.
	 * <p>This is an advanced property that should be used only when a
	 * customized {@link AntPathMatcher} or a custom PathMatcher is required.
	 * <p>By default this is {@link AntPathMatcher}.
	 * <p><strong>Note:</strong> Setting {@code PathMatcher} enforces use of
	 * String pattern matching even when a
	 * {@link ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
	 * is available.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Set the CORS configuration mappings.
	 * <p>For pattern syntax see {@link AntPathMatcher} and {@link PathPattern}
	 * as well as class-level Javadoc for details on which one may in use.
	 * Generally the syntax is largely the same with {@link PathPattern} more
	 * tailored for web usage.
	 * @param corsConfigurations the mappings to use
	 * @see PathPattern
	 * @see AntPathMatcher
	 */
	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> corsConfigurations) {
		this.corsConfigurations.clear();
		if (corsConfigurations != null) {
			corsConfigurations.forEach(this::registerCorsConfiguration);
		}
	}

	/**
	 * Variant of {@link #setCorsConfigurations(Map)} to register one mapping at a time.
	 * @param pattern the mapping pattern
	 * @param config the CORS configuration to use for the pattern
	 * @see PathPattern
	 * @see AntPathMatcher
	 */
	public void registerCorsConfiguration(String pattern, CorsConfiguration config) {
		this.corsConfigurations.put(this.patternParser.parse(pattern), config);
	}

	/**
	 * Return all configured CORS mappings.
	 */
	public Map<String, CorsConfiguration> getCorsConfigurations() {
		Map<String, CorsConfiguration> result = new HashMap<>(this.corsConfigurations.size());
		this.corsConfigurations.forEach((pattern, config) -> result.put(pattern.getPatternString(), config));
		return Collections.unmodifiableMap(result);
	}


	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		Object path = resolvePath(request);
		boolean isPathContainer = (path instanceof PathContainer);
		for (Map.Entry<PathPattern, CorsConfiguration> entry : this.corsConfigurations.entrySet()) {
			if (match(path, isPathContainer, entry.getKey())) {
				return entry.getValue();
			}
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	private Object resolvePath(HttpServletRequest request) {
		if (this.allowInitLookupPath && !ServletRequestPathUtils.hasCachedPath(request)) {
			return (this.lookupPathAttributeName != null ?
					this.urlPathHelper.getLookupPathForRequest(request, this.lookupPathAttributeName) :
					this.urlPathHelper.getLookupPathForRequest(request));
		}
		Object lookupPath = ServletRequestPathUtils.getCachedPath(request);
		if (this.pathMatcher != defaultPathMatcher) {
			lookupPath = lookupPath.toString();
		}
		return lookupPath;
	}

	private boolean match(Object path, boolean isPathContainer, PathPattern pattern) {
		return (isPathContainer ?
				pattern.matches((PathContainer) path) :
				this.pathMatcher.match(pattern.getPatternString(), (String) path));
	}

}
