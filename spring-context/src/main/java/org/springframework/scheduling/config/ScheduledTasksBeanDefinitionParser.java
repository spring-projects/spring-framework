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
import org.springframework.beans.factory.support.ManagedList;
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
 * @author Chris Beams
 * @since 3.0
 */
public class ScheduledTasksBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String ELEMENT_SCHEDULED = "scheduled";
	private static final long ZERO_INITIAL_DELAY = 0;

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
		ManagedList<RuntimeBeanReference> cronTaskList = new ManagedList<RuntimeBeanReference>();
		ManagedList<RuntimeBeanReference> fixedDelayTaskList = new ManagedList<RuntimeBeanReference>();
		ManagedList<RuntimeBeanReference> fixedRateTaskList = new ManagedList<RuntimeBeanReference>();
		ManagedList<RuntimeBeanReference> triggerTaskList = new ManagedList<RuntimeBeanReference>();
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

			String cronAttribute = taskElement.getAttribute("cron");
			String fixedDelayAttribute = taskElement.getAttribute("fixed-delay");
			String fixedRateAttribute = taskElement.getAttribute("fixed-rate");
			String triggerAttribute = taskElement.getAttribute("trigger");
			String initialDelayAttribute = taskElement.getAttribute("initial-delay");

			boolean hasCronAttribute = StringUtils.hasText(cronAttribute);
			boolean hasFixedDelayAttribute = StringUtils.hasText(fixedDelayAttribute);
			boolean hasFixedRateAttribute = StringUtils.hasText(fixedRateAttribute);
			boolean hasTriggerAttribute = StringUtils.hasText(triggerAttribute);
			boolean hasInitialDelayAttribute = StringUtils.hasText(initialDelayAttribute);

			if (!(hasCronAttribute || hasFixedDelayAttribute || hasFixedRateAttribute || hasTriggerAttribute)) {
				parserContext.getReaderContext().error(
						"one of the 'cron', 'fixed-delay', 'fixed-rate', or 'trigger' attributes is required", taskElement);
				continue; // with the possible next task element
			}

			if (hasInitialDelayAttribute && (hasCronAttribute || hasTriggerAttribute)) {
				parserContext.getReaderContext().error(
						"the 'initial-delay' attribute may not be used with cron and trigger tasks", taskElement);
				continue; // with the possible next task element
			}

			String runnableName =
					runnableReference(ref, method, taskElement, parserContext).getBeanName();

			if (hasFixedDelayAttribute) {
				fixedDelayTaskList.add(intervalTaskReference(runnableName,
						initialDelayAttribute, fixedDelayAttribute, taskElement, parserContext));
			}
			if (hasFixedRateAttribute) {
				fixedRateTaskList.add(intervalTaskReference(runnableName,
						initialDelayAttribute, fixedRateAttribute, taskElement, parserContext));
			}
			if (hasCronAttribute) {
				cronTaskList.add(cronTaskReference(runnableName, cronAttribute,
						taskElement, parserContext));
			}
			if (hasTriggerAttribute) {
				String triggerName = new RuntimeBeanReference(triggerAttribute).getBeanName();
				triggerTaskList.add(triggerTaskReference(runnableName, triggerName,
						taskElement, parserContext));
			}
		}
		String schedulerRef = element.getAttribute("scheduler");
		if (StringUtils.hasText(schedulerRef)) {
			builder.addPropertyReference("taskScheduler", schedulerRef);
		}
		builder.addPropertyValue("cronTasksList", cronTaskList);
		builder.addPropertyValue("fixedDelayTasksList", fixedDelayTaskList);
		builder.addPropertyValue("fixedRateTasksList", fixedRateTaskList);
		builder.addPropertyValue("triggerTasksList", triggerTaskList);
	}

	private boolean isScheduledElement(Node node, ParserContext parserContext) {
		return node.getNodeType() == Node.ELEMENT_NODE &&
				ELEMENT_SCHEDULED.equals(parserContext.getDelegate().getLocalName(node));
	}

	private RuntimeBeanReference runnableReference(String ref, String method, Element taskElement, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.scheduling.support.ScheduledMethodRunnable");
		builder.addConstructorArgReference(ref);
		builder.addConstructorArgValue(method);
		return beanReference(taskElement, parserContext, builder);
	}

	private RuntimeBeanReference intervalTaskReference(String runnableBeanName,
			String initialDelay, String interval, Element taskElement, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.scheduling.config.IntervalTask");
		builder.addConstructorArgReference(runnableBeanName);
		builder.addConstructorArgValue(interval);
		builder.addConstructorArgValue(StringUtils.hasLength(initialDelay) ? initialDelay : ZERO_INITIAL_DELAY);
		return beanReference(taskElement, parserContext, builder);
	}

	private RuntimeBeanReference cronTaskReference(String runnableBeanName,
			String cronExpression, Element taskElement, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.scheduling.config.CronTask");
		builder.addConstructorArgReference(runnableBeanName);
		builder.addConstructorArgValue(cronExpression);
		return beanReference(taskElement, parserContext, builder);
	}

	private RuntimeBeanReference triggerTaskReference(String runnableBeanName,
			String triggerBeanName, Element taskElement, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.scheduling.config.TriggerTask");
		builder.addConstructorArgReference(runnableBeanName);
		builder.addConstructorArgReference(triggerBeanName);
		return beanReference(taskElement, parserContext, builder);
	}

	private RuntimeBeanReference beanReference(Element taskElement,
			ParserContext parserContext, BeanDefinitionBuilder builder) {
		// Extract the source of the current task
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(taskElement));
		String generatedName = parserContext.getReaderContext().generateBeanName(builder.getRawBeanDefinition());
		parserContext.registerBeanComponent(new BeanComponentDefinition(builder.getBeanDefinition(), generatedName));
		return new RuntimeBeanReference(generatedName);
	}

}
