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

package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@code HttpRequestHandler} that serves static resources in an optimized way
 * according to the guidelines of Page Speed, YSlow, etc.
 *
 * <p>The properties {@linkplain #setLocations "locations"} and
 * {@linkplain #setLocationValues "locationValues"} accept locations from which
 * static resources can be served by this handler. This can be relative to the
 * root of the web application, or from the classpath, e.g.
 * "classpath:/META-INF/public-web-resources/", allowing convenient packaging
 * and serving of resources such as .js, .css, and others in jar files.
 *
 * <p>This request handler may also be configured with a
 * {@link #setResourceResolvers(List) resourcesResolver} and
 * {@link #setResourceTransformers(List) resourceTransformer} chains to support
 * arbitrary resolution and transformation of resources being served. By default
 * a {@link PathResourceResolver} simply finds resources based on the configured
 * "locations". An application can configure additional resolvers and transformers
 * such as the {@link VersionResourceResolver} which can resolve and prepare URLs
 * for resources with a version in the URL.
 *
 * <p>This handler also properly evaluates the {@code Last-Modified} header
 * (if present) so that a {@code 304} status code will be returned as appropriate,
 * avoiding unnecessary overhead for resources that are already cached by the client.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler extends WebContentGenerator
		implements HttpRequestHandler, EmbeddedValueResolverAware, InitializingBean, CorsConfigurationSource {

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private static final String URL_RESOURCE_CHARSET_PREFIX = "[charset=";


	private final List<String> locationValues = new ArrayList<>(4);

	private final List<Resource> locationResources = new ArrayList<>(4);

	private final List<Resource> locationsToUse = new ArrayList<>(4);

	private final Map<Resource, Charset> locationCharsets = new HashMap<>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

	@Nullable
	private ResourceResolverChain resolverChain;

	@Nullable
	private ResourceTransformerChain transformerChain;

	@Nullable
	private ResourceHttpMessageConverter resourceHttpMessageConverter;

	@Nullable
	private ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	private final Map<String, MediaType> mediaTypes = new HashMap<>(4);

	@Nullable
	private CorsConfiguration corsConfiguration;

	@Nullable
	private UrlPathHelper urlPathHelper;

	private boolean useLastModified = true;

	private boolean optimizeLocations = false;

	@Nullable
	private StringValueResolver embeddedValueResolver;


	public ResourceHttpRequestHandler() {
		super(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}


	/**
	 * Configure String-based locations to serve resources from.
	 * <p>For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}}
	 * allows resources to be served both from the web application root and
	 * from any JAR on the classpath that contains a
	 * {@code /META-INF/public-web-resources/} directory, with resources in the
	 * web application root taking precedence.
	 * <p>For {@link org.springframework.core.io.UrlResource URL-based resources}
	 * (e.g. files, HTTP URLs, etc) this method supports a special prefix to
	 * indicate the charset associated with the URL so that relative paths
	 * appended to it can be encoded correctly, for example
	 * {@code "[charset=Windows-31J]https://example.org/path"}.
	 * @since 4.3.13
	 * @see #setLocations(List)
	 */
	public void setLocationValues(List<String> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locationValues.clear();
		this.locationValues.addAll(locations);
	}

	/**
	 * Configure locations to serve resources from as pre-resourced Resource's.
	 * @see #setLocationValues(List)
	 */
	public void setLocations(List<Resource> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locationResources.clear();
		this.locationResources.addAll(locations);
	}

	/**
	 * Return the configured {@code List} of {@code Resource} locations including
	 * both String-based locations provided via
	 * {@link #setLocationValues(List) setLocationValues} and pre-resolved
	 * {@code Resource} locations provided via {@link #setLocations(List) setLocations}.
	 * <p>Note that the returned list is fully initialized only after
	 * initialization via {@link #afterPropertiesSet()}.
	 * <p><strong>Note:</strong> As of 5.3.11 the list of locations may be filtered to
	 * exclude those that don't actually exist and therefore the list returned from this
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
	 * Configure the {@link ResourceHttpMessageConverter} to use.
	 * <p>By default a {@link ResourceHttpMessageConverter} will be configured.
	 * @since 4.3
	 */
	public void setResourceHttpMessageConverter(@Nullable ResourceHttpMessageConverter messageConverter) {
		this.resourceHttpMessageConverter = messageConverter;
	}

	/**
	 * Return the configured resource converter.
	 * @since 4.3
	 */
	@Nullable
	public ResourceHttpMessageConverter getResourceHttpMessageConverter() {
		return this.resourceHttpMessageConverter;
	}

	/**
	 * Configure the {@link ResourceRegionHttpMessageConverter} to use.
	 * <p>By default a {@link ResourceRegionHttpMessageConverter} will be configured.
	 * @since 4.3
	 */
	public void setResourceRegionHttpMessageConverter(@Nullable ResourceRegionHttpMessageConverter messageConverter) {
		this.resourceRegionHttpMessageConverter = messageConverter;
	}

	/**
	 * Return the configured resource region converter.
	 * @since 4.3
	 */
	@Nullable
	public ResourceRegionHttpMessageConverter getResourceRegionHttpMessageConverter() {
		return this.resourceRegionHttpMessageConverter;
	}

	/**
	 * Configure a {@code ContentNegotiationManager} to help determine the
	 * media types for resources being served. If the manager contains a path
	 * extension strategy it will be checked for registered file extension.
	 * @since 4.3
	 * @deprecated as of 5.2.4 in favor of using {@link #setMediaTypes(Map)}
	 * with mappings possibly obtained from
	 * {@link ContentNegotiationManager#getMediaTypeMappings()}.
	 */
	@Deprecated
	public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * Return the configured content negotiation manager.
	 * @since 4.3
	 * @deprecated as of 5.2.4
	 */
	@Nullable
	@Deprecated
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * Add mappings between file extensions, extracted from the filename of a
	 * static {@link Resource}, and corresponding media type to set on the
	 * response.
	 * <p>Use of this method is typically not necessary since mappings are
	 * otherwise determined via
	 * {@link javax.servlet.ServletContext#getMimeType(String)} or via
	 * {@link MediaTypeFactory#getMediaType(Resource)}.
	 * @param mediaTypes media type mappings
	 * @since 5.2.4
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		mediaTypes.forEach((ext, mediaType) ->
				this.mediaTypes.put(ext.toLowerCase(Locale.ENGLISH), mediaType));
	}

	/**
	 * Return the {@link #setMediaTypes(Map) configured} media types.
	 * @since 5.2.4
	 */
	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * Specify the CORS configuration for resources served by this handler.
	 * <p>By default this is not set in which allows cross-origin requests.
	 */
	public void setCorsConfiguration(CorsConfiguration corsConfiguration) {
		this.corsConfiguration = corsConfiguration;
	}

	/**
	 * Return the specified CORS configuration.
	 */
	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		return this.corsConfiguration;
	}

	/**
	 * Provide a reference to the {@link UrlPathHelper} used to map requests to
	 * static resources. This helps to derive information about the lookup path
	 * such as whether it is decoded or not.
	 * @since 4.3.13
	 */
	public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * The configured {@link UrlPathHelper}.
	 * @since 4.3.13
	 */
	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Set whether we should look at the {@link Resource#lastModified()} when
	 * serving resources and use this information to drive {@code "Last-Modified"}
	 * HTTP response headers.
	 * <p>This option is enabled by default and should be turned off if the metadata
	 * of the static files should be ignored.
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

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		resolveResourceLocations();

		if (this.resourceResolvers.isEmpty()) {
			this.resourceResolvers.add(new PathResourceResolver());
		}

		initAllowedLocations();

		// Initialize immutable resolver and transformer chains
		this.resolverChain = new DefaultResourceResolverChain(this.resourceResolvers);
		this.transformerChain = new DefaultResourceTransformerChain(this.resolverChain, this.resourceTransformers);

		if (this.resourceHttpMessageConverter == null) {
			this.resourceHttpMessageConverter = new ResourceHttpMessageConverter();
		}
		if (this.resourceRegionHttpMessageConverter == null) {
			this.resourceRegionHttpMessageConverter = new ResourceRegionHttpMessageConverter();
		}

		ContentNegotiationManager manager = getContentNegotiationManager();
		if (manager != null) {
			setMediaTypes(manager.getMediaTypeMappings());
		}

		@SuppressWarnings("deprecation")
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy strategy =
				initContentNegotiationStrategy();
		if (strategy != null) {
			setMediaTypes(strategy.getMediaTypes());
		}
	}

	private void resolveResourceLocations() {
		List<Resource> result = new ArrayList<>();
		if (!this.locationValues.isEmpty()) {
			ApplicationContext applicationContext = obtainApplicationContext();
			for (String location : this.locationValues) {
				if (this.embeddedValueResolver != null) {
					String resolvedLocation = this.embeddedValueResolver.resolveStringValue(location);
					if (resolvedLocation == null) {
						throw new IllegalArgumentException("Location resolved to null: " + location);
					}
					location = resolvedLocation;
				}
				Charset charset = null;
				location = location.trim();
				if (location.startsWith(URL_RESOURCE_CHARSET_PREFIX)) {
					int endIndex = location.indexOf(']', URL_RESOURCE_CHARSET_PREFIX.length());
					if (endIndex == -1) {
						throw new IllegalArgumentException("Invalid charset syntax in location: " + location);
					}
					String value = location.substring(URL_RESOURCE_CHARSET_PREFIX.length(), endIndex);
					charset = Charset.forName(value);
					location = location.substring(endIndex + 1);
				}
				Resource resource = applicationContext.getResource(location);
				if (location.equals("/") && !(resource instanceof ServletContextResource)) {
					throw new IllegalStateException(
							"The String-based location \"/\" should be relative to the web application root " +
							"but resolved to a Resource of type: " + resource.getClass() + ". " +
							"If this is intentional, please pass it as a pre-configured Resource via setLocations.");
				}
				result.add(resource);
				if (charset != null) {
					if (!(resource instanceof UrlResource)) {
						throw new IllegalArgumentException("Unexpected charset for non-UrlResource: " + resource);
					}
					this.locationCharsets.put(resource, charset);
				}
			}
		}

		result.addAll(this.locationResources);
		if (isOptimizeLocations()) {
			result = result.stream().filter(Resource::exists).collect(Collectors.toList());
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
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				PathResourceResolver pathResolver = (PathResourceResolver) getResourceResolvers().get(i);
				if (ObjectUtils.isEmpty(pathResolver.getAllowedLocations())) {
					pathResolver.setAllowedLocations(getLocations().toArray(new Resource[0]));
				}
				if (this.urlPathHelper != null) {
					pathResolver.setLocationCharsets(this.locationCharsets);
					pathResolver.setUrlPathHelper(this.urlPathHelper);
				}
				break;
			}
		}
	}

	/**
	 * Initialize the strategy to use to determine the media type for a resource.
	 * @deprecated as of 5.2.4 this method returns {@code null}, and if a
	 * sub-class returns an actual instance,the instance is used only as a
	 * source of media type mappings, if it contains any. Please, use
	 * {@link #setMediaTypes(Map)} instead, or if you need to change behavior,
	 * you can override {@link #getMediaType(HttpServletRequest, Resource)}.
	 */
	@Nullable
	@Deprecated
	@SuppressWarnings("deprecation")
	protected org.springframework.web.accept.PathExtensionContentNegotiationStrategy initContentNegotiationStrategy() {
		return null;
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
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// For very general mappings (e.g. "/") we need to check 404 first
		Resource resource = getResource(request);
		if (resource == null) {
			logger.debug("Resource not found");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return;
		}

		// Supported methods and required session
		checkRequest(request);

		// Header phase
		if (isUseLastModified() && new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
			logger.trace("Resource not modified");
			return;
		}

		// Apply cache settings, if any
		prepareResponse(response);

		// Check the media type for the resource
		MediaType mediaType = getMediaType(request, resource);
		setHeaders(response, resource, mediaType);

		// Content phase
		ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
		if (request.getHeader(HttpHeaders.RANGE) == null) {
			Assert.state(this.resourceHttpMessageConverter != null, "Not initialized");
			this.resourceHttpMessageConverter.write(resource, mediaType, outputMessage);
		}
		else {
			Assert.state(this.resourceRegionHttpMessageConverter != null, "Not initialized");
			ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
			try {
				List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				this.resourceRegionHttpMessageConverter.write(
						HttpRange.toResourceRegions(httpRanges, resource), mediaType, outputMessage);
			}
			catch (IllegalArgumentException ex) {
				response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			}
		}
	}

	@Nullable
	protected Resource getResource(HttpServletRequest request) throws IOException {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" +
					HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}

		path = processPath(path);
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return null;
		}
		if (isInvalidEncodedPath(path)) {
			return null;
		}

		Assert.notNull(this.resolverChain, "ResourceResolverChain not initialized.");
		Assert.notNull(this.transformerChain, "ResourceTransformerChain not initialized.");

		Resource resource = this.resolverChain.resolveResource(request, path, getLocations());
		if (resource != null) {
			resource = this.transformerChain.transform(request, resource);
		}
		return resource;
	}

	/**
	 * Process the given resource path.
	 * <p>The default implementation replaces:
	 * <ul>
	 * <li>Backslash with forward slash.
	 * <li>Duplicate occurrences of slash with a single slash.
	 * <li>Any combination of leading slash and control characters (00-1F and 7F)
	 * with a single "/" or "". For example {@code "  / // foo/bar"}
	 * becomes {@code "/foo/bar"}.
	 * </ul>
	 * @since 3.2.12
	 */
	protected String processPath(String path) {
		path = StringUtils.replace(path, "\\", "/");
		path = cleanDuplicateSlashes(path);
		return cleanLeadingSlash(path);
	}

	private String cleanDuplicateSlashes(String path) {
		StringBuilder sb = null;
		char prev = 0;
		for (int i = 0; i < path.length(); i++) {
			char curr = path.charAt(i);
			try {
				if ((curr == '/') && (prev == '/')) {
					if (sb == null) {
						sb = new StringBuilder(path.substring(0, i));
					}
					continue;
				}
				if (sb != null) {
					sb.append(path.charAt(i));
				}
			}
			finally {
				prev = curr;
			}
		}
		return sb != null ? sb.toString() : path;
	}

	private String cleanLeadingSlash(String path) {
		boolean slash = false;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				slash = true;
			}
			else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				if (i == 0 || (i == 1 && slash)) {
					return path;
				}
				return (slash ? "/" + path.substring(i) : path.substring(i));
			}
		}
		return (slash ? "/" : "");
	}

	/**
	 * Check whether the given path contains invalid escape sequences.
	 * @param path the path to validate
	 * @return {@code true} if the path is invalid, {@code false} otherwise
	 */
	private boolean isInvalidEncodedPath(String path) {
		if (path.contains("%")) {
			try {
				// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
				String decodedPath = URLDecoder.decode(path, "UTF-8");
				if (isInvalidPath(decodedPath)) {
					return true;
				}
				decodedPath = processPath(decodedPath);
				if (isInvalidPath(decodedPath)) {
					return true;
				}
			}
			catch (IllegalArgumentException ex) {
				// May not be possible to decode...
			}
			catch (UnsupportedEncodingException ex) {
				// Should never happen...
			}
		}
		return false;
	}

	/**
	 * Identifies invalid resource paths. By default rejects:
	 * <ul>
	 * <li>Paths that contain "WEB-INF" or "META-INF"
	 * <li>Paths that contain "../" after a call to
	 * {@link org.springframework.util.StringUtils#cleanPath}.
	 * <li>Paths that represent a {@link org.springframework.util.ResourceUtils#isUrl
	 * valid URL} or would represent one after the leading slash is removed.
	 * </ul>
	 * <p><strong>Note:</strong> this method assumes that leading, duplicate '/'
	 * or control characters (e.g. white space) have been trimmed so that the
	 * path starts predictably with a single '/' or does not have one.
	 * @param path the path to validate
	 * @return {@code true} if the path is invalid, {@code false} otherwise
	 * @since 3.0.6
	 */
	protected boolean isInvalidPath(String path) {
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			if (logger.isWarnEnabled()) {
				logger.warn("Path with \"WEB-INF\" or \"META-INF\": [" + path + "]");
			}
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				if (logger.isWarnEnabled()) {
					logger.warn("Path represents URL or has \"url:\" prefix: [" + path + "]");
				}
				return true;
			}
		}
		if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
			if (logger.isWarnEnabled()) {
				logger.warn("Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]");
			}
			return true;
		}
		return false;
	}

	/**
	 * Determine the media type for the given request and the resource matched
	 * to it. This implementation tries to determine the MediaType using one of
	 * the following lookups based on the resource filename and its path
	 * extension:
	 * <ol>
	 * <li>{@link javax.servlet.ServletContext#getMimeType(String)}
	 * <li>{@link #getMediaTypes()}
	 * <li>{@link MediaTypeFactory#getMediaType(String)}
	 * </ol>
	 * @param request the current request
	 * @param resource the resource to check
	 * @return the corresponding media type, or {@code null} if none found
	 */
	@Nullable
	protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
		MediaType result = null;
		String mimeType = request.getServletContext().getMimeType(resource.getFilename());
		if (StringUtils.hasText(mimeType)) {
			result = MediaType.parseMediaType(mimeType);
		}
		if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
			MediaType mediaType = null;
			String filename = resource.getFilename();
			String ext = StringUtils.getFilenameExtension(filename);
			if (ext != null) {
				mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
			}
			if (mediaType == null) {
				List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
				if (!CollectionUtils.isEmpty(mediaTypes)) {
					mediaType = mediaTypes.get(0);
				}
			}
			if (mediaType != null) {
				result = mediaType;
			}
		}
		return result;
	}

	/**
	 * Set headers on the given servlet response.
	 * Called for GET requests as well as HEAD requests.
	 * @param response current servlet response
	 * @param resource the identified resource (never {@code null})
	 * @param mediaType the resource's media type (never {@code null})
	 * @throws IOException in case of errors while setting the headers
	 */
	protected void setHeaders(HttpServletResponse response, Resource resource, @Nullable MediaType mediaType)
			throws IOException {

		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}

		if (resource instanceof HttpResource) {
			HttpHeaders resourceHeaders = ((HttpResource) resource).getResponseHeaders();
			resourceHeaders.forEach((headerName, headerValues) -> {
				boolean first = true;
				for (String headerValue : headerValues) {
					if (first) {
						response.setHeader(headerName, headerValue);
					}
					else {
						response.addHeader(headerName, headerValue);
					}
					first = false;
				}
			});
		}

		response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
	}


	@Override
	public String toString() {
		return "ResourceHttpRequestHandler " + locationToString(getLocations());
	}

	private String locationToString(List<Resource> locations) {
		return locations.toString()
				.replaceAll("class path resource", "classpath")
				.replaceAll("ServletContext resource", "ServletContext");
	}

}
