/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

/**
 * A filter that wraps the {@link HttpServletResponse} and overrides its
 * {@link HttpServletResponse#encodeURL(String) encodeURL} method in order to
 * translate internal resource request URLs into public URL paths for external
 * use.
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
			throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("ResourceUrlEncodingFilter just supports HTTP requests");
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		filterChain.doFilter(httpRequest, new ResourceUrlEncodingResponseWrapper(httpRequest, httpResponse));
	}


	private static class ResourceUrlEncodingResponseWrapper extends HttpServletResponseWrapper {

		private final HttpServletRequest request;

		/* Cache the index and prefix of the path within the DispatcherServlet mapping */
		@Nullable
		private Integer indexLookupPath;

		private String prefixLookupPath = "";

		public ResourceUrlEncodingResponseWrapper(HttpServletRequest request, HttpServletResponse wrapped) {
			super(wrapped);
			this.request = request;
		}

		@Override
		public String encodeURL(String url) {
			ResourceUrlProvider resourceUrlProvider = getResourceUrlProvider();
			if (resourceUrlProvider == null) {
				logger.debug("Request attribute exposing ResourceUrlProvider not found");
				return super.encodeURL(url);
			}

			int index = initLookupPath(resourceUrlProvider);
			if (url.startsWith(this.prefixLookupPath)) {
				int suffixIndex = getQueryParamsIndex(url);
				String suffix = url.substring(suffixIndex);
				String lookupPath = url.substring(index, suffixIndex);
				lookupPath = resourceUrlProvider.getForLookupPath(lookupPath);
				if (lookupPath != null) {
					return super.encodeURL(this.prefixLookupPath + lookupPath + suffix);
				}
			}

			return super.encodeURL(url);
		}

		@Nullable
		private ResourceUrlProvider getResourceUrlProvider() {
			return (ResourceUrlProvider) this.request.getAttribute(
					ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
		}

		private int initLookupPath(ResourceUrlProvider urlProvider) {
			if (this.indexLookupPath == null) {
				UrlPathHelper pathHelper = urlProvider.getUrlPathHelper();
				String requestUri = pathHelper.getRequestUri(this.request);
				String lookupPath = pathHelper.getLookupPathForRequest(this.request);
				this.indexLookupPath = requestUri.lastIndexOf(lookupPath);
				this.prefixLookupPath = requestUri.substring(0, this.indexLookupPath);
				if ("/".equals(lookupPath) && !"/".equals(requestUri)) {
					String contextPath = pathHelper.getContextPath(this.request);
					if (requestUri.equals(contextPath)) {
						this.indexLookupPath = requestUri.length();
						this.prefixLookupPath = requestUri;
					}
				}
			}
			return this.indexLookupPath;
		}

		private int getQueryParamsIndex(String url) {
			int index = url.indexOf('?');
			return (index > 0 ? index : url.length());
		}
	}

}
