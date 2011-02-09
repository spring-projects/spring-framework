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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * TODO SPR-7420: document
 * <p>Adapter is necessary as opposed to having ParserContext
 * implement ComponentRegistrar directly due to tooling issues.
 * STS may ship with a version of Spring older that 3.1 (when
 * this type was introduced), and will run into
 * IncompatibleClassChangeErrors when it's (3.0.5) ParserContext
 * tries to mix with our (3.1.0) BeanDefinitionParser
 * (and related) infrastructure.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class ComponentRegistrarAdapter implements ComponentRegistrar {

	private final ParserContext parserContext;

	public ComponentRegistrarAdapter(ParserContext parserContext) {
		this.parserContext = parserContext;
	}

	public String registerWithGeneratedName(BeanDefinition beanDefinition) {
		return this.parserContext.getReaderContext().registerWithGeneratedName(beanDefinition);
	}

	public void registerBeanComponent(BeanComponentDefinition component) {
		this.parserContext.registerBeanComponent(component);
	}

	public void registerComponent(ComponentDefinition component) {
		this.parserContext.registerComponent(component);
	}

}
