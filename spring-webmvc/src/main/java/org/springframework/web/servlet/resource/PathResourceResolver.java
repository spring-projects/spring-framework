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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;


/**
 * A simple path-based {@link ResourceResolver} that appends the request path to each
 * configured Resource location and checks if such a resource exists.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PathResourceResolver implements ResourceResolver {

	private static final Log logger = LogFactory.getLog(PathResourceResolver.class);


	@Override
	public Resource resolveResource(HttpServletRequest request,
			String requestPath, List<Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, locations);
	}

	@Override
	public String resolveUrlPath(String resourcePath, List<Resource> locations, ResourceResolverChain chain) {
		return (getResource(resourcePath, locations) != null) ? resourcePath : null;
	}

	private Resource getResource(String path, List<Resource> locations) {
		for (Resource location : locations) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Trying relative path [" + path + "] against base location: " + location);
				}
				Resource resource = location.createRelative(path);
				if (resource.exists() && resource.isReadable()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Found matching resource: " + resource);
					}
					return resource;
				}
				else if (logger.isTraceEnabled()) {
					logger.trace("Relative resource doesn't exist or isn't readable: " + resource);
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to create relative resource - trying next resource location", ex);
			}
		}
		return null;
	}

}
