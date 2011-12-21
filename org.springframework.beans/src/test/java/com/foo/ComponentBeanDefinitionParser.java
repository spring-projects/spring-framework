package com.foo;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public class ComponentBeanDefinitionParser extends AbstractBeanDefinitionParser {

	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {
		return parseComponentElement(element);
	}

	private static AbstractBeanDefinition parseComponentElement(Element element) {
		BeanDefinitionBuilder factory = BeanDefinitionBuilder
				.rootBeanDefinition(ComponentFactoryBean.class);

		factory.addPropertyValue("parent", parseComponent(element));

		List<Element> childElements = DomUtils.getChildElementsByTagName(
				element, "component");
		if (childElements != null && childElements.size() > 0) {
			parseChildComponents(childElements, factory);
		}

		return factory.getBeanDefinition();
	}

	private static BeanDefinition parseComponent(Element element) {
		BeanDefinitionBuilder component = BeanDefinitionBuilder
				.rootBeanDefinition(Component.class);
		component.addPropertyValue("name", element.getAttribute("name"));
		return component.getBeanDefinition();
	}

	private static void parseChildComponents(List<Element> childElements,
			BeanDefinitionBuilder factory) {
		ManagedList<BeanDefinition> children = new ManagedList<BeanDefinition>(
				childElements.size());
		for (Element element : childElements) {
			children.add(parseComponentElement(element));
		}
		factory.addPropertyValue("children", children);
	}
}
