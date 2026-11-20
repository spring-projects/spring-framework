/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.config;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * {@link BeanDefinitionParser} responsible for parsing the
 * {@code <context:spring-configured/>} tag.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
class SpringConfiguredBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * The bean name of the internally managed bean configurer aspect.
	 */
	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME =
			"org.springframework.context.config.internalBeanConfigurerAspect";

	static final String BEAN_CONFIGURER_ASPECT_CLASS_NAME =
			"org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect";


	@Override
	public @Nullable BeanDefinition parse(Element element, ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition();
			def.setBeanClassName(BEAN_CONFIGURER_ASPECT_CLASS_NAME);
			def.setFactoryMethodName("aspectOf");
			def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			def.setSource(parserContext.extractSource(element));
			parserContext.registerBeanComponent(new BeanComponentDefinition(def, BEAN_CONFIGURER_ASPECT_BEAN_NAME));
		}
		return null;
	}

}
