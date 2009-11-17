/**
 * 
 */
package org.springframework.jdbc.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

public class SortedResourcesFactoryBean implements FactoryBean<Resource[]> {

	private static final Log logger = LogFactory.getLog(SortedResourcesFactoryBean.class);
	
	private ResourceLoader resourceLoader;
	private List<String> locations;

	public SortedResourcesFactoryBean(ResourceLoader resourceLoader, List<String> locations) {
		super();
		this.resourceLoader = resourceLoader;
		this.locations = locations;
	}

	public Resource[] getObject() throws Exception {
		List<Resource> scripts = new ArrayList<Resource>();
		for (String location : locations) {

			if (logger.isDebugEnabled()) {
				logger.debug("Adding resources from pattern: "+location);
			}

			if (resourceLoader instanceof ResourcePatternResolver) {
				List<Resource> resources = new ArrayList<Resource>(Arrays
						.asList(((ResourcePatternResolver) resourceLoader).getResources(location)));
				Collections.<Resource> sort(resources, new Comparator<Resource>() {
					public int compare(Resource o1, Resource o2) {
						try {
							return o1.getURL().toString().compareTo(o2.getURL().toString());
						} catch (IOException e) {
							return 0;
						}
					}
				});
				for (Resource resource : resources) {
					scripts.add(resource);
				}

			} else {
				scripts.add(resourceLoader.getResource(location));
			}

		}
		return scripts.toArray(new Resource[scripts.size()]);
	}

	public Class<? extends Resource[]> getObjectType() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return false;
	}

}