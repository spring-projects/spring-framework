/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that
 * parses an {@code embedded-database} element and creates a {@link BeanDefinition}
 * for an {@link EmbeddedDatabaseFactoryBean}.
 *
 * <p>Picks up nested {@code script} elements and configures a
 * {@link ResourceDatabasePopulator} for each of them.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see DatabasePopulatorConfigUtils
 */
class EmbeddedDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * Constant for the "database-name" attribute.
	 */
	static final String DB_NAME_ATTRIBUTE = "database-name";

	/**
	 * Constant for the "generate-name" attribute.
	 */
	static final String GENERATE_NAME_ATTRIBUTE = "generate-name";


	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(EmbeddedDatabaseFactoryBean.class);
		setGenerateUniqueDatabaseNameFlag(element, builder);
		setDatabaseName(element, builder);
		setDatabaseType(element, builder);
		DatabasePopulatorConfigUtils.setDatabasePopulator(element, builder);
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		return builder.getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	private void setGenerateUniqueDatabaseNameFlag(Element element, BeanDefinitionBuilder builder) {
		String generateName = element.getAttribute(GENERATE_NAME_ATTRIBUTE);
		if (StringUtils.hasText(generateName)) {
			builder.addPropertyValue("generateUniqueDatabaseName", generateName);
		}
	}

	private void setDatabaseName(Element element, BeanDefinitionBuilder builder) {
		// 1) Check for an explicit database name
		String name = element.getAttribute(DB_NAME_ATTRIBUTE);

		// 2) Fall back to an implicit database name based on the ID
		if (!StringUtils.hasText(name)) {
			name = element.getAttribute(ID_ATTRIBUTE);
		}

		if (StringUtils.hasText(name)) {
			builder.addPropertyValue("databaseName", name);
		}
		// else, let EmbeddedDatabaseFactory use the default "testdb" name
	}

	private void setDatabaseType(Element element, BeanDefinitionBuilder builder) {
		String type = element.getAttribute("type");
		if (StringUtils.hasText(type)) {
			builder.addPropertyValue("databaseType", type);
		}
	}

}
