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

package org.springframework.web.reactive.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves request paths containing a version string that can be used as part
 * of an HTTP caching strategy in which a resource is cached with a date in the
 * distant future (for example, 1 year) and cached until the version, and therefore the
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
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see VersionStrategy
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/** Map from path pattern -> VersionStrategy. */
	private final Map<String, VersionStrategy> versionStrategyMap = new LinkedHashMap<>();


	/**
	 * Set a Map with URL paths as keys and {@code VersionStrategy} as values.
	 * <p>Supports direct URL matches and Ant-style pattern matches. For syntax
	 * details, see the {@link AntPathMatcher} javadoc.
	 * @param map a map with URLs as keys and version strategies as values
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
	 * patterns. The version is computed from the content of the file, for example,
	 * {@code "css/main-e36d2e05253c6c7085a91522ce43a0b4.css"}. This is a good
	 * default strategy to use except when it cannot be, for example when using
	 * JavaScript module loaders, use {@link #addFixedVersionStrategy} instead
	 * for serving JavaScript files.
	 * @param pathPatterns one or more resource URL path patterns,
	 * relative to the pattern configured with the resource handler
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
	 * and set with SpEL expressions in the configuration (for example, see {@code @Value}
	 * in Java config).
	 * <p>If not done already, variants of the given {@code pathPatterns}, prefixed with
	 * the {@code version} will be also configured. For example, adding a {@code "/js/**"} path pattern
	 * will also configure automatically a {@code "/v1.0.0/js/**"} with {@code "v1.0.0"} the
	 * {@code version} String given as an argument.
	 * @param version a version string
	 * @param pathPatterns one or more resource URL path patterns,
	 * relative to the pattern configured with the resource handler
	 * @return the current instance for chained method invocation
	 * @see FixedVersionStrategy
	 */
	public VersionResourceResolver addFixedVersionStrategy(String version, String... pathPatterns) {
		List<String> patternsList = Arrays.asList(pathPatterns);
		List<String> prefixedPatterns = new ArrayList<>(pathPatterns.length);
		String versionPrefix = "/" + version;
		for (String pattern : patternsList) {
			prefixedPatterns.add(pattern);
			if (!pattern.startsWith(versionPrefix) && !patternsList.contains(versionPrefix + pattern)) {
				prefixedPatterns.add(versionPrefix + pattern);
			}
		}
		return addVersionStrategy(new FixedVersionStrategy(version), StringUtils.toStringArray(prefixedPatterns));
	}

	/**
	 * Register a custom VersionStrategy to apply to resource URLs that match the
	 * given path patterns.
	 * @param strategy the custom strategy
	 * @param pathPatterns one or more resource URL path patterns,
	 * relative to the pattern configured with the resource handler
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
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations)
				.switchIfEmpty(Mono.defer(() ->
						resolveVersionedResource(exchange, requestPath, locations, chain)));
	}

	private Mono<Resource> resolveVersionedResource(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		VersionStrategy versionStrategy = getStrategyForPath(requestPath);
		if (versionStrategy == null) {
			return Mono.empty();
		}

		String candidate = versionStrategy.extractVersion(requestPath);
		if (!StringUtils.hasLength(candidate)) {
			return Mono.empty();
		}

		String simplePath = versionStrategy.removeVersion(requestPath, candidate);
		return chain.resolveResource(exchange, simplePath, locations)
				.filterWhen(resource -> versionStrategy.getResourceVersion(resource)
						.map(actual -> {
							if (candidate.equals(actual)) {
								return true;
							}
							else {
								if (logger.isTraceEnabled()) {
									String logPrefix = exchange != null ? exchange.getLogPrefix() : "";
									logger.trace(logPrefix + "Found resource for \"" + requestPath +
											"\", but version [" + candidate + "] does not match");
								}
								return false;
							}
						}))
				.map(resource -> new FileNameVersionedResource(resource, candidate));
	}

	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations)
				.flatMap(baseUrl -> {
					if (StringUtils.hasText(baseUrl)) {
						VersionStrategy strategy = getStrategyForPath(resourceUrlPath);
						if (strategy == null) {
							return Mono.just(baseUrl);
						}
						return chain.resolveResource(null, baseUrl, locations)
								.flatMap(resource -> strategy.getResourceVersion(resource)
										.map(version -> strategy.addVersion(baseUrl, version)));
					}
					return Mono.empty();
				});
	}

	/**
	 * Find a {@code VersionStrategy} for the request path of the requested resource.
	 * @return an instance of a {@code VersionStrategy} or null if none matches that request path
	 */
	protected @Nullable VersionStrategy getStrategyForPath(String requestPath) {
		String path = "/".concat(requestPath);
		List<String> matchingPatterns = new ArrayList<>();
		for (String pattern : this.versionStrategyMap.keySet()) {
			if (this.pathMatcher.match(pattern, path)) {
				matchingPatterns.add(pattern);
			}
		}
		if (!matchingPatterns.isEmpty()) {
			Comparator<String> comparator = this.pathMatcher.getPatternComparator(path);
			matchingPatterns.sort(comparator);
			return this.versionStrategyMap.get(matchingPatterns.get(0));
		}
		return null;
	}


	private static class FileNameVersionedResource extends AbstractResource implements HttpResource {

		private final Resource original;

		private final String version;

		public FileNameVersionedResource(Resource original, String version) {
			this.original = original;
			this.version = version;
		}

		@Override
		public boolean exists() {
			return this.original.exists();
		}

		@Override
		public boolean isReadable() {
			return this.original.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.original.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.original.isFile();
		}

		@Override
		public URL getURL() throws IOException {
			return this.original.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.original.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.original.getFile();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.original.getInputStream();
		}

		@Override
		public ReadableByteChannel readableChannel() throws IOException {
			return this.original.readableChannel();
		}

		@Override
		public byte[] getContentAsByteArray() throws IOException {
			return this.original.getContentAsByteArray();
		}

		@Override
		public String getContentAsString(Charset charset) throws IOException {
			return this.original.getContentAsString(charset);
		}

		@Override
		public long contentLength() throws IOException {
			return this.original.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.original.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.original.createRelative(relativePath);
		}

		@Override
		public @Nullable String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public String getDescription() {
			return this.original.getDescription();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			HttpHeaders headers = (this.original instanceof HttpResource hr ? hr.getResponseHeaders() : new HttpHeaders());
			headers.setETag("W/\"" + this.version + "\"");
			return headers;
		}
	}

}
