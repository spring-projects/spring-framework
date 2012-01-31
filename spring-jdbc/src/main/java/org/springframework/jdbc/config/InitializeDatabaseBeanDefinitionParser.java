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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses an {@code initialize-database}
 * element and creates a {@link BeanDefinition} of type {@link DataSourceInitializer}. Picks up nested
 * {@code script} elements and configures a {@link ResourceDatabasePopulator} for them.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @since 3.0
 */
class InitializeDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DataSourceInitializer.class);
		builder.addPropertyReference("dataSource", element.getAttribute("data-source"));
		builder.addPropertyValue("enabled", element.getAttribute("enabled"));
		DatabasePopulatorConfigUtils.setDatabasePopulator(element, builder);
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		return builder.getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

}
