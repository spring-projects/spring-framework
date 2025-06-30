/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

/**
 * A filter that wraps the {@link HttpServletResponse} and overrides its
 * {@link HttpServletResponse#encodeURL(String) encodeURL} method in order to
 * translate internal resource request URLs into public URL paths for external use.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
public class ResourceUrlEncodingFilter extends GenericFilterBean {

	private static final Log logger = LogFactory.getLog(ResourceUrlEncodingFilter.class);


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
			throw new ServletException("ResourceUrlEncodingFilter only supports HTTP requests");
		}

		ResourceUrlEncodingRequestWrapper wrappedRequest =
				new ResourceUrlEncodingRequestWrapper(httpRequest);
		ResourceUrlEncodingResponseWrapper wrappedResponse =
				new ResourceUrlEncodingResponseWrapper(wrappedRequest, httpResponse);

		filterChain.doFilter(wrappedRequest, wrappedResponse);
	}


	private static class ResourceUrlEncodingRequestWrapper extends HttpServletRequestWrapper {

		private @Nullable ResourceUrlProvider resourceUrlProvider;

		private @Nullable Integer indexLookupPath;

		private String prefixLookupPath = "";

		ResourceUrlEncodingRequestWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public void setAttribute(String name, Object value) {
			super.setAttribute(name, value);
			if (ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR.equals(name)) {
				if (value instanceof ResourceUrlProvider urlProvider) {
					initLookupPath(urlProvider);
				}
			}
		}

		@SuppressWarnings("removal")
		private void initLookupPath(ResourceUrlProvider urlProvider) {
			this.resourceUrlProvider = urlProvider;
			if (this.indexLookupPath == null) {
				UrlPathHelper pathHelper = this.resourceUrlProvider.getUrlPathHelper();
				String requestUri = pathHelper.getRequestUri(this);
				String lookupPath = pathHelper.getLookupPathForRequest(this);
				this.indexLookupPath = requestUri.lastIndexOf(lookupPath);
				if (this.indexLookupPath == -1) {
					throw new LookupPathIndexException(lookupPath, requestUri);
				}
				this.prefixLookupPath = requestUri.substring(0, this.indexLookupPath);
				if (StringUtils.matchesCharacter(lookupPath, '/') && !StringUtils.matchesCharacter(requestUri, '/')) {
					String contextPath = pathHelper.getContextPath(this);
					if (requestUri.equals(contextPath)) {
						this.indexLookupPath = requestUri.length();
						this.prefixLookupPath = requestUri;
					}
				}
			}
		}

		public @Nullable String resolveUrlPath(String url) {
			if (this.resourceUrlProvider == null) {
				logger.trace("ResourceUrlProvider not available via request attribute " +
						ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
				return null;
			}
			if (this.indexLookupPath != null && url.startsWith(this.prefixLookupPath)) {
				int suffixIndex = getEndPathIndex(url);
				String suffix = url.substring(suffixIndex);
				String lookupPath = url.substring(this.indexLookupPath, suffixIndex);
				lookupPath = this.resourceUrlProvider.getForLookupPath(lookupPath);
				if (lookupPath != null) {
					return this.prefixLookupPath + lookupPath + suffix;
				}
			}
			return null;
		}

		private int getEndPathIndex(String path) {
			int end = path.indexOf('?');
			int fragmentIndex = path.indexOf('#');
			if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
				end = fragmentIndex;
			}
			if (end == -1) {
				end = path.length();
			}
			return end;
		}
	}


	private static class ResourceUrlEncodingResponseWrapper extends HttpServletResponseWrapper {

		private final ResourceUrlEncodingRequestWrapper request;

		ResourceUrlEncodingResponseWrapper(ResourceUrlEncodingRequestWrapper request, HttpServletResponse wrapped) {
			super(wrapped);
			this.request = request;
		}

		@Override
		public String encodeURL(String url) {
			String urlPath = this.request.resolveUrlPath(url);
			if (urlPath != null) {
				return super.encodeURL(urlPath);
			}
			return super.encodeURL(url);
		}
	}


	/**
	 * Runtime exception to get far enough (to ResourceUrlProviderExposingInterceptor)
	 * where it can be re-thrown as ServletRequestBindingException to result in
	 * a 400 response.
	 */
	@SuppressWarnings("serial")
	static class LookupPathIndexException extends IllegalArgumentException {

		LookupPathIndexException(String lookupPath, String requestUri) {
			super("Failed to find lookupPath '" + lookupPath + "' within requestUri '" + requestUri + "'. " +
					"This could be because the path has invalid encoded characters or isn't normalized.");
		}
	}

}
