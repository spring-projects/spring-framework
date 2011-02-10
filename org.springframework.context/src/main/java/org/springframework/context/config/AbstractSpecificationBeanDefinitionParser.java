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

package org.springframework.context.config;

import java.lang.reflect.Field;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ComponentRegistrarAdapter;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.env.DefaultEnvironment;
import org.w3c.dom.Element;

/**
 * TODO SPR-7420: document
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class AbstractSpecificationBeanDefinitionParser implements BeanDefinitionParser {

	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		FeatureSpecification spec = doParse(element, parserContext);
		if (spec instanceof SourceAwareSpecification) {
			((SourceAwareSpecification)spec).source(parserContext.getReaderContext().extractSource(element));
			((SourceAwareSpecification)spec).sourceName(element.getTagName());
		}
		spec.execute(specificationContextFrom(parserContext));
		return null;
	}

	abstract protected FeatureSpecification doParse(Element element, ParserContext parserContext);

	/**
	 * Adapt the given ParserContext into a SpecificationContext.
	 */
	private SpecificationContext specificationContextFrom(ParserContext parserContext) {
		SpecificationContext specContext = new SpecificationContext();
		specContext.setRegistry(parserContext.getRegistry());
		specContext.setRegistrar(new ComponentRegistrarAdapter(parserContext));
		specContext.setResourceLoader(parserContext.getReaderContext().getResourceLoader());
		try {
			// again, STS constraints around the addition of the new getEnvironment()
			// method in 3.1.0 (it's not present in STS current spring version, 3.0.5)
			// TODO 3.1 GA: remove this block prior to 3.1 GA
			specContext.setEnvironment(parserContext.getDelegate().getEnvironment());
		} catch (NoSuchMethodError ex) {
			specContext.setEnvironment(new DefaultEnvironment());
		}
		try {
			// access the reader context's problem reporter reflectively in order to
			// compensate for tooling (STS) constraints around introduction of changes
			// to parser context / reader context classes.
			// TODO 3.1 GA: remove this block prior to 3.1 GA
			Field field = ReaderContext.class.getDeclaredField("problemReporter");
			field.setAccessible(true);
			ProblemReporter problemReporter = (ProblemReporter)field.get(parserContext.getReaderContext());
			specContext.setProblemReporter(problemReporter);
		} catch (Exception ex) {
			throw new IllegalStateException(
					"Could not access field 'ReaderContext#problemReporter' on object " +
					parserContext.getReaderContext(), ex);
		}
		return specContext;
	}

}
