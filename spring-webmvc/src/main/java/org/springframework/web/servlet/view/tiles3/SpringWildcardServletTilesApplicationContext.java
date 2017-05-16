/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.view.tiles3;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import javax.servlet.ServletContext;

import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.request.locale.URLApplicationResource;
import org.apache.tiles.request.servlet.ServletApplicationContext;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * Spring-specific subclass of the Tiles ServletApplicationContext.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class SpringWildcardServletTilesApplicationContext extends ServletApplicationContext {

	private final ResourcePatternResolver resolver;


	public SpringWildcardServletTilesApplicationContext(ServletContext servletContext) {
		super(servletContext);
		this.resolver = new ServletContextResourcePatternResolver(servletContext);
	}


	@Override
	public ApplicationResource getResource(String localePath) {
		Collection<ApplicationResource> urlSet = getResources(localePath);
		if (!CollectionUtils.isEmpty(urlSet)) {
			return urlSet.iterator().next();
		}
		return null;
	}

	@Override
	public ApplicationResource getResource(ApplicationResource base, Locale locale) {
		Collection<ApplicationResource> urlSet = getResources(base.getLocalePath(locale));
		if (!CollectionUtils.isEmpty(urlSet)) {
			return urlSet.iterator().next();
		}
		return null;
	}

	@Override
	public Collection<ApplicationResource> getResources(String path) {
		Resource[] resources;
		try {
			resources = this.resolver.getResources(path);
		}
		catch (IOException ex) {
			((ServletContext) getContext()).log("Resource retrieval failed for path: " + path, ex);
			return Collections.emptyList();
		}
		if (ObjectUtils.isEmpty(resources)) {
			((ServletContext) getContext()).log("No resources found for path pattern: " + path);
			return Collections.emptyList();
		}

		Collection<ApplicationResource> resourceList = new ArrayList<>(resources.length);
		for (Resource resource : resources) {
			try {
				URL url = resource.getURL();
				resourceList.add(new URLApplicationResource(url.toExternalForm(), url));
			}
			catch (IOException ex) {
				// Shouldn't happen with the kind of resources we're using
				throw new IllegalArgumentException("No URL for " + resource, ex);
			}
		}
		return resourceList;
	}

}
