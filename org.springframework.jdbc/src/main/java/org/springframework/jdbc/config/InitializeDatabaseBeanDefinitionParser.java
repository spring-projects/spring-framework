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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses {@code initialize-database} element and
 * creates a {@link BeanDefinition} for {@link DataSourceInitializer}. Picks up nested {@code script} elements and
 * configures a {@link ResourceDatabasePopulator} for them.
@author Dave Syer
 *
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

	private DatabasePopulator createDatabasePopulator(Element element, List<Element> scripts, ParserContext context) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setIgnoreFailedDrops(element.getAttribute("ignore-failures").equals("DROPS"));
		populator.setContinueOnError(element.getAttribute("ignore-failures").equals("ALL"));
		for (Element scriptElement : scripts) {
			String location = scriptElement.getAttribute("location");
			ResourceLoader resourceLoader = context.getReaderContext().getResourceLoader();
			if (resourceLoader instanceof ResourcePatternResolver) {
				try {
					List<Resource> resources = new ArrayList<Resource>(Arrays.asList(((ResourcePatternResolver)resourceLoader).getResources(location)));
					Collections.<Resource>sort(resources, new Comparator<Resource>()  {
						public int compare(Resource o1, Resource o2) {
							try {
								return o1.getURL().toString().compareTo(o2.getURL().toString());
							} catch (IOException e) {
								return 0;
							}
						}
					});
					for (Resource resource : resources) {
						populator.addScript(resource);					
					}
				} catch (IOException e) {
					context.getReaderContext().error("Cannot locate resources for script from location="+location, scriptElement);
				}
			} else {
				populator.addScript(resourceLoader.getResource(location));
			}
		}
		return populator;
	}

	private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
			ParserContext context) {
		AbstractBeanDefinition definition = builder.getBeanDefinition();
		definition.setSource(context.extractSource(source));
		return definition;
	}

}
