/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.multipart.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ClassUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * {@link ServerHttpRequest} implementation that accesses one part of a multipart
 * request. If using {@link MultipartResolver} configuration the part is accessed
 * through a {@link MultipartFile}. Or if using Servlet 3.0 multipart processing
 * the part is accessed through {@code ServletRequest.getPart}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class RequestPartServletServerHttpRequest extends ServletServerHttpRequest {

	private final MultipartHttpServletRequest multipartRequest;

	private final String partName;

	private final HttpHeaders headers;


	/**
	 * Create a new instance.
	 * @param request the current request
	 * @param partName the name of the part to adapt to the {@link ServerHttpRequest} contract
	 * @throws MissingServletRequestPartException if the request part cannot be found
	 * @throws IllegalArgumentException if MultipartHttpServletRequest cannot be initialized
	 */
	public RequestPartServletServerHttpRequest(HttpServletRequest request, String partName)
			throws MissingServletRequestPartException {

		super(request);

		this.multipartRequest = asMultipartRequest(request);
		this.partName = partName;

		this.headers = this.multipartRequest.getMultipartHeaders(this.partName);
		if (this.headers == null) {
			if (request instanceof MultipartHttpServletRequest) {
				throw new MissingServletRequestPartException(partName);
			}
			else {
				throw new IllegalArgumentException(
						"Failed to obtain request part: " + partName + ". " +
						"The part is missing or multipart processing is not configured. " +
						"Check for a MultipartResolver bean or if Servlet 3.0 multipart processing is enabled.");
			}
		}
	}

	private static MultipartHttpServletRequest asMultipartRequest(HttpServletRequest request) {
		if (request instanceof MultipartHttpServletRequest) {
			return (MultipartHttpServletRequest) request;
		}
		else if (ClassUtils.hasMethod(HttpServletRequest.class, "getParts")) {
			// Servlet 3.0 available ..
			return new StandardMultipartHttpServletRequest(request);
		}
		throw new IllegalArgumentException("Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		if (this.multipartRequest instanceof StandardMultipartHttpServletRequest) {
			try {
				return this.multipartRequest.getPart(this.partName).getInputStream();
			}
			catch (Exception ex) {
				throw new MultipartException("Could not parse multipart servlet request", ex);
			}
		}
		else {
			MultipartFile file = this.multipartRequest.getFile(this.partName);
			if (file != null) {
				return file.getInputStream();
			}
			else {
				String paramValue = this.multipartRequest.getParameter(this.partName);
				return new ByteArrayInputStream(paramValue.getBytes(FORM_CHARSET));
			}
		}
	}

}
