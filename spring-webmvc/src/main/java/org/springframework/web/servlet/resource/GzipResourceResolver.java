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
 * 
 * @author Jeremy Grelle
 */
public class GzipResourceResolver extends AbstractResourceResolver {

	private final Log logger = LogFactory.getLog(getClass());
	
	@Override
	protected Resource resolveInternal(HttpServletRequest request, String path,
			List<Resource> locations, ResourceResolverChain chain, Resource resolved) {
		
		if (!isGzipAccepted(request) || resolved == null) {
			return resolved;
		}
		
		try {
			Resource gzipped = new GzippedResource(resolved);
			if (gzipped.exists()) {
				return gzipped;
			}
		} catch (IOException e) {
			this.logger.trace("Error occurred locating gzipped resource", e);
		}
		return resolved;
	}

	/**
	 * @param request 
	 * @return
	 */
	private boolean isGzipAccepted(HttpServletRequest request) {
		String val = request.getHeader("Accept-Encoding");
		return val != null && val.toLowerCase().contains("gzip");
	}
	
	private static final class GzippedResource extends AbstractResource implements EncodedResource {

		private final Resource original;
		
		private final Resource gzipped;
		
		public GzippedResource(Resource original) throws IOException {
			this.original = original;
			this.gzipped = original.createRelative(original.getFilename()+".gz");
		}

		public InputStream getInputStream() throws IOException {
			return gzipped.getInputStream();
		}

		public boolean exists() {
			return gzipped.exists();
		}

		public boolean isReadable() {
			return gzipped.isReadable();
		}

		public boolean isOpen() {
			return gzipped.isOpen();
		}

		public URL getURL() throws IOException {
			return gzipped.getURL();
		}

		public URI getURI() throws IOException {
			return gzipped.getURI();
		}

		public File getFile() throws IOException {
			return gzipped.getFile();
		}

		public long contentLength() throws IOException {
			return gzipped.contentLength();
		}

		public long lastModified() throws IOException {
			return gzipped.lastModified();
		}

		public Resource createRelative(String relativePath) throws IOException {
			return gzipped.createRelative(relativePath);
		}

		public String getFilename() {
			return original.getFilename();
		}

		public String getDescription() {
			return gzipped.getDescription();
		}

		public String getEncoding() {
			return "gzip";
		}

	}	

}
