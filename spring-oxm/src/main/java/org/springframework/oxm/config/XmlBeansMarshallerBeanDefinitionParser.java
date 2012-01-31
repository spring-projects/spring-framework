/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.oxm.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the <code>&lt;oxm:xmlbeans-marshaller/&gt; element.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
class XmlBeansMarshallerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	public static final String XML_BEANS_MARSHALLER_CLASS_NAME = "org.springframework.oxm.xmlbeans.XmlBeansMarshaller";

	@Override
	protected String getBeanClassName(Element element) {
		return XML_BEANS_MARSHALLER_CLASS_NAME;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder beanDefinitionBuilder) {
		String optionsName = element.getAttribute("options");
		if (StringUtils.hasText(optionsName)) {
			beanDefinitionBuilder.addPropertyReference("xmlOptions", optionsName);
		}
	}

}
