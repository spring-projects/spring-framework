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
 * 
 * @author Jeremy Grelle
 */
public class ResourceUrlEncodingFilter extends OncePerRequestFilter {

	private ResourceUrlMapper mapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		filterChain.doFilter(request, new ResourceUrlResponseWrapper(request, response));
	}
	
	@Override
	protected void initFilterBean() throws ServletException {
		WebApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		this.mapper = appContext.getBean(ResourceUrlMapper.class);
	}

	private class ResourceUrlResponseWrapper extends HttpServletResponseWrapper {
		
		private final UrlPathHelper pathHelper = new UrlPathHelper();
		
		private String pathPrefix;
		
		private ResourceUrlResponseWrapper(HttpServletRequest request, HttpServletResponse wrapped) {
			super(wrapped);
			
			this.pathPrefix = pathHelper.getContextPath(request);
			String servletPath = pathHelper.getServletPath(request);
			String appPath = pathHelper.getPathWithinApplication(request);
			//This accounts for the behavior when servlet is mapped to "/"
			if (!servletPath.equals(appPath)) {
				this.pathPrefix += pathHelper.getServletPath(request);
			}
		}

		@Override
		public String encodeURL(String url) {
			if(url.startsWith(pathPrefix)) {
				String relativeUrl = url.replaceFirst(pathPrefix, "");
				if (!relativeUrl.startsWith("/")) {
					relativeUrl = "/" + relativeUrl;
				}
				if (mapper.isResourceUrl(relativeUrl)) {
					String resourceUrl = mapper.getUrlForResource(relativeUrl);
					if (resourceUrl != null) {
						return resourceUrl;
					}
				}
			}
			return super.encodeURL(url);
		}

	}

}
