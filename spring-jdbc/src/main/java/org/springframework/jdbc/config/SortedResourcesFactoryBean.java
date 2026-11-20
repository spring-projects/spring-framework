/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

/**
 * {@link FactoryBean} implementation that takes a list of location Strings
 * and creates a sorted array of {@link Resource} instances.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Christian Dupuis
 * @since 3.0
 */
public class SortedResourcesFactoryBean extends AbstractFactoryBean<Resource[]> implements ResourceLoaderAware {

	private final List<String> locations;

	private ResourcePatternResolver resourcePatternResolver;


	public SortedResourcesFactoryBean(List<String> locations) {
		this.locations = locations;
		this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
	}

	public SortedResourcesFactoryBean(ResourceLoader resourceLoader, List<String> locations) {
		this.locations = locations;
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	@Override
	public Class<? extends Resource[]> getObjectType() {
		return Resource[].class;
	}

	@Override
	protected Resource[] createInstance() throws Exception {
		List<Resource> result = new ArrayList<>();
		for (String location : this.locations) {
			Resource[] resources = this.resourcePatternResolver.getResources(location);

			// Cache URLs to avoid repeated I/O during sorting
			Map<Resource, String> urlCache = new LinkedHashMap<>(resources.length);
			List<Resource> failingResources = new ArrayList<>();
			for (Resource resource : resources) {
				try {
					urlCache.put(resource, resource.getURL().toString());
				}
				catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to resolve " + resource + " for sorting purposes: " + ex);
					}
					failingResources.add(resource);
				}
			}

			// Sort using cached URLs
			List<Resource> sortedResources = new ArrayList<>(urlCache.keySet());
			sortedResources.sort(Comparator.comparing(urlCache::get));

			result.addAll(sortedResources);
			result.addAll(failingResources);
		}
		return result.toArray(new Resource[0]);
	}

}
