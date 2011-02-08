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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.config.SpecificationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser for the {@code <context:component-scan/>} element. Parsed metadata is
 * used to populate and execute a {@link ComponentScanSpec} instance.
 *
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see ComponentScan
 * @see ComponentScanSpec
 * @see ComponentScanExecutor
 */
public class ComponentScanBeanDefinitionParser implements BeanDefinitionParser {

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		XmlReaderContext readerContext = parserContext.getReaderContext();
		ClassLoader classLoader = readerContext.getResourceLoader().getClassLoader();

		ComponentScanSpec spec =
			ComponentScanSpec.forDelimitedPackages(element.getAttribute("base-package"))
			.includeAnnotationConfig(element.getAttribute("annotation-config"))
			.useDefaultFilters(element.getAttribute("use-default-filters"))
			.resourcePattern(element.getAttribute("resource-pattern"))
			.beanNameGenerator(element.getAttribute("name-generator"), classLoader)
			.scopeMetadataResolver(element.getAttribute("scope-resolver"), classLoader)
			.scopedProxyMode(element.getAttribute("scoped-proxy"));

		// Parse exclude and include filter elements.
		NodeList nodeList = element.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String localName = parserContext.getDelegate().getLocalName(node);
				String filterType = ((Element)node).getAttribute("type");
				String expression = ((Element)node).getAttribute("expression");
				if ("include-filter".equals(localName)) {
					spec.addIncludeFilter(filterType, expression, classLoader);
				}
				else if ("exclude-filter".equals(localName)) {
					spec.addExcludeFilter(filterType, expression, classLoader);
				}
			}
		}

		spec.beanDefinitionDefaults(parserContext.getDelegate().getBeanDefinitionDefaults())
			.autowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns())
			.source(readerContext.extractSource(element))
			.sourceName(element.getTagName())
			.execute(createSpecificationContext(parserContext));
		return null;
	}


	// Adapt the given ParserContext instance into an SpecificationContext.
	// TODO SPR-7420: create a common ParserContext-to-SpecificationContext adapter utility
	//                or otherwise unify these two types
	private SpecificationContext createSpecificationContext(ParserContext parserContext) {
		SpecificationContext specificationContext = new SpecificationContext();
		specificationContext.setRegistry(parserContext.getRegistry());
		specificationContext.setRegistrar(parserContext);
		specificationContext.setResourceLoader(parserContext.getReaderContext().getResourceLoader());
		specificationContext.setEnvironment(parserContext.getDelegate().getEnvironment());
		specificationContext.setProblemReporter(parserContext.getReaderContext().getProblemReporter());
		return specificationContext;
	}

}
