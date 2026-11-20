/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Parse the <code>&lt;mvc:groovy-configurer&gt;</code> MVC namespace element and register a
 * {@code GroovyConfigurer} bean.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class GroovyMarkupConfigurerBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	/**
	 * The bean name used for the {@code GroovyMarkupConfigurer}.
	 */
	public static final String BEAN_NAME = "mvcGroovyMarkupConfigurer";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return BEAN_NAME;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer";
	}

	@Override
	protected boolean isEligibleAttribute(String name) {
		return (name.equals("auto-indent") || name.equals("cache-templates") || name.equals("resource-loader-path"));
	}

}
