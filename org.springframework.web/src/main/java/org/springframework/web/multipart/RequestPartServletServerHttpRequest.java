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

package org.springframework.web.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * {@link ServerHttpRequest} implementation that is based on a part of a {@link MultipartHttpServletRequest}.
 * The part is accessed as {@link MultipartFile} and adapted to the ServerHttpRequest contract.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class RequestPartServletServerHttpRequest implements ServerHttpRequest {

	private final MultipartHttpServletRequest request;
	
	private final MultipartFile multipartFile;

	private HttpHeaders headers;

	/**
	 * Creates a new {@link RequestPartServletServerHttpRequest} instance.
	 * 
	 * @param request the multipart request.
	 * @param name the name of the part to adapt to the {@link ServerHttpRequest} contract.
	 */
	public RequestPartServletServerHttpRequest(MultipartHttpServletRequest request, String name) {
		this.request = request;
		this.multipartFile = request.getFile(name);
		Assert.notNull(multipartFile, "Request part named '" + name + "' not found. " + 
				"Available request part names: " + request.getMultiFileMap().keySet());

	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.request.getMethod());
	}

	public URI getURI() {
		try {
			return new URI(this.request.getScheme(), null, this.request.getServerName(),
					this.request.getServerPort(), this.request.getRequestURI(),
					this.request.getQueryString(), null);
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpServletRequest URI: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Returns the headers associated with the part of the multi-part request associated with this instance. 
	 * If the underlying implementation supports access to headers, then all headers are returned. 
	 * Otherwise, the returned headers will have a 'Content-Type' header in the very least.
	 */
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			Iterator<String> iterator = this.multipartFile.getHeaderNames();
			while (iterator.hasNext()) {
				String name = iterator.next();
				String[] values = this.multipartFile.getHeaders(name);
				for (String value : values) {
					this.headers.add(name, value);
				}
			}
		}
		return this.headers;
	}

	public InputStream getBody() throws IOException {
		return this.multipartFile.getInputStream();
	}

}
