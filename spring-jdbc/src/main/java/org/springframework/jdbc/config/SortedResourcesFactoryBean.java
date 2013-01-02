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

package org.springframework.jdbc.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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


	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	@Override
	public Class<? extends Resource[]> getObjectType() {
		return Resource[].class;
	}

	@Override
	protected Resource[] createInstance() throws Exception {
		List<Resource> scripts = new ArrayList<Resource>();
		for (String location : this.locations) {
			List<Resource> resources = new ArrayList<Resource>(
					Arrays.asList(this.resourcePatternResolver.getResources(location)));
			Collections.sort(resources, new Comparator<Resource>() {
				public int compare(Resource r1, Resource r2) {
					try {
						return r1.getURL().toString().compareTo(r2.getURL().toString());
					}
					catch (IOException ex) {
						return 0;
					}
				}
			});
			for (Resource resource : resources) {
				scripts.add(resource);
			}
		}
		return scripts.toArray(new Resource[scripts.size()]);
	}

}
