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

package org.springframework.web.multipart.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
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

	private final String requestPartName;

	private final HttpHeaders multipartHeaders;


	/**
	 * Create a new {@code RequestPartServletServerHttpRequest} instance.
	 * @param request the current servlet request
	 * @param requestPartName the name of the part to adapt to the {@link ServerHttpRequest} contract
	 * @throws MissingServletRequestPartException if the request part cannot be found
	 * @throws MultipartException if MultipartHttpServletRequest cannot be initialized
	 */
	public RequestPartServletServerHttpRequest(HttpServletRequest request, String requestPartName)
			throws MissingServletRequestPartException {

		super(request);

		this.multipartRequest = MultipartResolutionDelegate.asMultipartHttpServletRequest(request);
		this.requestPartName = requestPartName;

		HttpHeaders multipartHeaders = this.multipartRequest.getMultipartHeaders(requestPartName);
		if (multipartHeaders == null) {
			throw new MissingServletRequestPartException(requestPartName);
		}
		this.multipartHeaders = multipartHeaders;
	}


	@Override
	public HttpHeaders getHeaders() {
		return this.multipartHeaders;
	}

	@Override
	public InputStream getBody() throws IOException {
		// Prefer Servlet Part resolution to cover file as well as parameter streams
		if (this.multipartRequest instanceof StandardMultipartHttpServletRequest) {
			try {
				return this.multipartRequest.getPart(this.requestPartName).getInputStream();
			}
			catch (Exception ex) {
				throw new MultipartException("Failed to retrieve request part '" + this.requestPartName + "'", ex);
			}
		}

		// Spring-style distinction between MultipartFile and String parameters
		MultipartFile file = this.multipartRequest.getFile(this.requestPartName);
		if (file != null) {
			return file.getInputStream();
		}
		String paramValue = this.multipartRequest.getParameter(this.requestPartName);
		if (paramValue != null) {
			return new ByteArrayInputStream(paramValue.getBytes(determineEncoding()));
		}

		throw new IllegalStateException("No body available for request part '" + this.requestPartName + "'");
	}

	private String determineEncoding() {
		MediaType contentType = getHeaders().getContentType();
		if (contentType != null) {
			Charset charset = contentType.getCharset();
			if (charset != null) {
				return charset.name();
			}
		}
		String encoding = this.multipartRequest.getCharacterEncoding();
		return (encoding != null ? encoding : FORM_CHARSET);
	}

}
