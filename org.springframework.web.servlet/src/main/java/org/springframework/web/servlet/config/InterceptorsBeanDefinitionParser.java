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

package org.springframework.web.servlet.config;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses the {@code interceptors} element to configure
 * a set of global Spring MVC {@link HandlerInterceptor HandlerInterceptors}.
 * The set is expected to be configured by type on each registered HandlerMapping.
 *
 * @author Keith Donald
 * @since 3.0
 */
class InterceptorsBeanDefinitionParser implements BeanDefinitionParser {

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		List<Element> beans = DomUtils.getChildElementsByTagName(element, "bean");
		for (Element bean : beans) {			
			BeanDefinitionHolder beanHolder = parserContext.getDelegate().parseBeanDefinitionElement(bean);
			parserContext.getDelegate().decorateBeanDefinitionIfRequired(bean, beanHolder);
			parserContext.getReaderContext().registerWithGeneratedName(beanHolder.getBeanDefinition());
		}
		return null;
	}
	
}