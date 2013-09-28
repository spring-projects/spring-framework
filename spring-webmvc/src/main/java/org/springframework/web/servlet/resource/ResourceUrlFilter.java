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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;


/**
 * A filter that wraps the {@link HttpServletResponse} and overrides its
 * {@link HttpServletResponse#encodeURL(String) encodeURL} method in order to generate
 * resource URL links via {@link ResourceUrlGenerator}.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ResourceUrlFilter extends OncePerRequestFilter {

	private Set<ResourceUrlGenerator> resourceUrlGenerators;


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		filterChain.doFilter(request, new ResourceUrlResponseWrapper(request, response));
	}

	@Override
	protected void initFilterBean() throws ServletException {
		WebApplicationContext cxt = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		Map<String, ResourceUrlGenerator> beans = cxt.getBeansOfType(ResourceUrlGenerator.class);
		this.resourceUrlGenerators = new LinkedHashSet<ResourceUrlGenerator>();
		this.resourceUrlGenerators.addAll(beans.values());
	}


	private class ResourceUrlResponseWrapper extends HttpServletResponseWrapper {

		private final UrlPathHelper pathHelper = new UrlPathHelper();

		private String pathPrefix;


		private ResourceUrlResponseWrapper(HttpServletRequest request, HttpServletResponse wrapped) {
			super(wrapped);
			String requestUri = this.pathHelper.getRequestUri(request);
			String lookupPath = this.pathHelper.getLookupPathForRequest(request);
			this.pathPrefix = requestUri.replace(lookupPath, "");
		}

		@Override
		public String encodeURL(String url) {
			if(url.startsWith(this.pathPrefix)) {
				String relativeUrl = url.replaceFirst(this.pathPrefix, "");
				if (!relativeUrl.startsWith("/")) {
					relativeUrl = "/" + relativeUrl;
				}
				for (ResourceUrlGenerator generator : resourceUrlGenerators) {
					String resourceUrl = generator.getResourceUrl(relativeUrl);
					if (resourceUrl != null) {
						return super.encodeURL(this.pathPrefix + resourceUrl);
					}
				}
			}
			return super.encodeURL(url);
		}
	}

}
