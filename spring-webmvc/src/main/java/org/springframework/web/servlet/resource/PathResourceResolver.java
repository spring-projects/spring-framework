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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A simple {@code ResourceResolver} that tries to find a resource under the given
 * locations matching to the request path.
 *
 * <p>This resolver does not delegate to the {@code ResourceResolverChain} and is
 * expected to be configured at the end in a chain of resolvers.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class PathResourceResolver extends AbstractResourceResolver {

	private Resource @Nullable [] allowedLocations;

	private final Map<Resource, Charset> locationCharsets = new HashMap<>(4);

	private @Nullable UrlPathHelper urlPathHelper;


	/**
	 * By default, when a Resource is found, the path of the resolved resource is
	 * compared to ensure it's under the input location where it was found.
	 * However sometimes that may not be the case, for example, when
	 * {@link org.springframework.web.servlet.resource.CssLinkResourceTransformer}
	 * resolves public URLs of links it contains, the CSS file is the location
	 * and the resources being resolved are css files, images, fonts and others
	 * located in adjacent or parent directories.
	 * <p>This property allows configuring a complete list of locations under
	 * which resources must be so that if a resource is not under the location
	 * relative to which it was found, this list may be checked as well.
	 * <p>By default {@link ResourceHttpRequestHandler} initializes this property
	 * to match its list of locations.
	 * @param locations the list of allowed locations
	 * @since 4.1.2
	 * @see ResourceHttpRequestHandler#initAllowedLocations()
	 */
	public void setAllowedLocations(Resource @Nullable ... locations) {
		this.allowedLocations = locations;
	}

	public Resource @Nullable [] getAllowedLocations() {
		return this.allowedLocations;
	}

	/**
	 * Configure charsets associated with locations. If a static resource is found
	 * under a {@link org.springframework.core.io.UrlResource URL resource}
	 * location the charset is used to encode the relative path
	 * <p><strong>Note:</strong> the charset is used only if the
	 * {@link #setUrlPathHelper urlPathHelper} property is also configured and
	 * its {@code urlDecode} property is set to true.
	 * @since 4.3.13
	 */
	public void setLocationCharsets(Map<Resource, Charset> locationCharsets) {
		this.locationCharsets.clear();
		this.locationCharsets.putAll(locationCharsets);
	}

	/**
	 * Return charsets associated with static resource locations.
	 * @since 4.3.13
	 */
	public Map<Resource, Charset> getLocationCharsets() {
		return Collections.unmodifiableMap(this.locationCharsets);
	}

	/**
	 * Provide a reference to the {@link UrlPathHelper} used to map requests to
	 * static resources. This helps to derive information about the lookup path
	 * such as whether it is decoded or not.
	 * @since 4.3.13
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
	 * for use at runtime in web modules in favor of parsed patterns with
	 * {@link PathPatternParser}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * The configured {@link UrlPathHelper}.
	 * @since 4.3.13
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
	 * for use at runtime in web modules in favor of parsed patterns with
	 * {@link PathPatternParser}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public @Nullable UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	@Override
	protected @Nullable Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, request, locations);
	}

	@Override
	protected @Nullable String resolveUrlPathInternal(String resourcePath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return (StringUtils.hasText(resourcePath) &&
				getResource(resourcePath, null, locations) != null ? resourcePath : null);
	}

	private @Nullable Resource getResource(String resourcePath, @Nullable HttpServletRequest request,
			List<? extends Resource> locations) {

		for (Resource location : locations) {
			try {
				String pathToUse = encodeOrDecodeIfNecessary(resourcePath, request, location);
				Resource resource = getResource(pathToUse, location);
				if (resource != null) {
					return resource;
				}
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					String error = "Skip location [" + location + "] due to error";
					if (logger.isTraceEnabled()) {
						logger.trace(error, ex);
					}
					else {
						logger.debug(error + ": " + ex.getMessage());
					}
				}
			}
		}
		return null;
	}

	/**
	 * Find the resource under the given location.
	 * <p>The default implementation checks if there is a readable
	 * {@code Resource} for the given path relative to the location.
	 * @param resourcePath the path to the resource
	 * @param location the location to check
	 * @return the resource, or {@code null} if none found
	 */
	protected @Nullable Resource getResource(String resourcePath, Resource location) throws IOException {
		Resource resource = location.createRelative(resourcePath);
		if (resource.isReadable()) {
			if (checkResource(resource, location)) {
				return resource;
			}
			else if (logger.isWarnEnabled()) {
				Resource[] allowed = getAllowedLocations();
				logger.warn(LogFormatUtils.formatValue(
						"Resource path \"" + resourcePath + "\" was successfully resolved " +
								"but resource \"" + resource + "\" is neither under " +
								"the current location \"" + location + "\" nor under any of " +
								"the allowed locations " + (allowed != null ? Arrays.asList(allowed) : "[]"), -1, true));
			}
		}
		return null;
	}

	/**
	 * Perform additional checks on a resolved resource beyond checking whether the
	 * resource exists and is readable. The default implementation also verifies
	 * the resource is either under the location relative to which it was found or
	 * is under one of the {@linkplain #setAllowedLocations allowed locations}.
	 * @param resource the resource to check
	 * @param location the location relative to which the resource was found
	 * @return "true" if resource is in a valid location, "false" otherwise
	 * @since 4.1.2
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		if (ResourceHandlerUtils.isResourceUnderLocation(location, resource)) {
			return true;
		}
		Resource[] allowedLocations = getAllowedLocations();
		if (allowedLocations != null) {
			for (Resource current : allowedLocations) {
				if (ResourceHandlerUtils.isResourceUnderLocation(current, resource)) {
					return true;
				}
			}
		}
		return false;
	}

	private String encodeOrDecodeIfNecessary(String path, @Nullable HttpServletRequest request, Resource location) {
		if (request != null) {
			boolean usesPathPattern = (
					ServletRequestPathUtils.hasCachedPath(request) &&
					ServletRequestPathUtils.getCachedPath(request) instanceof PathContainer);

			if (shouldDecodeRelativePath(location, usesPathPattern)) {
				return UriUtils.decode(path, StandardCharsets.UTF_8);
			}
			else if (shouldEncodeRelativePath(location, usesPathPattern)) {
				Charset charset = this.locationCharsets.getOrDefault(location, StandardCharsets.UTF_8);
				StringBuilder sb = new StringBuilder();
				StringTokenizer tokenizer = new StringTokenizer(path, "/");
				while (tokenizer.hasMoreTokens()) {
					String value = UriUtils.encode(tokenizer.nextToken(), charset);
					sb.append(value);
					sb.append('/');
				}
				if (!path.endsWith("/")) {
					sb.setLength(sb.length() - 1);
				}
				return sb.toString();
			}
		}
		return path;
	}

	/**
	 * When the {@code HandlerMapping} is set to not decode the URL path, the
	 * path needs to be decoded for non-{@code UrlResource} locations.
	 */
	private boolean shouldDecodeRelativePath(Resource location, boolean usesPathPattern) {
		return (!(location instanceof UrlResource) &&
				(usesPathPattern || (this.urlPathHelper != null && !this.urlPathHelper.isUrlDecode())));
	}

	/**
	 * When the {@code HandlerMapping} is set to decode the URL path, the path
	 * needs to be encoded for {@code UrlResource} locations.
	 */
	private boolean shouldEncodeRelativePath(Resource location, boolean usesPathPattern) {
		return (location instanceof UrlResource && !usesPathPattern &&
				this.urlPathHelper != null && this.urlPathHelper.isUrlDecode());
	}

}
