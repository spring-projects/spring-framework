/*
 * Copyright 2002-2011 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses an {@code embedded-database}
 * element and creates a {@link BeanDefinition} for {@link EmbeddedDatabaseFactoryBean}. Picks up nested
 * {@code script} elements and configures a {@link ResourceDatabasePopulator} for them.
 *
 * @author Oliver Gierke
 * @since 3.0
 */
class EmbeddedDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(EmbeddedDatabaseFactoryBean.class);
		setDatabaseType(element, builder);
		setDatabasePopulator(element, builder);
		useIdAsDatabaseNameIfGiven(element, builder);
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		return builder.getBeanDefinition();
	}

	private void useIdAsDatabaseNameIfGiven(Element element, BeanDefinitionBuilder builder) {
		String id = element.getAttribute(ID_ATTRIBUTE);
		if (StringUtils.hasText(id)) {
			builder.addPropertyValue("databaseName", id);
		}
	}

	private void setDatabaseType(Element element, BeanDefinitionBuilder builder) {
		String type = element.getAttribute("type");
		if (StringUtils.hasText(type)) {
			builder.addPropertyValue("databaseType", type);
		}
	}

	private void setDatabasePopulator(Element element, BeanDefinitionBuilder builder) {
		List<Element> scripts = DomUtils.getChildElementsByTagName(element, "script");
		if (scripts.size() > 0) {
			builder.addPropertyValue("databasePopulator", createDatabasePopulator(scripts));
		}
	}

	private BeanDefinition createDatabasePopulator(List<Element> scripts) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ResourceDatabasePopulator.class);
		List<String> locations = new ArrayList<String>();
		for (Element scriptElement : scripts) {
			locations.add(scriptElement.getAttribute("location"));
		}
		// Use a factory bean for the resources so they can be given an order if a pattern is used
		BeanDefinitionBuilder resourcesFactory = BeanDefinitionBuilder.genericBeanDefinition(SortedResourcesFactoryBean.class);
		resourcesFactory.addConstructorArgValue(locations);
		builder.addPropertyValue("scripts", resourcesFactory.getBeanDefinition());
		return builder.getBeanDefinition();
	}

}
