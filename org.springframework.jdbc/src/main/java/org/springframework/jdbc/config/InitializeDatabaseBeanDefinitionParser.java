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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses an {@code initialize-database} element and
 * creates a {@link BeanDefinition} of type {@link DataSourceInitializer}. Picks up nested {@code script} elements and
 * configures a {@link ResourceDatabasePopulator} for them.
 * @author Dave Syer
 * @since 3.0
 */
public class InitializeDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext context) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DataSourceInitializer.class);
		builder.addPropertyReference("dataSource", element.getAttribute("data-source"));
		builder.addPropertyValue("enabled", element.getAttribute("enabled"));
		setDatabasePopulator(element, context, builder);
		return getSourcedBeanDefinition(builder, element, context);
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	private void setDatabasePopulator(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		List<Element> scripts = DomUtils.getChildElementsByTagName(element, "script");
		if (scripts.size() > 0) {
			builder.addPropertyValue("databasePopulator", createDatabasePopulator(element, scripts, context));
		}
	}

	private BeanDefinition createDatabasePopulator(Element element, List<Element> scripts, ParserContext context) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ResourceDatabasePopulator.class);
		builder.addPropertyValue("ignoreFailedDrops", element.getAttribute("ignore-failures").equals("DROPS"));
		builder.addPropertyValue("continueOnError", element.getAttribute("ignore-failures").equals("ALL"));

		List<String> locations = new ArrayList<String>();
		for (Element scriptElement : scripts) {
			String location = scriptElement.getAttribute("location");
			locations.add(location);
		}

		// Use a factor bean for the resources so they can be given an order if a pattern is used
		BeanDefinitionBuilder resourcesFactory = BeanDefinitionBuilder
				.genericBeanDefinition(SortedResourcesFactoryBean.class);
		resourcesFactory.addConstructorArgValue(context.getReaderContext().getResourceLoader());
		resourcesFactory.addConstructorArgValue(locations);
		builder.addPropertyValue("scripts", resourcesFactory.getBeanDefinition());

		return builder.getBeanDefinition();
	}

	private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
			ParserContext context) {
		AbstractBeanDefinition definition = builder.getBeanDefinition();
		definition.setSource(context.extractSource(source));
		return definition;
	}

	public static class SortedResourcesFactoryBean implements FactoryBean<Resource[]> {

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
					logger.debug("Adding resources from pattern: " + location);
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

}
