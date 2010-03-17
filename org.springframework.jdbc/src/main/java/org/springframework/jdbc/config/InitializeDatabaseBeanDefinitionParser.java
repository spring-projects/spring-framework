/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
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

		// Use a factory bean for the resources so they can be given an order if a pattern is used
		BeanDefinitionBuilder resourcesFactory = BeanDefinitionBuilder
				.genericBeanDefinition(SortedResourcesFactoryBean.class);
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

}
