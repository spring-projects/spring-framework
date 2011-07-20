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
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * {@link ServerHttpRequest} implementation that is based on a part of a {@link MultipartHttpServletRequest}.
 * The part is accessed as {@link MultipartFile} and adapted to the ServerHttpRequest contract.
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
	 * Create a new {@link RequestPartServletServerHttpRequest} instance.
	 * @param request the multipart request
	 * @param partName the name of the part to adapt to the {@link ServerHttpRequest} contract
	 */
	public RequestPartServletServerHttpRequest(HttpServletRequest request, String partName) {
		super(request);

		this.multipartRequest = (request instanceof MultipartHttpServletRequest ?
				(MultipartHttpServletRequest) request : new StandardMultipartHttpServletRequest(request));
		this.partName = partName;

		this.headers = this.multipartRequest.getMultipartHeaders(this.partName);
		if (this.headers == null) {
			throw new IllegalArgumentException("No request part found for name '" + this.partName + "'");
		}
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
