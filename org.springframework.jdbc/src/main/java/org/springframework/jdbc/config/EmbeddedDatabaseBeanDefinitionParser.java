package org.springframework.jdbc.config;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.embedded.DatabasePopulator;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.embedded.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses {@code embedded-database} element and
 * creates a {@link BeanDefinition} for {@link EmbeddedDatabaseFactoryBean}. Picks up nested {@code script} elements and
 * configures a {@link ResourceDatabasePopulator} for them.
 * 
 * @author Oliver Gierke
 */
public class EmbeddedDatabaseBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext context) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(EmbeddedDatabaseFactoryBean.class);
		setDatabaseType(element, builder);
		setDatabasePopulator(element, context, builder);
		return getSourcedBeanDefinition(builder, element, context);
	}

	private void setDatabaseType(Element element, BeanDefinitionBuilder builder) {
		String type = element.getAttribute("type");
		if (StringUtils.hasText(type)) {
			builder.addPropertyValue("databaseType", type);
		}
	}

	private void setDatabasePopulator(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		List<Element> scripts = DomUtils.getChildElementsByTagName(element, "script");
		if (scripts.size() > 0) {
			builder.addPropertyValue("databasePopulator", createDatabasePopulator(scripts, context));
		}
	}

	private DatabasePopulator createDatabasePopulator(List<Element> scripts, ParserContext context) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		for (Element scriptElement : scripts) {
			Resource script = context.getReaderContext().getResourceLoader().getResource(scriptElement.getAttribute("location"));
			populator.addScript(script);
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
