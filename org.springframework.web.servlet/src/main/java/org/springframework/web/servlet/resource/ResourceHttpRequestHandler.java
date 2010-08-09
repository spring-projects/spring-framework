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
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * {@link HttpRequestHandler} that serves static resources optimized for superior browser performance 
 * (according to the guidelines of Page Speed, YSlow, etc.) by adding far future cache expiration headers.
 *
 * <p>The constructor takes a list of Spring {@link Resource} locations from which static resources are allowed 
 * to be served by this handler. For a given request, the list of locations will be consulted in order for the
 * presence of the requested resource, and the first found match will be written to the response, with {@code 
 * Expires} and {@code Cache-Control} headers set for one year in the future. The handler also properly evaluates
 * the {@code Last-Modified} header (if present) so that a {@code 304} status code will be returned as appropriate, 
 * avoiding unnecessary overhead for resources that are already cached by the client. The use of {@code Resource}
 * locations allows resource requests to easily be mapped to locations other than the web application root. For
 * example, resources could be served from a classpath location such as "classpath:/META-INF/public-web-resources/", 
 * allowing convenient packaging and serving of resources such as a JavaScript library from within jar files.
 *
 * <p>To ensure that users with a primed browser cache get the latest changes to application-specific resources 
 * upon deployment of new versions of the application, it is recommended that a version string is used in the URL 
 * mapping pattern that selects this handler.  Such patterns can be easily parameterized using Spring EL. See the
 * reference manual for further examples of this approach.  
 *
 * <p>Rather than being directly configured as a bean, this handler will typically be configured through use of 
 * the <code>&lt;mvc:resources/&gt;</code> Spring configuration tag. 
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler extends WebContentGenerator implements HttpRequestHandler {

	private List<Resource> locations;


	public ResourceHttpRequestHandler() {
		super(METHOD_GET);
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
		Resource resource = getResource(request);
		if (resource == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (checkNotModified(resource, request, response)) {
			return;
		}
		writeResponse(resource, response);
	}

	private Resource getResource(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" +
					HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}
		if (!StringUtils.hasText(path) || path.contains("WEB-INF") || path.contains("META-INF")) {
			return null;
		}
		for (Resource resourcePath : this.locations) {
			try {
				Resource resource = resourcePath.createRelative(path);
				if (resource.exists() && resource.isReadable()) {
					return resource;
				}
			}
			catch (IOException ex) {
				// resource not found
				return null;
			}
		}
		return null;
	}

	private boolean checkNotModified(Resource resource,HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		long ifModifiedSince = request.getDateHeader("If-Modified-Since");
		long lastModified = resource.lastModified();
		boolean notModified = ifModifiedSince >= (lastModified / 1000 * 1000);
		if (notModified) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		}
		else {
			response.setDateHeader("Last-Modified", lastModified);
		}
		return notModified;
	}

	private void writeResponse(Resource resource, HttpServletResponse response) throws IOException {
		MediaType mediaType = getMediaType(resource);
		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}
		long length = resource.contentLength();
		if (length > Integer.MAX_VALUE) {
			throw new IOException("Resource content too long (beyond Integer.MAX_VALUE): " + resource);
		}
		response.setContentLength((int) length);
		FileCopyUtils.copy(resource.getInputStream(), response.getOutputStream());
	}

	protected MediaType getMediaType(Resource resource) {
		String mimeType = getServletContext().getMimeType(resource.getFilename());
		return (StringUtils.hasText(mimeType) ? MediaType.parseMediaType(mimeType) : null);
	}

}
