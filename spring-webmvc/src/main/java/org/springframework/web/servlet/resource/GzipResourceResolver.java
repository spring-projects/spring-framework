/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;


/**
 * A {@link ResourceResolver} that lets the next resolver in the chain locate a Resource
 * and then attempts to find a variation of that Resource with ".gz" extension. This
 * resolver will only get involved if the client has indicated it supports gzipped
 * responses through the "Accept-Encoding" header.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class GzipResourceResolver extends AbstractResourceResolver {

	private static final Log logger = LogFactory.getLog(GzipResourceResolver.class);


	@Override
	protected Resource resolveInternal(HttpServletRequest request, String path,
			List<Resource> locations, ResourceResolverChain chain, Resource resource) {

		if ((resource == null) || !isGzipAccepted(request)) {
			return resource;
		}

		try {
			Resource gzipped = new GzippedResource(resource);
			if (gzipped.exists()) {
				return gzipped;
			}
		}
		catch (IOException e) {
			logger.trace("No gzipped resource for " + resource.getFilename(), e);
		}

		return resource;
	}

	private boolean isGzipAccepted(HttpServletRequest request) {
		String value = request.getHeader("Accept-Encoding");
		return ((value != null) && value.toLowerCase().contains("gzip"));
	}


	private static final class GzippedResource extends AbstractResource implements EncodedResource {

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

		public String getContentEncoding() {
			return "gzip";
		}
	}

}
