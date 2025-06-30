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

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@code HttpRequestHandler} that serves static resources in an optimized way
 * according to the guidelines of Page Speed, YSlow, etc.
 *
 * <p>The {@linkplain #setLocations "locations"} property takes a list of Spring
 * {@link Resource} locations from which static resources are allowed to
 * be served by this handler. Resources could be served from a classpath location,
 * for example, "classpath:/META-INF/public-web-resources/", allowing convenient packaging
 * and serving of resources such as .js, .css, and others in jar files.
 *
 * <p>This request handler may also be configured with a
 * {@link #setResourceResolvers(List) resourcesResolver} and
 * {@link #setResourceTransformers(List) resourceTransformer} chains to support
 * arbitrary resolution and transformation of resources being served. By default
 * a {@link PathResourceResolver} simply finds resources based on the configured
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
 * @author Juergen Hoeller
 * @since 5.0
 */
public class ResourceWebHandler implements WebHandler, InitializingBean {

	private static final Set<HttpMethod> SUPPORTED_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);

	private static final Log logger = LogFactory.getLog(ResourceWebHandler.class);


	private @Nullable ResourceLoader resourceLoader;

	private final List<String> locationValues = new ArrayList<>(4);

	private final List<Resource> locationResources = new ArrayList<>(4);

	private final List<Resource> locationsToUse = new ArrayList<>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

	private @Nullable ResourceResolverChain resolverChain;

	private @Nullable ResourceTransformerChain transformerChain;

	private @Nullable CacheControl cacheControl;

	private @Nullable ResourceHttpMessageWriter resourceHttpMessageWriter;

	private @Nullable Map<String, MediaType> mediaTypes;

	private boolean useLastModified = true;

	private @Nullable Function<Resource, String> etagGenerator;

	private boolean optimizeLocations = false;


	/**
	 * Provide the ResourceLoader to load {@link #setLocationValues location values} with.
	 * @since 5.1
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Accepts a list of String-based location values to be resolved into
	 * {@link Resource} locations.
	 * @since 5.1
	 */
	public void setLocationValues(List<String> locationValues) {
		Assert.notNull(locationValues, "Location values list must not be null");
		this.locationValues.clear();
		this.locationValues.addAll(locationValues);
	}

	/**
	 * Return the configured location values.
	 * @since 5.1
	 */
	public List<String> getLocationValues() {
		return this.locationValues;
	}

	/**
	 * Set the {@code List} of {@code Resource} paths to use as sources
	 * for serving static resources.
	 */
	public void setLocations(@Nullable List<Resource> locations) {
		this.locationResources.clear();
		if (locations != null) {
			for (Resource location : locations) {
				ResourceHandlerUtils.assertResourceLocation(location);
				this.locationResources.add(location);
			}
		}
	}

	/**
	 * Return the {@code List} of {@code Resource} paths to use as sources for
	 * serving static resources.
	 * <p>Note that if {@link #setLocationValues(List) locationValues} are provided,
	 * instead of loaded Resource-based locations, this method will return empty
	 * until after initialization via {@link #afterPropertiesSet()}.
	 * <p><strong>Note:</strong> The list of locations may be filtered to exclude
	 * those that don't actually exist and therefore the list returned from this
	 * method may be a subset of all given locations. See {@link #setOptimizeLocations}.
	 * @see #setLocationValues
	 * @see #setLocations
	 */
	public List<Resource> getLocations() {
		if (this.locationsToUse.isEmpty()) {
			// Possibly not yet initialized, return only what we have so far
			return this.locationResources;
		}
		return this.locationsToUse;
	}

	/**
	 * Configure the list of {@link ResourceResolver ResourceResolvers} to use.
	 * <p>By default {@link PathResourceResolver} is configured. If using this property,
	 * it is recommended to add {@link PathResourceResolver} as the last resolver.
	 */
	public void setResourceResolvers(@Nullable List<ResourceResolver> resourceResolvers) {
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
	 * Configure the list of {@link ResourceTransformer ResourceTransformers} to use.
	 * <p>By default no transformers are configured for use.
	 */
	public void setResourceTransformers(@Nullable List<ResourceTransformer> resourceTransformers) {
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
	 * Configure the {@link ResourceHttpMessageWriter} to use.
	 * <p>By default a {@link ResourceHttpMessageWriter} will be configured.
	 */
	public void setResourceHttpMessageWriter(@Nullable ResourceHttpMessageWriter httpMessageWriter) {
		this.resourceHttpMessageWriter = httpMessageWriter;
	}

	/**
	 * Return the configured resource message writer.
	 */
	public @Nullable ResourceHttpMessageWriter getResourceHttpMessageWriter() {
		return this.resourceHttpMessageWriter;
	}

	/**
	 * Set the {@link org.springframework.http.CacheControl} instance to build
	 * the Cache-Control HTTP response header.
	 */
	public void setCacheControl(@Nullable CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * Return the {@link org.springframework.http.CacheControl} instance to build
	 * the Cache-Control HTTP response header.
	 */
	public @Nullable CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * Set whether we should look at the {@link Resource#lastModified()}
	 * when serving resources and use this information to drive {@code "Last-Modified"}
	 * HTTP response headers.
	 * <p>This option is enabled by default and should be turned off if the metadata of
	 * the static files should be ignored.
	 * @since 5.3
	 */
	public void setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
	}

	/**
	 * Return whether the {@link Resource#lastModified()} information is used
	 * to drive HTTP responses when serving static resources.
	 * @since 5.3
	 */
	public boolean isUseLastModified() {
		return this.useLastModified;
	}

	/**
	 * Configure a generator function that will be used to create the ETag information,
	 * given a {@link Resource} that is about to be written to the response.
	 * <p>This function should return a String that will be used as an argument in
	 * {@link ServerWebExchange#checkNotModified(String)}, or {@code null} if no value
	 * can be generated for the given resource.
	 * @param etagGenerator the HTTP ETag generator function to use.
	 * @since 6.1
	 */
	public void setEtagGenerator(@Nullable Function<Resource, String> etagGenerator) {
		this.etagGenerator = etagGenerator;
	}

	/**
	 * Return the HTTP ETag generator function to be used when serving resources.
	 * @return the HTTP ETag generator function
	 * @since 6.1
	 */
	public @Nullable Function<Resource, String> getEtagGenerator() {
		return this.etagGenerator;
	}

	/**
	 * Set whether to optimize the specified locations through an existence
	 * check on startup, filtering non-existing directories upfront so that
	 * they do not have to be checked on every resource access.
	 * <p>The default is {@code false}, for defensiveness against zip files
	 * without directory entries which are unable to expose the existence of
	 * a directory upfront. Switch this flag to {@code true} for optimized
	 * access in case of a consistent jar layout with directory entries.
	 * @since 5.3.13
	 */
	public void setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
	}

	/**
	 * Return whether to optimize the specified locations through an existence
	 * check on startup, filtering non-existing directories upfront so that
	 * they do not have to be checked on every resource access.
	 * @since 5.3.13
	 */
	public boolean isOptimizeLocations() {
		return this.optimizeLocations;
	}

	/**
	 * Add mappings between file extensions extracted from the filename of static
	 * {@link Resource}s and the media types to use for the response.
	 * <p>Use of this method is typically not necessary since mappings can be
	 * also determined via {@link MediaTypeFactory#getMediaType(Resource)}.
	 * @param mediaTypes media type mappings
	 * @since 5.3.2
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		if (this.mediaTypes == null) {
			this.mediaTypes = new HashMap<>(mediaTypes.size());
		}
		mediaTypes.forEach((ext, type) ->
				this.mediaTypes.put(ext.toLowerCase(Locale.ROOT), type));
	}

	/**
	 * Return the {@link #setMediaTypes(Map) configured} media type mappings.
	 * @since 5.3.2
	 */
	public Map<String, MediaType> getMediaTypes() {
		return (this.mediaTypes != null ? this.mediaTypes : Collections.emptyMap());
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		resolveResourceLocations();

		if (this.resourceResolvers.isEmpty()) {
			this.resourceResolvers.add(new PathResourceResolver());
		}

		initAllowedLocations();

		if (getResourceHttpMessageWriter() == null) {
			this.resourceHttpMessageWriter = new ResourceHttpMessageWriter();
		}

		// Initialize immutable resolver and transformer chains
		this.resolverChain = new DefaultResourceResolverChain(this.resourceResolvers);
		this.transformerChain = new DefaultResourceTransformerChain(this.resolverChain, this.resourceTransformers);
	}

	private void resolveResourceLocations() {
		List<Resource> result = new ArrayList<>(this.locationResources);

		if (!this.locationValues.isEmpty()) {
			Assert.notNull(this.resourceLoader,
					"ResourceLoader is required when \"locationValues\" are configured.");
			Assert.isTrue(CollectionUtils.isEmpty(this.locationResources), "Please set " +
					"either Resource-based \"locations\" or String-based \"locationValues\", but not both.");
			for (String location : this.locationValues) {
				location = ResourceHandlerUtils.initLocationPath(location);
				result.add(this.resourceLoader.getResource(location));
			}
		}

		if (isOptimizeLocations()) {
			result = result.stream().filter(Resource::exists).toList();
		}

		this.locationsToUse.clear();
		this.locationsToUse.addAll(result);
	}

	/**
	 * Look for a {@code PathResourceResolver} among the configured resource
	 * resolvers and set its {@code allowedLocations} property (if empty) to
	 * match the {@link #setLocations locations} configured on this class.
	 */
	protected void initAllowedLocations() {
		if (CollectionUtils.isEmpty(getLocations())) {
			return;
		}
		for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
			if (getResourceResolvers().get(i) instanceof PathResourceResolver resolver) {
				if (ObjectUtils.isEmpty(resolver.getAllowedLocations())) {
					resolver.setAllowedLocations(getLocations().toArray(new Resource[0]));
				}
				break;
			}
		}
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
				.switchIfEmpty(Mono.defer(() -> {
					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "Resource not found");
					}
					return Mono.error(new NoResourceFoundException(getResourcePath(exchange)));
				}))
				.flatMap(resource -> {
					try {
						if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
							exchange.getResponse().getHeaders().add("Allow", "GET,HEAD,OPTIONS");
							return Mono.empty();
						}

						// Supported methods and required session
						HttpMethod httpMethod = exchange.getRequest().getMethod();
						if (!SUPPORTED_METHODS.contains(httpMethod)) {
							return Mono.error(new MethodNotAllowedException(
									exchange.getRequest().getMethod(), SUPPORTED_METHODS));
						}

						// Header phase
						String eTagValue = (getEtagGenerator() != null) ? getEtagGenerator().apply(resource) : null;
						Instant lastModified = isUseLastModified() ? Instant.ofEpochMilli(resource.lastModified()) : Instant.MIN;
						if (exchange.checkNotModified(eTagValue, lastModified)) {
							if (logger.isTraceEnabled()) {
								logger.trace(exchange.getLogPrefix() + "Resource not modified");
							}
							return Mono.empty();
						}

						// Apply cache settings, if any
						CacheControl cacheControl = getCacheControl();
						if (cacheControl != null) {
							exchange.getResponse().getHeaders().setCacheControl(cacheControl);
						}

						// Check the media type for the resource
						MediaType mediaType = getMediaType(resource);
						setHeaders(exchange, resource, mediaType);

						// Content phase
						ResourceHttpMessageWriter writer = getResourceHttpMessageWriter();
						Assert.state(writer != null, "No ResourceHttpMessageWriter");
						if (HttpMethod.HEAD == httpMethod) {
							return writer.addDefaultHeaders(exchange.getResponse(), resource, mediaType,
											Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix()))
									.then(exchange.getResponse().setComplete());
						}
						else {
							return writer.write(Mono.just(resource),
									null, ResolvableType.forClass(Resource.class), mediaType,
									exchange.getRequest(), exchange.getResponse(),
									Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix()));
						}
					}
					catch (IOException ex) {
						return Mono.error(ex);
					}
				});
	}

	@SuppressWarnings("NullAway") // Lambda
	protected Mono<Resource> getResource(ServerWebExchange exchange) {
		String rawPath = getResourcePath(exchange);
		String path = processPath(rawPath);
		if (ResourceHandlerUtils.shouldIgnoreInputPath(path) || isInvalidPath(path)) {
			return Mono.empty();
		}

		Assert.state(this.resolverChain != null, "ResourceResolverChain not initialized");
		Assert.state(this.transformerChain != null, "ResourceTransformerChain not initialized");

		return this.resolverChain.resolveResource(exchange, path, getLocations())
				.flatMap(resource -> this.transformerChain.transform(exchange, resource));
	}

	private String getResourcePath(ServerWebExchange exchange) {
		PathPattern pattern = exchange.getRequiredAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (!pattern.hasPatternSyntax()) {
			return pattern.getPatternString();
		}
		PathContainer pathWithinHandler = exchange.getRequiredAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		return pathWithinHandler.value();
	}

	/**
	 * Process the given resource path.
	 * <p>By default, this method delegates to {@link ResourceHandlerUtils#normalizeInputPath}.
	 */
	protected String processPath(String path) {
		return ResourceHandlerUtils.normalizeInputPath(path);
	}

	/**
	 * Invoked after {@link ResourceHandlerUtils#isInvalidPath(String)}
	 * to allow subclasses to perform further validation.
	 * <p>By default, this method does not perform any validations.
	 */
	protected boolean isInvalidPath(String path) {
		return false;
	}

	private @Nullable MediaType getMediaType(Resource resource) {
		MediaType mediaType = null;
		String filename = resource.getFilename();
		if (!CollectionUtils.isEmpty(this.mediaTypes)) {
			String ext = StringUtils.getFilenameExtension(filename);
			if (ext != null) {
				mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ROOT));
			}
		}
		if (mediaType == null) {
			List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
			if (!CollectionUtils.isEmpty(mediaTypes)) {
				mediaType = mediaTypes.get(0);
			}
		}
		return mediaType;
	}

	/**
	 * Set headers on the response. Called for both GET and HEAD requests.
	 * @param exchange current exchange
	 * @param resource the identified resource (never {@code null})
	 * @param mediaType the resource's media type (never {@code null})
	 */
	protected void setHeaders(ServerWebExchange exchange, Resource resource, @Nullable MediaType mediaType)
			throws IOException {

		HttpHeaders headers = exchange.getResponse().getHeaders();

		long length = resource.contentLength();
		headers.setContentLength(length);

		if (mediaType != null) {
			headers.setContentType(mediaType);
		}

		if (resource instanceof HttpResource httpResource) {
			exchange.getResponse().getHeaders().putAll(httpResource.getResponseHeaders());
		}
	}


	@Override
	public String toString() {
		return "ResourceWebHandler " + locationToString(getLocations());
	}

	private String locationToString(List<Resource> locations) {
		return locations.toString()
				.replaceAll("class path resource", "classpath")
				.replaceAll("ServletContext resource", "ServletContext");
	}

}
