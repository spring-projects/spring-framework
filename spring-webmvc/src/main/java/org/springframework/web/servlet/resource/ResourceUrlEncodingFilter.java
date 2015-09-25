/*
 * Copyright 2002-2015 the original author or authors.
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter that wraps the {@link HttpServletResponse} and overrides its
 * {@link HttpServletResponse#encodeURL(String) encodeURL} method in order to
 * translate internal resource request URLs into public URL paths for external
 * use.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class ResourceUrlEncodingFilter extends OncePerRequestFilter {

	private static final Log logger = LogFactory.getLog(ResourceUrlEncodingFilter.class);


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		filterChain.doFilter(request, new ResourceUrlEncodingResponseWrapper(request, response));
	}


	private static class ResourceUrlEncodingResponseWrapper extends HttpServletResponseWrapper {

		private HttpServletRequest request;

		/* Cache the index of the path within the DispatcherServlet mapping. */
		private Integer indexLookupPath;

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
			initIndexLookupPath(resourceUrlProvider);
			if (url.length() >= this.indexLookupPath) {
				String prefix = url.substring(0, this.indexLookupPath);
				int suffixIndex = getQueryParamsIndex(url);
				String suffix = url.substring(suffixIndex);
				String lookupPath = url.substring(this.indexLookupPath, suffixIndex);
				lookupPath = resourceUrlProvider.getForLookupPath(lookupPath);
				if (lookupPath != null) {
					return super.encodeURL(prefix + lookupPath + suffix);
				}
			}
			return super.encodeURL(url);
		}

		private ResourceUrlProvider getResourceUrlProvider() {
			String name = ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR;
			return (ResourceUrlProvider) this.request.getAttribute(name);
		}

		private void initIndexLookupPath(ResourceUrlProvider urlProvider) {
			if (this.indexLookupPath == null) {
				String requestUri = urlProvider.getPathHelper().getRequestUri(this.request);
				String lookupPath = urlProvider.getPathHelper().getLookupPathForRequest(this.request);
				this.indexLookupPath = requestUri.lastIndexOf(lookupPath);
			}
		}

		private int getQueryParamsIndex(String url) {
			int index = url.indexOf("?");
			return index > 0 ? index : url.length();
		}
	}

}
