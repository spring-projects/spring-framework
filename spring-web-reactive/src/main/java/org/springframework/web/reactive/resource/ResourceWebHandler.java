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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.CompositeContentTypeResolver;
import org.springframework.web.reactive.accept.PathExtensionContentTypeResolver;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * {@code HttpRequestHandler} that serves static resources in an optimized way
 * according to the guidelines of Page Speed, YSlow, etc.
 *
 * <p>The {@linkplain #setLocations "locations"} property takes a list of Spring
 * {@link Resource} locations from which static resources are allowed to
 * be served by this handler. Resources could be served from a classpath location,
 * e.g. "classpath:/META-INF/public-web-resources/", allowing convenient packaging
 * and serving of resources such as .js, .css, and others in jar files.
 *
 * <p>This request handler may also be configured with a
 * {@link #setResourceResolvers(List) resourcesResolver} and
 * {@link #setResourceTransformers(List) resourceTransformer} chains to support
 * arbitrary resolution and transformation of resources being served. By default a
 * {@link PathResourceResolver} simply finds resources based on the configured
 * "locations". An application can configure additional resolvers and
 * transformers such as the {@link VersionResourceResolver} which can resolve
 * and prepare URLs for resources with a version in the URL.
 *
 * <p>This handler also properly evaluates the {@code Last-Modified} header (if
 * present) so that a {@code 304} status code will be returned as appropriate,
 * avoiding unnecessary overhead for resources that are already cached by the
 * client.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceWebHandler
		implements WebHandler, InitializingBean, SmartInitializingSingleton {

	/** Set of supported HTTP methods */
	private static final Set<String> SUPPORTED_METHODS = new LinkedHashSet<>(2);

	private static final Log logger = LogFactory.getLog(ResourceWebHandler.class);

	static {
		SUPPORTED_METHODS.addAll(Arrays.asList("GET", "HEAD"));
	}


	private final List<Resource> locations = new ArrayList<>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

	private CacheControl cacheControl;

	private ResourceHttpMessageWriter resourceHttpMessageWriter;

	private CompositeContentTypeResolver contentTypeResolver;

	private PathExtensionContentTypeResolver pathExtensionResolver;


	/**
	 * Set the {@code List} of {@code Resource} paths to use as sources
	 * for serving static resources.
	 */
	public void setLocations(List<Resource> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locations.clear();
		this.locations.addAll(locations);
	}

	/**
	 * Return the {@code List} of {@code Resource} paths to use as sources
	 * for serving static resources.
	 */
	public List<Resource> getLocations() {
		return this.locations;
	}

	/**
	 * Configure the list of {@link ResourceResolver}s to use.
	 * <p>By default {@link PathResourceResolver} is configured. If using this property,
	 * it is recommended to add {@link PathResourceResolver} as the last resolver.
	 */
	public void setResourceResolvers(List<ResourceResolver> resourceResolvers) {
		this.resourceResolvers.clear();
		if (resourceResolvers != null) {
			this.resourceResolvers.addAll(resourceResolvers);
		}
	}

	/**
	 * Return the list of configured resource resolvers.
	 */
	public List<ResourceResolver> getResourceResolvers() {
		return this.resourceResolvers;
	}

	/**
	 * Configure the list of {@link ResourceTransformer}s to use.
	 * <p>By default no transformers are configured for use.
	 */
	public void setResourceTransformers(List<ResourceTransformer> resourceTransformers) {
		this.resourceTransformers.clear();
		if (resourceTransformers != null) {
			this.resourceTransformers.addAll(resourceTransformers);
		}
	}

	/**
	 * Return the list of configured resource transformers.
	 */
	public List<ResourceTransformer> getResourceTransformers() {
		return this.resourceTransformers;
	}

	/**
	 * Set the {@link org.springframework.http.CacheControl} instance to build
	 * the Cache-Control HTTP response header.
	 */
	public void setCacheControl(CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	public CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * Configure the {@link ResourceHttpMessageWriter} to use.
	 * <p>By default a {@link ResourceHttpMessageWriter} will be configured.
	 */
	public void setResourceHttpMessageWriter(ResourceHttpMessageWriter httpMessageWriter) {
		this.resourceHttpMessageWriter = httpMessageWriter;
	}

	/**
	 * Return the configured resource message writer.
	 */
	public ResourceHttpMessageWriter getResourceHttpMessageWriter() {
		return this.resourceHttpMessageWriter;
	}

	/**
	 * Configure a {@link CompositeContentTypeResolver} to help determine the
	 * media types for resources being served. If the manager contains a path
	 * extension resolver it will be checked for registered file extension.
	 * @param contentTypeResolver the resolver in use
	 */
	public void setContentTypeResolver(CompositeContentTypeResolver contentTypeResolver) {
		this.contentTypeResolver = contentTypeResolver;
	}

	/**
	 * Return the configured {@link CompositeContentTypeResolver}.
	 */
	public CompositeContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (logger.isWarnEnabled() && CollectionUtils.isEmpty(this.locations)) {
			logger.warn("Locations list is empty. No resources will be served unless a " +
					"custom ResourceResolver is configured as an alternative to PathResourceResolver.");
		}
		if (this.resourceResolvers.isEmpty()) {
			this.resourceResolvers.add(new PathResourceResolver());
		}
		initAllowedLocations();
		if (this.resourceHttpMessageWriter == null) {
			this.resourceHttpMessageWriter = new ResourceHttpMessageWriter();
		}
	}

	/**
	 * Look for a {@code PathResourceResolver} among the configured resource
	 * resolvers and set its {@code allowedLocations} property (if empty) to
	 * match the {@link #setLocations locations} configured on this class.
	 */
	protected void initAllowedLocations() {
		if (CollectionUtils.isEmpty(this.locations)) {
			return;
		}
		for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				PathResourceResolver resolver = (PathResourceResolver) getResourceResolvers().get(i);
				if (ObjectUtils.isEmpty(resolver.getAllowedLocations())) {
					resolver.setAllowedLocations(getLocations().toArray(new Resource[getLocations().size()]));
				}
				break;
			}
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.pathExtensionResolver = initContentNegotiationStrategy();
	}

	protected PathExtensionContentTypeResolver initContentNegotiationStrategy() {
		Map<String, MediaType> mediaTypes = null;
		if (getContentTypeResolver() != null) {
			PathExtensionContentTypeResolver strategy =
					getContentTypeResolver().findResolver(PathExtensionContentTypeResolver.class);
			if (strategy != null) {
				mediaTypes = new HashMap<>(strategy.getMediaTypes());
			}
		}
		return new PathExtensionContentTypeResolver(mediaTypes);
	}


	/**
	 * Processes a resource request.
	 * <p>Checks for the existence of the requested resource in the configured list of locations.
	 * If the resource does not exist, a {@code 404} response will be returned to the client.
	 * If the resource exists, the request will be checked for the presence of the
	 * {@code Last-Modified} header, and its value will be compared against the last-modified
	 * timestamp of the given resource, returning a {@code 304} status code if the
	 * {@code Last-Modified} value  is greater. If the resource is newer than the
	 * {@code Last-Modified} value, or the header is not present, the content resource
	 * of the resource will be written to the response with caching headers
	 * set to expire one year in the future.
	 */
	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {

		return getResource(exchange)
				.otherwiseIfEmpty(Mono.defer(() -> {
					logger.trace("No matching resource found - returning 404");
					exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
					return Mono.empty();
				}))
				.then(resource -> {
					try {
						if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
							exchange.getResponse().getHeaders().add("Allow", "GET,HEAD,OPTIONS");
							return Mono.empty();
						}

						// Supported methods and required session
						String httpMehtod = exchange.getRequest().getMethod().name();
						if (!SUPPORTED_METHODS.contains(httpMehtod)) {
							return Mono.error(new MethodNotAllowedException(httpMehtod, SUPPORTED_METHODS));
						}

						// Header phase
						if (exchange.checkNotModified(Instant.ofEpochMilli(resource.lastModified()))) {
							logger.trace("Resource not modified - returning 304");
							return Mono.empty();
						}

						// Apply cache settings, if any
						if (getCacheControl() != null) {
							String value = getCacheControl().getHeaderValue();
							if (value != null) {
								exchange.getResponse().getHeaders().setCacheControl(value);
							}
						}

						// Check the media type for the resource
						MediaType mediaType = getMediaType(exchange, resource);
						if (mediaType != null) {
							if (logger.isTraceEnabled()) {
								logger.trace("Determined media type '" + mediaType + "' for " + resource);
							}
						}
						else {
							if (logger.isTraceEnabled()) {
								logger.trace("No media type found " +
										"for " + resource + " - not sending a content-type header");
							}
						}

						// Content phase
						if (HttpMethod.HEAD.equals(exchange.getRequest().getMethod())) {
							setHeaders(exchange, resource, mediaType);
							logger.trace("HEAD request - skipping content");
							return Mono.empty();
						}

						setHeaders(exchange, resource, mediaType);
						return this.resourceHttpMessageWriter.write(Mono.just(resource),
								null, ResolvableType.forClass(Resource.class), mediaType,
								exchange.getRequest(), exchange.getResponse(), Collections.emptyMap());
					}
					catch (IOException|ResponseStatusException ex) {
						return Mono.error(ex);
					}
				});
	}

	protected Mono<Resource> getResource(ServerWebExchange exchange) {

		String attributeName = HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;
		Optional<String> optional = exchange.getAttribute(attributeName);
		if (!optional.isPresent()) {
			return Mono.error(new IllegalStateException(
					"Required request attribute '" + attributeName + "' is not set"));
		}

		String path = processPath(optional.get());
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring invalid resource path [" + path + "]");
			}
			return Mono.empty();
		}

		if (path.contains("%")) {
			try {
				// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
				if (isInvalidPath(URLDecoder.decode(path, "UTF-8"))) {
					if (logger.isTraceEnabled()) {
						logger.trace("Ignoring invalid resource path with escape sequences [" + path + "].");
					}
					return Mono.empty();
				}
			}
			catch (IllegalArgumentException ex) {
				// ignore
			}
			catch (UnsupportedEncodingException ex) {
				return Mono.error(Exceptions.propagate(ex));
			}
		}

		ResourceResolverChain resolveChain = createResolverChain();
		return resolveChain.resolveResource(exchange, path, getLocations())
				.then(resource -> {
					ResourceTransformerChain transformerChain = createTransformerChain(resolveChain);
					return transformerChain.transform(exchange, resource);
				});
	}

	/**
	 * Process the given resource path to be used.
	 * <p>The default implementation replaces any combination of leading '/' and
	 * control characters (00-1F and 7F) with a single "/" or "". For example
	 * {@code "  // /// ////  foo/bar"} becomes {@code "/foo/bar"}.
	 */
	protected String processPath(String path) {
		boolean slash = false;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				slash = true;
			}
			else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				if (i == 0 || (i == 1 && slash)) {
					return path;
				}
				path = slash ? "/" + path.substring(i) : path.substring(i);
				if (logger.isTraceEnabled()) {
					logger.trace("Path after trimming leading '/' and control characters: " + path);
				}
				return path;
			}
		}
		return (slash ? "/" : "");
	}

	/**
	 * Identifies invalid resource paths. By default rejects:
	 * <ul>
	 * <li>Paths that contain "WEB-INF" or "META-INF"
	 * <li>Paths that contain "../" after a call to
	 * {@link StringUtils#cleanPath}.
	 * <li>Paths that represent a {@link ResourceUtils#isUrl
	 * valid URL} or would represent one after the leading slash is removed.
	 * </ul>
	 * <p><strong>Note:</strong> this method assumes that leading, duplicate '/'
	 * or control characters (e.g. white space) have been trimmed so that the
	 * path starts predictably with a single '/' or does not have one.
	 * @param path the path to validate
	 * @return {@code true} if the path is invalid, {@code false} otherwise
	 */
	protected boolean isInvalidPath(String path) {
		if (logger.isTraceEnabled()) {
			logger.trace("Applying \"invalid path\" checks to path: " + path);
		}
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			if (logger.isTraceEnabled()) {
				logger.trace("Path contains \"WEB-INF\" or \"META-INF\".");
			}
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				if (logger.isTraceEnabled()) {
					logger.trace("Path represents URL or has \"url:\" prefix.");
				}
				return true;
			}
		}
		if (path.contains("..")) {
			path = StringUtils.cleanPath(path);
			if (path.contains("../")) {
				if (logger.isTraceEnabled()) {
					logger.trace("Path contains \"../\" after call to StringUtils#cleanPath.");
				}
				return true;
			}
		}
		return false;
	}

	private ResourceResolverChain createResolverChain() {
		return new DefaultResourceResolverChain(getResourceResolvers());
	}

	private ResourceTransformerChain createTransformerChain(ResourceResolverChain resolverChain) {
		return new DefaultResourceTransformerChain(resolverChain, getResourceTransformers());
	}

	/**
	 * Determine the media type for the given request and the resource matched
	 * to it. This implementation tries to determine the MediaType based on the
	 * file extension of the Resource via
	 * {@link PathExtensionContentTypeResolver#resolveMediaTypeForResource(Resource)}.
	 * @param exchange the current exchange
	 * @param resource the resource to check
	 * @return the corresponding media type, or {@code null} if none found
	 */
	protected MediaType getMediaType(ServerWebExchange exchange, Resource resource) {
		return this.pathExtensionResolver.resolveMediaTypeForResource(resource);
	}

	/**
	 * Set headers on the response. Called for both GET and HEAD requests.
	 * @param exchange current exchange
	 * @param resource the identified resource (never {@code null})
	 * @param mediaType the resource's media type (never {@code null})
	 */
	protected void setHeaders(ServerWebExchange exchange, Resource resource, MediaType mediaType)
			throws IOException {

		HttpHeaders headers = exchange.getResponse().getHeaders();

		long length = resource.contentLength();
		headers.setContentLength(length);

		if (mediaType != null) {
			headers.setContentType(mediaType);
		}
		if (resource instanceof HttpResource) {
			HttpHeaders resourceHeaders = ((HttpResource) resource).getResponseHeaders();
			exchange.getResponse().getHeaders().putAll(resourceHeaders);
		}
		headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
	}


	@Override
	public String toString() {
		return "ResourceWebHandler [locations=" + getLocations() + ", resolvers=" + getResourceResolvers() + "]";
	}

}
