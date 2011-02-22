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

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.config.AbstractSpecificationBeanDefinitionParser;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses 
 * a {@code interceptors} element to register  set of {@link MappedInterceptor}
 * definitions.
 * 
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * 
 * @since 3.0
 */
class InterceptorsBeanDefinitionParser extends AbstractSpecificationBeanDefinitionParser {

	/**
	 * Parses the {@code <mvc:interceptors/>} tag.
	 */
	public FeatureSpecification doParse(Element element, ParserContext parserContext) {
		MvcInterceptors mvcInterceptors = new MvcInterceptors();

		List<Element> interceptors = DomUtils.getChildElementsByTagName(element, new String[] { "bean", "interceptor" });
		for (Element interceptor : interceptors) {
			if ("interceptor".equals(interceptor.getLocalName())) {
				List<Element> paths = DomUtils.getChildElementsByTagName(interceptor, "mapping");
				String[] pathPatterns = new String[paths.size()];
				for (int i = 0; i < paths.size(); i++) {
					pathPatterns[i] = paths.get(i).getAttribute("path");
				}
				Element beanElement = DomUtils.getChildElementByTagName(interceptor, "bean");
				mvcInterceptors.interceptor(pathPatterns, parseBeanElement(parserContext, beanElement));
			} else {
				mvcInterceptors.interceptor(null, parseBeanElement(parserContext, interceptor));
			}
		}

		return mvcInterceptors;
	}

	private BeanDefinitionHolder parseBeanElement(ParserContext parserContext, Element interceptor) {
		BeanDefinitionHolder beanDef = parserContext.getDelegate().parseBeanDefinitionElement(interceptor);
		beanDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(interceptor, beanDef);
		return beanDef;
	}

}
