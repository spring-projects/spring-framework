/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class BootstrapExecutorBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
//		It's not getting executed, I'm looking into the problem
//		This class's task is done by the processConfigBeanDefinitions method
//		in ConfigurationClassPostProcessor right now, but I plan to move the logic back to this
//		class once the problem is solved.
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskExecutor.class);

		builder.addPropertyValue("threadNamePrefix", element.getAttribute("thread-name-prefix"));
		builder.addPropertyValue("corePoolSize", element.getAttribute("core-pool-size"));
		// Set more properties here ...
		builder.addPropertyValue("daemon", true);

		// Register bean
		String beanName = "bootstrapExecutor";
		parserContext.getRegistry().registerBeanDefinition(beanName, builder.getBeanDefinition());
		return null;
	}

}
