/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling.config;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser for the 'scheduled-tasks' element of the scheduling namespace.
 * 
 * @author Mark Fisher
 * @since 3.0
 */
public class ScheduledTasksBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String ELEMENT_SCHEDULED = "scheduled";

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.scheduling.config.ScheduledTaskRegistrar";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.setLazyInit(false); // lazy scheduled tasks are a contradiction in terms -> force to false
		ManagedMap<RuntimeBeanReference, String> cronTaskMap = new ManagedMap<RuntimeBeanReference, String>();
		ManagedMap<RuntimeBeanReference, String> fixedDelayTaskMap = new ManagedMap<RuntimeBeanReference, String>();
		ManagedMap<RuntimeBeanReference, String> fixedRateTaskMap = new ManagedMap<RuntimeBeanReference, String>();
		ManagedMap<RuntimeBeanReference, RuntimeBeanReference> triggerTaskMap = new ManagedMap<RuntimeBeanReference, RuntimeBeanReference>();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (!isScheduledElement(child, parserContext)) {
				continue;
			}
			Element taskElement = (Element) child;
			String ref = taskElement.getAttribute("ref");
			String method = taskElement.getAttribute("method");
			
			// Check that 'ref' and 'method' are specified
			if (!StringUtils.hasText(ref) || !StringUtils.hasText(method)) {
				parserContext.getReaderContext().error("Both 'ref' and 'method' are required", taskElement);
				// Continue with the possible next task element
				continue;
			}
			
			RuntimeBeanReference runnableBeanRef = new RuntimeBeanReference(
					createRunnableBean(ref, method, taskElement, parserContext));

			String cronAttribute = taskElement.getAttribute("cron");
			String fixedDelayAttribute = taskElement.getAttribute("fixed-delay");
			String fixedRateAttribute = taskElement.getAttribute("fixed-rate");
			String triggerAttribute = taskElement.getAttribute("trigger");

			boolean hasCronAttribute = StringUtils.hasText(cronAttribute);
			boolean hasFixedDelayAttribute = StringUtils.hasText(fixedDelayAttribute);
			boolean hasFixedRateAttribute = StringUtils.hasText(fixedRateAttribute);
			boolean hasTriggerAttribute = StringUtils.hasText(triggerAttribute);

			if (!(hasCronAttribute | hasFixedDelayAttribute | hasFixedRateAttribute | hasTriggerAttribute)) {
				parserContext.getReaderContext().error(
						"one of the 'cron', 'fixed-delay', 'fixed-rate', or 'trigger' attributes is required", taskElement);
				continue; // with the possible next task element
			}

			if (hasCronAttribute) {
				cronTaskMap.put(runnableBeanRef, cronAttribute);
			}
			if (hasFixedDelayAttribute) {
				fixedDelayTaskMap.put(runnableBeanRef, fixedDelayAttribute);
			}
			if (hasFixedRateAttribute) {
				fixedRateTaskMap.put(runnableBeanRef, fixedRateAttribute);
			}
			if (hasTriggerAttribute) {
				triggerTaskMap.put(runnableBeanRef, new RuntimeBeanReference(triggerAttribute));
			}
		}
		String schedulerRef = element.getAttribute("scheduler");
		if (StringUtils.hasText(schedulerRef)) {
			builder.addPropertyReference("taskScheduler", schedulerRef);
		}
		builder.addPropertyValue("cronTasks", cronTaskMap);
		builder.addPropertyValue("fixedDelayTasks", fixedDelayTaskMap);
		builder.addPropertyValue("fixedRateTasks", fixedRateTaskMap);
		builder.addPropertyValue("triggerTasks", triggerTaskMap);
	}

	private boolean isScheduledElement(Node node, ParserContext parserContext) {
		return node.getNodeType() == Node.ELEMENT_NODE &&
				ELEMENT_SCHEDULED.equals(parserContext.getDelegate().getLocalName(node));
	}

	private String createRunnableBean(String ref, String method, Element taskElement, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.scheduling.support.ScheduledMethodRunnable");
		builder.addConstructorArgReference(ref);
		builder.addConstructorArgValue(method);
		// Extract the source of the current task
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(taskElement));
		String generatedName = parserContext.getReaderContext().generateBeanName(builder.getRawBeanDefinition());
		parserContext.registerBeanComponent(new BeanComponentDefinition(builder.getBeanDefinition(), generatedName));
		return generatedName;
	}

}
