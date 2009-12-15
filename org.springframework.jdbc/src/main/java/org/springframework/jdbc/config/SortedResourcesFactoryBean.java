/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Dave Syer
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SortedResourcesFactoryBean implements FactoryBean<Resource[]> {

	private final Resource[] resources;

	public SortedResourcesFactoryBean(ResourceLoader resourceLoader, List<String> locations) throws IOException {
		List<Resource> scripts = new ArrayList<Resource>();
		for (String location : locations) {
			if (resourceLoader instanceof ResourcePatternResolver) {
				List<Resource> resources = new ArrayList<Resource>(
						Arrays.asList(((ResourcePatternResolver) resourceLoader).getResources(location)));
				Collections.sort(resources, new Comparator<Resource>() {
					public int compare(Resource o1, Resource o2) {
						try {
							return o1.getURL().toString().compareTo(o2.getURL().toString());
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
			else {
				scripts.add(resourceLoader.getResource(location));
			}
		}
		this.resources = scripts.toArray(new Resource[scripts.size()]);
	}

	public Resource[] getObject() {
		return this.resources;
	}

	public Class<? extends Resource[]> getObjectType() {
		return Resource[].class;
	}

	public boolean isSingleton() {
		return true;
	}

}
