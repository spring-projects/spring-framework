/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Parser for the &lt;context:property-override/&gt; element.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @since 2.5.2
 */
class PropertyOverrideBeanDefinitionParser extends AbstractPropertyLoadingBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return PropertyOverrideConfigurer.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		builder.addPropertyValue("ignoreInvalidKeys",
				Boolean.valueOf(element.getAttribute("ignore-unresolvable")));

	}

}
