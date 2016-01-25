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

package org.springframework.web.servlet.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * {@link HttpRequestHandler} that serves static resources optimized for superior browser performance
 * (according to the guidelines of Page Speed, YSlow, etc.) by allowing for flexible cache settings
 * ({@linkplain #setCacheSeconds "cacheSeconds" property}, last-modified support).
 *
 * <p>The {@linkplain #setLocations "locations" property} takes a list of Spring {@link Resource}
 * locations from which static resources are allowed  to be served by this handler. For a given request,
 * the list of locations will be consulted in order for the presence of the requested resource, and the
 * first found match will be written to the response, with a HTTP Caching headers
 * set as configured. The handler also properly evaluates the {@code Last-Modified} header
 * (if present) so that a {@code 304} status code will be returned as appropriate, avoiding unnecessary
 * overhead for resources that are already cached by the client. The use of {@code Resource} locations
 * allows resource requests to easily be mapped to locations other than the web application root.
 * For example, resources could be served from a classpath location such as
 * "classpath:/META-INF/public-web-resources/", allowing convenient packaging and serving of resources
 * such as a JavaScript library from within jar files.
 *
 * <p>To ensure that users with a primed browser cache get the latest changes to application-specific
 * resources upon deployment of new versions of the application, it is recommended that a version
 * string is used in the URL  mapping pattern that selects this handler. Such patterns can be easily
 * parameterized using Spring EL. See the reference manual for further examples of this approach.
 *
 * <p>For various front-end needs &mdash; such as ensuring that users with a primed browser cache
 * get the latest changes, or serving variations of resources (e.g., minified versions) &mdash;
 * {@link org.springframework.web.servlet.resource.ResourceResolver}s can be configured.
 *
 * <p>This handler can be configured through use of a
 * {@link org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry}
 * or the {@code <mvc:resources/>} XML configuration element.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler extends WebContentGenerator
		implements HttpRequestHandler, InitializingBean, CorsConfigurationSource {

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private static final boolean jafPresent = ClassUtils.isPresent(
			"javax.activation.FileTypeMap", ResourceHttpRequestHandler.class.getClassLoader());


	private final List<Resource> locations = new ArrayList<Resource>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<ResourceResolver>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<ResourceTransformer>(4);

	private CorsConfiguration corsConfiguration;


	public ResourceHttpRequestHandler() {
		super(HttpMethod.GET.name(), HttpMethod.HEAD.name());
		this.resourceResolvers.add(new PathResourceResolver());
	}


	/**
	 * Set a {@code List} of {@code Resource} paths to use as sources
	 * for serving static resources.
	 */
	public void setLocations(List<Resource> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locations.clear();
		this.locations.addAll(locations);
	}

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

	public void setCorsConfiguration(CorsConfiguration corsConfiguration) {
		this.corsConfiguration = corsConfiguration;
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		return this.corsConfiguration;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (logger.isWarnEnabled() && CollectionUtils.isEmpty(this.locations)) {
			logger.warn("Locations list is empty. No resources will be served unless a " +
					"custom ResourceResolver is configured as an alternative to PathResourceResolver.");
		}
		initAllowedLocations();
	}

	/**
	 * Look for a {@link org.springframework.web.servlet.resource.PathResourceResolver}
	 * among the {@link #getResourceResolvers() resource resolvers} and configure
	 * its {@code "allowedLocations"} to match the value of the
	 * {@link #setLocations(java.util.List) locations} property unless the "allowed
	 * locations" of the {@code PathResourceResolver} is non-empty.
	 */
	protected void initAllowedLocations() {
		if (CollectionUtils.isEmpty(this.locations)) {
			return;
		}
		for (int i = getResourceResolvers().size()-1; i >= 0; i--) {
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				PathResourceResolver pathResolver = (PathResourceResolver) getResourceResolvers().get(i);
				if (ObjectUtils.isEmpty(pathResolver.getAllowedLocations())) {
					pathResolver.setAllowedLocations(getLocations().toArray(new Resource[getLocations().size()]));
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
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return;
		}

		// Supported methods and required session
		checkRequest(request);

		// Check whether a matching resource exists
		Resource resource = getResource(request);
		if (resource == null) {
			logger.trace("No matching resource found - returning 404");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// Header phase
		if (new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
			logger.trace("Resource not modified - returning 304");
			return;
		}

		// Apply cache settings, if any
		prepareResponse(response);

		// Check the resource's media type
		MediaType mediaType = getMediaType(resource);
		if (mediaType != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Determined media type '" + mediaType + "' for " + resource);
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("No media type found for " + resource + " - not sending a content-type header");
			}
		}

		// Content phase
		if (METHOD_HEAD.equals(request.getMethod())) {
			setHeaders(response, resource, mediaType);
			logger.trace("HEAD request - skipping content");
			return;
		}

		if (request.getHeader(HttpHeaders.RANGE) == null) {
			setHeaders(response, resource, mediaType);
			writeContent(response, resource);
		}
		else {
			writePartialContent(request, response, resource, mediaType);
		}
	}

	protected Resource getResource(HttpServletRequest request) throws IOException {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" +
					HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}
		path = processPath(path);
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring invalid resource path [" + path + "]");
			}
			return null;
		}
		if (path.contains("%")) {
			try {
				// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
				if (isInvalidPath(URLDecoder.decode(path, "UTF-8"))) {
					if (logger.isTraceEnabled()) {
						logger.trace("Ignoring invalid resource path with escape sequences [" + path + "].");
					}
					return null;
				}
			}
			catch (IllegalArgumentException ex) {
				// ignore
			}
		}
		ResourceResolverChain resolveChain = new DefaultResourceResolverChain(getResourceResolvers());
		Resource resource = resolveChain.resolveResource(request, path, getLocations());
		if (resource == null || getResourceTransformers().isEmpty()) {
			return resource;
		}
		ResourceTransformerChain transformChain =
				new DefaultResourceTransformerChain(resolveChain, getResourceTransformers());
		resource = transformChain.transform(request, resource);
		return resource;
	}

	/**
	 * Process the given resource path to be used.
	 * <p>The default implementation replaces any combination of leading '/' and
	 * control characters (00-1F and 7F) with a single "/" or "". For example
	 * {@code "  // /// ////  foo/bar"} becomes {@code "/foo/bar"}.
	 * @since 3.2.12
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
	 * {@link org.springframework.util.StringUtils#cleanPath}.
	 * <li>Paths that represent a {@link org.springframework.util.ResourceUtils#isUrl
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
		if (path.contains("../")) {
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

	/**
	 * Determine an appropriate media type for the given resource.
	 * @param resource the resource to check
	 * @return the corresponding media type, or {@code null} if none found
	 */
	protected MediaType getMediaType(Resource resource) {
		MediaType mediaType = null;
		String mimeType = getServletContext().getMimeType(resource.getFilename());
		if (StringUtils.hasText(mimeType)) {
			mediaType = MediaType.parseMediaType(mimeType);
		}
		if (jafPresent && (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType))) {
			MediaType jafMediaType = ActivationMediaTypeFactory.getMediaType(resource.getFilename());
			if (jafMediaType != null && !MediaType.APPLICATION_OCTET_STREAM.equals(jafMediaType)) {
				mediaType = jafMediaType;
			}
		}
		return mediaType;
	}

	/**
	 * Set headers on the given servlet response.
	 * Called for GET requests as well as HEAD requests.
	 * @param response current servlet response
	 * @param resource the identified resource (never {@code null})
	 * @param mediaType the resource's media type (never {@code null})
	 * @throws IOException in case of errors while setting the headers
	 */
	protected void setHeaders(HttpServletResponse response, Resource resource, MediaType mediaType) throws IOException {
		long length = resource.contentLength();
		if (length > Integer.MAX_VALUE) {
			throw new IOException("Resource content too long (beyond Integer.MAX_VALUE): " + resource);
		}
		response.setContentLength((int) length);
		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}
		if (resource instanceof EncodedResource) {
			response.setHeader(HttpHeaders.CONTENT_ENCODING, ((EncodedResource) resource).getContentEncoding());
		}
		if (resource instanceof VersionedResource) {
			response.setHeader(HttpHeaders.ETAG, "\"" + ((VersionedResource) resource).getVersion() + "\"");
		}
		response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
	}

	/**
	 * Write the actual content out to the given servlet response,
	 * streaming the resource's content.
	 * @param response current servlet response
	 * @param resource the identified resource (never {@code null})
	 * @throws IOException in case of errors while writing the content
	 */
	protected void writeContent(HttpServletResponse response, Resource resource) throws IOException {
		try {
			InputStream in = resource.getInputStream();
			try {
				StreamUtils.copy(in, response.getOutputStream());
			}
			catch (NullPointerException ex) {
				// ignore, see SPR-13620
			}
			finally {
				try {
					in.close();
				}
				catch (Throwable ex) {
					// ignore, see SPR-12999
				}
			}
		}
		catch (FileNotFoundException ex) {
			// ignore, see SPR-12999
		}
	}

	/**
	 * Write parts of the resource as indicated by the request {@code Range} header.
	 * @param request current servlet request
	 * @param response current servlet response
	 * @param resource the identified resource (never {@code null})
	 * @param contentType the content type
	 * @throws IOException in case of errors while writing the content
	 */
	protected void writePartialContent(HttpServletRequest request, HttpServletResponse response,
			Resource resource, MediaType contentType) throws IOException {

		long length = resource.contentLength();

		List<HttpRange> ranges;
		try {
			HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
			ranges = headers.getRange();
		}
		catch (IllegalArgumentException ex) {
			response.addHeader("Content-Range", "bytes */" + length);
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
		}

		response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

		if (ranges.size() == 1) {
			HttpRange range = ranges.get(0);

			long start = range.getRangeStart(length);
			long end = range.getRangeEnd(length);
			long rangeLength = end - start + 1;

			setHeaders(response, resource, contentType);
			response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            response.setContentLength((int) rangeLength);

			InputStream in = resource.getInputStream();
			try {
				copyRange(in, response.getOutputStream(), start, end);
			}
			finally {
				try {
					in.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
		else {
			String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
			response.setContentType("multipart/byteranges; boundary=" + boundaryString);

			ServletOutputStream out = response.getOutputStream();

			for (HttpRange range : ranges) {
				long start = range.getRangeStart(length);
				long end = range.getRangeEnd(length);

				InputStream in = resource.getInputStream();

                // Writing MIME header.
                out.println();
                out.println("--" + boundaryString);
                if (contentType != null) {
	                out.println("Content-Type: " + contentType);
                }
                out.println("Content-Range: bytes " + start + "-" + end + "/" + length);
                out.println();

                // Printing content
                copyRange(in, out, start, end);
			}
			out.println();
            out.print("--" + boundaryString + "--");
		}
	}

	private void copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {
		long skipped = in.skip(start);
		if (skipped < start) {
			throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required.");
		}

		long bytesToCopy = end - start + 1;
		byte buffer[] = new byte[StreamUtils.BUFFER_SIZE];
		while (bytesToCopy > 0) {
			int bytesRead = in.read(buffer);
			if (bytesRead <= bytesToCopy) {
				out.write(buffer, 0, bytesRead);
				bytesToCopy -= bytesRead;
			}
			else {
				out.write(buffer, 0, (int) bytesToCopy);
				bytesToCopy = 0;
			}
			if (bytesRead == -1) {
				break;
			}
		}
	}


	@Override
	public String toString() {
		return "ResourceHttpRequestHandler [locations=" + getLocations() + ", resolvers=" + getResourceResolvers() + "]";
	}


	/**
	 * Inner class to avoid a hard-coded JAF dependency.
	 */
	private static class ActivationMediaTypeFactory {

		private static final FileTypeMap fileTypeMap;

		static {
			fileTypeMap = loadFileTypeMapFromContextSupportModule();
		}

		private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
			// See if we can find the extended mime.types from the context-support module...
			Resource mappingLocation = new ClassPathResource("org/springframework/mail/javamail/mime.types");
			if (mappingLocation.exists()) {
				InputStream inputStream = null;
				try {
					inputStream = mappingLocation.getInputStream();
					return new MimetypesFileTypeMap(inputStream);
				}
				catch (IOException ex) {
					// ignore
				}
				finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						}
						catch (IOException ex) {
							// ignore
						}
					}
				}
			}
			return FileTypeMap.getDefaultFileTypeMap();
		}

		public static MediaType getMediaType(String filename) {
			String mediaType = fileTypeMap.getContentType(filename);
			return (StringUtils.hasText(mediaType) ? MediaType.parseMediaType(mediaType) : null);
		}
	}

}
