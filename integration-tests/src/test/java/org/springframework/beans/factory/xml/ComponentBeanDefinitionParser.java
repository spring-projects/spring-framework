/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
