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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * Standard implementation of the {@link MultipartResolver} interface,
 * based on the Servlet 3.0 {@link javax.servlet.http.Part} API.
 * To be added as "multipartResolver" bean to a Spring DispatcherServlet context,
 * without any extra configuration at the bean level (see below).
 *
 * <p><b>Note:</b> In order to use Servlet 3.0 based multipart parsing,
 * you need to mark the affected servlet with a "multipart-config" section in
 * <code>web.xml</code>, or with a {@link javax.servlet.MultipartConfigElement}
 * in programmatic servlet registration, or (in case of a custom servlet class)
 * possibly with a {@link javax.servlet.annotation.MultipartConfig} annotation
 * on your servlet class. Configuration settings such as maximum sizes or
 * storage locations need to be applied at that servlet registration level;
 * Servlet 3.0 does not allow for them to be set at the MultipartResolver level.
 *
 * @author Juergen Hoeller
 * @since 3.1
 */
public class StandardServletMultipartResolver implements MultipartResolver {

	protected final Log logger = LogFactory.getLog(getClass());


	public boolean isMultipart(HttpServletRequest request) {
		// Same check as in Commons FileUpload...
		if (!"post".equals(request.getMethod().toLowerCase())) {
			return false;
		}
		String contentType = request.getContentType();
		return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
	}

	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		return new StandardMultipartHttpServletRequest(request);
	}

	public void cleanupMultipart(MultipartHttpServletRequest request) {
		// To be on the safe side: explicitly delete all parts.
		try {
			for (Part part : request.getParts()) {
				part.delete();
			}
		}
		catch (Exception ex) {
			logger.warn("Failed to perform cleanup of multipart items", ex);
		}
	}


	/**
	 * Spring MultipartHttpServletRequest adapter, wrapping a Servlet 3.0 HttpServletRequest
	 * and its Part objects. Parameters get exposed through the native request's getParameter
	 * methods - without any custom processing on our side.
	 */
	private static class StandardMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

		public StandardMultipartHttpServletRequest(HttpServletRequest request) throws MultipartException {
			super(request);
			try {
				Collection<Part> parts = request.getParts();
				MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<String, MultipartFile>(parts.size());
				for (Part part : parts) {
					files.add(part.getName(), new StandardMultipartFile(part));
				}
				setMultipartFiles(files);
			}
			catch (Exception ex) {
				throw new MultipartException("Could not parse multipart servlet request", ex);
			}
		}
	}


	/**
	 * Spring MultipartFile adapter, wrapping a Servlet 3.0 Part object.
	 */
	private static class StandardMultipartFile implements MultipartFile {

		private final Part part;

		public StandardMultipartFile(Part part) {
			this.part = part;
		}

		public String getName() {
			return this.part.getName();
		}

		public String getOriginalFilename() {
			return null;  // not supported in Servlet 3.0 - switch to Commons FileUpload if you need this
		}

		public String getContentType() {
			return this.part.getContentType();
		}

		public boolean isEmpty() {
			return (this.part.getSize() == 0);
		}

		public long getSize() {
			return this.part.getSize();
		}

		public byte[] getBytes() throws IOException {
			return FileCopyUtils.copyToByteArray(this.part.getInputStream());
		}

		public InputStream getInputStream() throws IOException {
			return this.part.getInputStream();
		}

		public void transferTo(File dest) throws IOException, IllegalStateException {
			this.part.write(dest.getPath());
		}
	}

}
