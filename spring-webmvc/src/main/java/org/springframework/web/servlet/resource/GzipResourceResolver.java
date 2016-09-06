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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

/**
 * A {@code ResourceResolver} that delegates to the chain to locate a resource
 * and then attempts to find a variation with the ".gz" extension.
 *
 * <p>The resolver gets involved only if the "Accept-Encoding" request header
 * contains the value "gzip" indicating the client accepts gzipped responses.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class GzipResourceResolver extends AbstractResourceResolver {

	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resource = chain.resolveResource(request, requestPath, locations);
		if ((resource == null) || (request != null && !isGzipAccepted(request))) {
			return resource;
		}

		try {
			Resource gzipped = new GzippedResource(resource);
			if (gzipped.exists()) {
				return gzipped;
			}
		}
		catch (IOException ex) {
			logger.trace("No gzipped resource for [" + resource.getFilename() + "]", ex);
		}

		return resource;
	}

	private boolean isGzipAccepted(HttpServletRequest request) {
		String value = request.getHeader("Accept-Encoding");
		return (value != null && value.toLowerCase().contains("gzip"));
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations);
	}


	private static final class GzippedResource extends AbstractResource implements ResolvedResource {

		private final Resource original;

		private final Resource gzipped;

		public GzippedResource(Resource original) throws IOException {
			this.original = original;
			this.gzipped = original.createRelative(original.getFilename() + ".gz");
		}

		public InputStream getInputStream() throws IOException {
			return this.gzipped.getInputStream();
		}

		public boolean exists() {
			return this.gzipped.exists();
		}

		public boolean isReadable() {
			return this.gzipped.isReadable();
		}

		public boolean isOpen() {
			return this.gzipped.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.gzipped.isFile();
		}

		public URL getURL() throws IOException {
			return this.gzipped.getURL();
		}

		public URI getURI() throws IOException {
			return this.gzipped.getURI();
		}

		public File getFile() throws IOException {
			return this.gzipped.getFile();
		}

		public long contentLength() throws IOException {
			return this.gzipped.contentLength();
		}

		public long lastModified() throws IOException {
			return this.gzipped.lastModified();
		}

		public Resource createRelative(String relativePath) throws IOException {
			return this.gzipped.createRelative(relativePath);
		}

		public String getFilename() {
			return this.original.getFilename();
		}

		public String getDescription() {
			return this.gzipped.getDescription();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			HttpHeaders headers;
			if(this.original instanceof ResolvedResource) {
				headers = ((ResolvedResource) this.original).getResponseHeaders();
			}
			else {
				headers = new HttpHeaders();
			}
			headers.add(HttpHeaders.CONTENT_ENCODING, "gzip");
			return headers;
		}

	}

}
