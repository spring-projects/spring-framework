/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * {@link HttpRequestHandler} that serves static resources optimized for superior browser performance 
 * (according to the guidelines of Page Speed, YSlow, etc.) by allowing for flexible cache settings
 * ({@link #setCacheSeconds "cacheSeconds" property}, last-modified support).
 *
 * <p>The {@link #setLocations "locations" property takes a list of Spring {@link Resource} locations
 * from which static resources are allowed  to be served by this handler. For a given request, the
 * list of locations will be consulted in order for the presence of the requested resource, and the
 * first found match will be written to the response, with {@code Expires} and {@code Cache-Control}
 * headers set as configured. The handler also properly evaluates the {@code Last-Modified} header
 * (if present) so that a {@code 304} status code will be returned as appropriate, avoiding unnecessary
 * overhead for resources that are already cached by the client. The use of {@code Resource} locations
 * allows resource requests to easily be mapped to locations other than the web application root. For
 * example, resources could be served from a classpath location such as "classpath:/META-INF/public-web-resources/", 
 * allowing convenient packaging and serving of resources such as a JavaScript library from within jar files.
 *
 * <p>To ensure that users with a primed browser cache get the latest changes to application-specific
 * resources upon deployment of new versions of the application, it is recommended that a version string
 * is used in the URL  mapping pattern that selects this handler. Such patterns can be easily parameterized
 * using Spring EL. See the reference manual for further examples of this approach.
 *
 * <p>Rather than being directly configured as a bean, this handler will typically be configured
 * through use of the <code>&lt;mvc:resources/&gt;</code> XML configuration element.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler extends WebContentGenerator implements HttpRequestHandler {

	private List<Resource> locations;


	public ResourceHttpRequestHandler() {
		super(METHOD_GET, METHOD_HEAD);
	}

	/**
	 * Set a {@code List} of {@code Resource} paths to use as sources
	 * for serving static resources.
	 */
	public void setLocations(List<Resource> locations) {
		Assert.notEmpty(locations, "Location list must not be empty");
		this.locations = locations;
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
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		checkAndPrepare(request, response, true);

		// check whether a matching resource exists
		Resource resource = getResource(request);
		if (resource == null) {
			logger.debug("No matching resource found - returning 404");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// check the resource's media type
		MediaType mediaType = getMediaType(resource);
		if (mediaType != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Determined media type [" + mediaType + "] for " + resource);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("No media type found for " + resource + " - returning 404");
			}
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// header phase
		setHeaders(response, resource, mediaType);
		if (new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
			logger.debug("Resource not modified - returning 304");
			return;
		}

		// content phase
		if (METHOD_HEAD.equals(request.getMethod())) {
			logger.trace("HEAD request - skipping content");
			return;
		}
		writeContent(response, resource);
	}

	protected Resource getResource(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" +
					HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}

		if (!StringUtils.hasText(path) || path.contains("WEB-INF") || path.contains("META-INF")) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring invalid resource path [" + path + "]");
			}
			return null;
		}

		for (Resource location : this.locations) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Trying relative path [" + path + "] against base location: " + location);
				}
				Resource resource = location.createRelative(path);
				if (resource.exists() && resource.isReadable()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Found matching resource: " + resource);
					}
					return resource;
				}
				else if (logger.isTraceEnabled()) {
					logger.trace("Relative resource doesn't exist or isn't readable: " + resource);
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to create relative resource - trying next resource location", ex);
			}
		}
		return null;
	}

	/**
	 * Determine an appropriate media type for the given resource.
	 * @param resource the resource to check
	 * @return the corresponding media type, or <code>null</code> if none found
	 */
	protected MediaType getMediaType(Resource resource) {
		String mimeType = getServletContext().getMimeType(resource.getFilename());
		return (StringUtils.hasText(mimeType) ? MediaType.parseMediaType(mimeType) : null);
	}

	/**
	 * Set headers on the given servlet response.
	 * Called for GET requests as well as HEAD requests.
	 * @param response current servlet response
	 * @param resource the identified resource (never <code>null</code>)
	 * @param mediaType the resource's media type (never <code>null</code>)
	 * @throws IOException in case of errors while setting the headers
	 */
	protected void setHeaders(HttpServletResponse response, Resource resource, MediaType mediaType) throws IOException {
		long length = resource.contentLength();
		if (length > Integer.MAX_VALUE) {
			throw new IOException("Resource content too long (beyond Integer.MAX_VALUE): " + resource);
		}
		response.setContentLength((int) length);
		response.setContentType(mediaType.toString());
	}

	/**
	 * Write the actual content out to the given servlet response,
	 * streaming the resource's content.
	 * @param response current servlet response
	 * @param resource the identified resource (never <code>null</code>)
	 * @throws IOException in case of errors while writing the content
	 */
	protected void writeContent(HttpServletResponse response, Resource resource) throws IOException {
		FileCopyUtils.copy(resource.getInputStream(), response.getOutputStream());
	}

}
