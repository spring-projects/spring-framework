/*
 * Copyright 2002-2022 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

/**
 * Standard implementation of the {@link MultipartResolver} interface,
 * based on the Servlet {@link jakarta.servlet.http.Part} API.
 * To be added as "multipartResolver" bean to a Spring DispatcherServlet context,
 * without any extra configuration at the bean level (see below).
 *
 * <p>This resolver variant uses your Servlet container's multipart parser as-is,
 * potentially exposing the application to container implementation differences.
 * Also, see this resolver's configuration option for
 * {@linkplain #setStrictServletCompliance strict Servlet compliance}, narrowing the
 * applicability of Spring's {@link MultipartHttpServletRequest} to form data only.
 *
 * <p><b>Note:</b> In order to use Servlet container based multipart parsing,
 * you need to mark the affected servlet with a "multipart-config" section in
 * {@code web.xml}, or with a {@link jakarta.servlet.MultipartConfigElement}
 * in programmatic servlet registration, or (in case of a custom servlet class)
 * possibly with a {@link jakarta.servlet.annotation.MultipartConfig} annotation
 * on your servlet class. Configuration settings such as maximum sizes or storage
 * locations need to be applied at that servlet registration level; a Servlet
 * container does not allow for them to be set at the MultipartResolver level.
 *
 * <pre class="code">
 * public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
 *   // ...
 *   &#064;Override
 *   protected void customizeRegistration(ServletRegistration.Dynamic registration) {
 *     // Optionally also set maxFileSize, maxRequestSize, fileSizeThreshold
 *     registration.setMultipartConfig(new MultipartConfigElement("/tmp"));
 *   }
 * }</pre>
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setResolveLazily
 * @see #setStrictServletCompliance
 * @see HttpServletRequest#getParts()
 */
public class StandardServletMultipartResolver implements MultipartResolver {

	private boolean resolveLazily = false;

	private boolean strictServletCompliance = false;


	/**
	 * Set whether to resolve the multipart request lazily at the time of
	 * file or parameter access.
	 * <p>Default is "false", resolving the multipart elements immediately, throwing
	 * corresponding exceptions at the time of the {@link #resolveMultipart} call.
	 * Switch this to "true" for lazy multipart parsing, throwing parse exceptions
	 * once the application attempts to obtain multipart files or parameters.
	 * @since 3.2.9
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * Specify whether this resolver should strictly comply with the Servlet
	 * specification, only kicking in for "multipart/form-data" requests.
	 * <p>Default is "false", trying to process any request with a "multipart/"
	 * content type as far as the underlying Servlet container supports it
	 * (which works on e.g. Tomcat but not on Jetty). For consistent portability
	 * and in particular for consistent custom handling of non-form multipart
	 * request types outside of Spring's {@link MultipartResolver} mechanism,
	 * switch this flag to "true": Only "multipart/form-data" requests will be
	 * wrapped with a {@link MultipartHttpServletRequest} then; other kinds of
	 * requests will be left as-is, allowing for custom processing in user code.
	 * @since 5.3.9
	 */
	public void setStrictServletCompliance(boolean strictServletCompliance) {
		this.strictServletCompliance = strictServletCompliance;
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return StringUtils.startsWithIgnoreCase(request.getContentType(),
				(this.strictServletCompliance ? MediaType.MULTIPART_FORM_DATA_VALUE : "multipart/"));
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest abstractMultipartHttpServletRequest) ||
				abstractMultipartHttpServletRequest.isResolved()) {
			// To be on the safe side: explicitly delete the parts,
			// but only actual file parts (for Resin compatibility)
			try {
				for (Part part : request.getParts()) {
					if (request.getFile(part.getName()) != null) {
						part.delete();
					}
				}
			}
			catch (Throwable ex) {
				LogFactory.getLog(getClass()).warn("Failed to perform cleanup of multipart items", ex);
			}
		}
	}

}
