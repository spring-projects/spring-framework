/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.view.tiles2;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContext;

import org.apache.tiles.servlet.context.ServletTilesApplicationContext;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * Spring-specific subclass of the Tiles ServletTilesApplicationContext.
 *
 * @author Juergen Hoeller
 * @since 4.0.1
 */
public class SpringWildcardServletTilesApplicationContext extends ServletTilesApplicationContext {

	private final ResourcePatternResolver resolver;


	public SpringWildcardServletTilesApplicationContext(ServletContext servletContext) {
		super(servletContext);
		this.resolver = new ServletContextResourcePatternResolver(servletContext);
	}


	@Override
	public URL getResource(String path) throws IOException {
		URL retValue = null;
		Set<URL> urlSet = getResources(path);
		if (urlSet != null && !urlSet.isEmpty()) {
			retValue = urlSet.iterator().next();
		}
		return retValue;
	}

	@Override
	public Set<URL> getResources(String path) throws IOException {
		Set<URL> urlSet = null;
		Resource[] resources = this.resolver.getResources(path);
		if (resources != null && resources.length > 0) {
			urlSet = new HashSet<URL>();
			for (Resource resource : resources) {
				urlSet.add(resource.getURL());
			}
		}
		return urlSet;
	}

}
