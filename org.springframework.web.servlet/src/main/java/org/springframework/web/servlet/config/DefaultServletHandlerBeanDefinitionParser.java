/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.config.AbstractSpecificationBeanDefinitionParser;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} that parses a {@code default-servlet-handler} element to 
 * register a {@link DefaultServletHttpRequestHandler}.  Will also register a 
 * {@link SimpleUrlHandlerMapping} for mapping resource requests, and a 
 * {@link HttpRequestHandlerAdapter} if necessary. 
 *
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 3.0.4
 */
class DefaultServletHandlerBeanDefinitionParser extends AbstractSpecificationBeanDefinitionParser {

	/**
	 * Parses the {@code <mvc:default-servlet-handler/>} tag.
	 */
	public FeatureSpecification doParse(Element element, ParserContext parserContext) {
		String defaultServletHandler = element.getAttribute("default-servlet-handler");
		return StringUtils.hasText(defaultServletHandler) ?
				new MvcDefaultServletHandler(defaultServletHandler) :
				new MvcDefaultServletHandler();
	}

}
