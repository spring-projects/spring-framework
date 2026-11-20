/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.util.CollectionUtils;
import org.springframework.util.xml.DomUtils;

public class ComponentBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		return parseComponentElement(element);
	}

	private static AbstractBeanDefinition parseComponentElement(Element element) {
		BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(ComponentFactoryBean.class);
		factory.addPropertyValue("parent", parseComponent(element));

		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "component");
		if (!CollectionUtils.isEmpty(childElements)) {
			parseChildComponents(childElements, factory);
		}

		return factory.getBeanDefinition();
	}

	private static BeanDefinition parseComponent(Element element) {
		BeanDefinitionBuilder component = BeanDefinitionBuilder.rootBeanDefinition(Component.class);
		component.addPropertyValue("name", element.getAttribute("name"));
		return component.getBeanDefinition();
	}

	private static void parseChildComponents(List<Element> childElements, BeanDefinitionBuilder factory) {
		ManagedList<BeanDefinition> children = new ManagedList<>(childElements.size());
		for (Element element : childElements) {
			children.add(parseComponentElement(element));
		}
		factory.addPropertyValue("children", children);
	}

}
