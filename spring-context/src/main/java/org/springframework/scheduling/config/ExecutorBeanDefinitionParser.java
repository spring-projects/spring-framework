/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.scheduling.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'executor' element of the 'task' namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ExecutorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.scheduling.config.TaskExecutorFactoryBean";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String keepAliveSeconds = element.getAttribute("keep-alive");
		if (StringUtils.hasText(keepAliveSeconds)) {
			builder.addPropertyValue("keepAliveSeconds", keepAliveSeconds);
		}
		String queueCapacity = element.getAttribute("queue-capacity");
		if (StringUtils.hasText(queueCapacity)) {
			builder.addPropertyValue("queueCapacity", queueCapacity);
		}
		configureRejectionPolicy(element, builder);
		String poolSize = element.getAttribute("pool-size");
		if (StringUtils.hasText(poolSize)) {
			builder.addPropertyValue("poolSize", poolSize);
		}
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
	}

	private void configureRejectionPolicy(Element element, BeanDefinitionBuilder builder) {
		String rejectionPolicy = element.getAttribute("rejection-policy");
		if (!StringUtils.hasText(rejectionPolicy)) {
			return;
		}
		String prefix = "java.util.concurrent.ThreadPoolExecutor.";
		String policyClassName = switch (rejectionPolicy) {
			case "ABORT" -> prefix + "AbortPolicy";
			case "CALLER_RUNS" -> prefix + "CallerRunsPolicy";
			case "DISCARD" -> prefix + "DiscardPolicy";
			case "DISCARD_OLDEST" -> prefix + "DiscardOldestPolicy";
			default -> rejectionPolicy;
		};
		builder.addPropertyValue("rejectedExecutionHandler", new RootBeanDefinition(policyClassName));
	}

}
