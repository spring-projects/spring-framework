/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.config.ExecutorContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that parses a
 * {@code resources} element. 
 *
 * @author Rossen Stoyanchev
 * @since 3.0.4
 * @see MvcResources
 * @see MvcResourcesExecutor
 */
class ResourcesBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * Parses the {@code <mvc:resources/>} tag
	 */
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		MvcResources spec = createSpecification(element, parserContext);
		if (spec != null) {
			spec.execute(createExecutorContext(parserContext));
		}
		return null;
	}

	private MvcResources createSpecification(Element element, ParserContext parserContext) {
		String mapping = element.getAttribute("mapping");
		if (!StringUtils.hasText(mapping)) {
			parserContext.getReaderContext().error("The 'mapping' attribute is required.",
					parserContext.extractSource(element));
			return null;
		}
		String[] locations = StringUtils.commaDelimitedListToStringArray(element.getAttribute("location"));
		if (locations.length == 0) {
			parserContext.getReaderContext().error("The 'location' attribute is required.",
					parserContext.extractSource(element));
			return null;
		}
		MvcResources spec = new MvcResources(mapping, locations);
		if (element.hasAttribute("cache-period")) {
			spec.cachePeriod(element.getAttribute("cache-period"));
		}
		if (element.hasAttribute("order")) {
			spec.order(element.getAttribute("order"));
		}
		spec.source(parserContext.extractSource(element));
		spec.sourceName(element.getTagName());
		return spec;
	}

	/**
	 * Adapt the given ParserContext instance into an ExecutorContext.
	 *
	 * TODO SPR-7420: consider unifying the two through a superinterface.
	 * TODO SPR-7420: create a common ParserContext-to-ExecutorContext adapter util
	 */
	private ExecutorContext createExecutorContext(ParserContext parserContext) {
		ExecutorContext executorContext = new ExecutorContext();
		executorContext.setRegistry(parserContext.getRegistry());
		executorContext.setRegistrar(parserContext);
		executorContext.setProblemReporter(parserContext.getReaderContext().getProblemReporter());
		return executorContext;
	}

}
