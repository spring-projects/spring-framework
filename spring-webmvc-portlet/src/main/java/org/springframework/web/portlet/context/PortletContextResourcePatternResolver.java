/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet.context;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.portlet.PortletContext;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * PortletContext-aware subclass of {@link PathMatchingResourcePatternResolver},
 * able to find matching resources below the web application root directory
 * via Portlet API's {@code PortletContext.getResourcePaths}.
 * Falls back to the superclass' file system checking for other resources.
 *
 * <p>The advantage of using {@code PortletContext.getResourcePaths} to
 * find matching files is that it will work in a WAR file which has not been
 * expanded too.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 */
public class PortletContextResourcePatternResolver extends PathMatchingResourcePatternResolver {

	/**
	 * Create a new PortletContextResourcePatternResolver.
	 * @param portletContext the PortletContext to load resources with
	 * @see PortletContextResourceLoader#PortletContextResourceLoader(javax.portlet.PortletContext)
	 */
	public PortletContextResourcePatternResolver(PortletContext portletContext) {
		super(new PortletContextResourceLoader(portletContext));
	}

	/**
	 * Create a new PortletContextResourcePatternResolver.
	 * @param resourceLoader the ResourceLoader to load root directories and
	 * actual resources with
	 */
	public PortletContextResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}


	/**
	 * Overridden version which checks for PortletContextResource
	 * and uses {@code PortletContext.getResourcePaths} to find
	 * matching resources below the web application root directory.
	 * In case of other resources, delegates to the superclass version.
	 * @see #doRetrieveMatchingPortletContextResources
	 * @see PortletContextResource
	 * @see javax.portlet.PortletContext#getResourcePaths
	 */
	@Override
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException {
		if (rootDirResource instanceof PortletContextResource) {
			PortletContextResource pcResource = (PortletContextResource) rootDirResource;
			PortletContext pc = pcResource.getPortletContext();
			String fullPattern = pcResource.getPath() + subPattern;
			Set<Resource> result = new HashSet<Resource>();
			doRetrieveMatchingPortletContextResources(pc, fullPattern, pcResource.getPath(), result);
			return result;
		}
		return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
	}

	/**
	 * Recursively retrieve PortletContextResources that match the given pattern,
	 * adding them to the given result set.
	 * @param portletContext the PortletContext to work on
	 * @param fullPattern the pattern to match against,
	 * with preprended root directory path
	 * @param dir the current directory
	 * @param result the Set of matching Resources to add to
	 * @throws IOException if directory contents could not be retrieved
	 * @see org.springframework.web.portlet.context.PortletContextResource
	 * @see javax.portlet.PortletContext#getResourcePaths
	 */
	protected void doRetrieveMatchingPortletContextResources(
			PortletContext portletContext, String fullPattern, String dir, Set<Resource> result) throws IOException {

		Set candidates = portletContext.getResourcePaths(dir);
		if (candidates != null) {
			boolean dirDepthNotFixed = fullPattern.contains("**");
			for (Iterator it = candidates.iterator(); it.hasNext();) {
				String currPath = (String) it.next();
				if (currPath.endsWith("/") &&
						(dirDepthNotFixed ||
						StringUtils.countOccurrencesOf(currPath, "/") <= StringUtils.countOccurrencesOf(fullPattern, "/"))) {
					doRetrieveMatchingPortletContextResources(portletContext, fullPattern, currPath, result);
				}
				if (getPathMatcher().match(fullPattern, currPath)) {
					result.add(new PortletContextResource(portletContext, currPath));
				}
			}
		}
	}

}
