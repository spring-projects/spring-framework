/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.oxm.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the {@code <oxm:jaxb2-marshaller/>} element.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
class Jaxb2MarshallerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.oxm.jaxb.Jaxb2Marshaller";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder beanDefinitionBuilder) {
		String contextPath = element.getAttribute("context-path");
		if (StringUtils.hasText(contextPath)) {
			beanDefinitionBuilder.addPropertyValue("contextPath", contextPath);
		}

		List<Element> classes = DomUtils.getChildElementsByTagName(element, "class-to-be-bound");
		if (!classes.isEmpty()) {
			ManagedList<String> classesToBeBound = new ManagedList<>(classes.size());
			for (Element classToBeBound : classes) {
				String className = classToBeBound.getAttribute("name");
				classesToBeBound.add(className);
			}
			beanDefinitionBuilder.addPropertyValue("classesToBeBound", classesToBeBound);
		}
	}

}
