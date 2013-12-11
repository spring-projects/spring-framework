/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jms.config;

import javax.jms.Session;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the JMS {@code &lt;listener-container&gt;} element.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 */
class JmsListenerContainerParser extends AbstractListenerContainerParser {

	private static final String CONTAINER_TYPE_ATTRIBUTE = "container-type";

	private static final String CONTAINER_CLASS_ATTRIBUTE = "container-class";

	private static final String CONNECTION_FACTORY_ATTRIBUTE = "connection-factory";

	private static final String TASK_EXECUTOR_ATTRIBUTE = "task-executor";

	private static final String ERROR_HANDLER_ATTRIBUTE = "error-handler";

	private static final String CACHE_ATTRIBUTE = "cache";

	private static final String RECEIVE_TIMEOUT_ATTRIBUTE = "receive-timeout";

	private static final String RECOVERY_INTERVAL_ATTRIBUTE = "recovery-interval";


	@Override
	protected BeanDefinition parseContainer(Element listenerEle, Element containerEle, ParserContext parserContext) {
		RootBeanDefinition containerDef = new RootBeanDefinition();
		containerDef.setSource(parserContext.extractSource(containerEle));

		parseListenerConfiguration(listenerEle, parserContext, containerDef);
		parseContainerConfiguration(containerEle, parserContext, containerDef);

		String containerType = containerEle.getAttribute(CONTAINER_TYPE_ATTRIBUTE);
		String containerClass = containerEle.getAttribute(CONTAINER_CLASS_ATTRIBUTE);
		if (!"".equals(containerClass)) {
			containerDef.setBeanClassName(containerClass);
		}
		else if ("".equals(containerType) || containerType.startsWith("default")) {
			containerDef.setBeanClassName("org.springframework.jms.listener.DefaultMessageListenerContainer");
		}
		else if (containerType.startsWith("simple")) {
			containerDef.setBeanClassName("org.springframework.jms.listener.SimpleMessageListenerContainer");
		}
		else {
			parserContext.getReaderContext().error(
					"Invalid 'container-type' attribute: only \"default\" and \"simple\" supported.", containerEle);
		}

		String connectionFactoryBeanName = "connectionFactory";
		if (containerEle.hasAttribute(CONNECTION_FACTORY_ATTRIBUTE)) {
			connectionFactoryBeanName = containerEle.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
			if (!StringUtils.hasText(connectionFactoryBeanName)) {
				parserContext.getReaderContext().error(
						"Listener container 'connection-factory' attribute contains empty value.", containerEle);
			}
		}
		if (StringUtils.hasText(connectionFactoryBeanName)) {
			containerDef.getPropertyValues().add("connectionFactory",
					new RuntimeBeanReference(connectionFactoryBeanName));
		}

		String taskExecutorBeanName = containerEle.getAttribute(TASK_EXECUTOR_ATTRIBUTE);
		if (StringUtils.hasText(taskExecutorBeanName)) {
			containerDef.getPropertyValues().add("taskExecutor",
					new RuntimeBeanReference(taskExecutorBeanName));
		}

		String errorHandlerBeanName = containerEle.getAttribute(ERROR_HANDLER_ATTRIBUTE);
		if (StringUtils.hasText(errorHandlerBeanName)) {
			containerDef.getPropertyValues().add("errorHandler",
					new RuntimeBeanReference(errorHandlerBeanName));
		}

		String destinationResolverBeanName = containerEle.getAttribute(DESTINATION_RESOLVER_ATTRIBUTE);
		if (StringUtils.hasText(destinationResolverBeanName)) {
			containerDef.getPropertyValues().add("destinationResolver",
					new RuntimeBeanReference(destinationResolverBeanName));
		}

		String cache = containerEle.getAttribute(CACHE_ATTRIBUTE);
		if (StringUtils.hasText(cache)) {
			if (containerType.startsWith("simple")) {
				if (!("auto".equals(cache) || "consumer".equals(cache))) {
					parserContext.getReaderContext().warning(
							"'cache' attribute not actively supported for listener container of type \"simple\". " +
							"Effective runtime behavior will be equivalent to \"consumer\" / \"auto\".", containerEle);
				}
			}
			else {
				containerDef.getPropertyValues().add("cacheLevelName", "CACHE_" + cache.toUpperCase());
			}
		}

		Integer acknowledgeMode = parseAcknowledgeMode(containerEle, parserContext);
		if (acknowledgeMode != null) {
			if (acknowledgeMode == Session.SESSION_TRANSACTED) {
				containerDef.getPropertyValues().add("sessionTransacted", Boolean.TRUE);
			}
			else {
				containerDef.getPropertyValues().add("sessionAcknowledgeMode", acknowledgeMode);
			}
		}

		String transactionManagerBeanName = containerEle.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE);
		if (StringUtils.hasText(transactionManagerBeanName)) {
			if (containerType.startsWith("simple")) {
				parserContext.getReaderContext().error(
						"'transaction-manager' attribute not supported for listener container of type \"simple\".", containerEle);
			}
			else {
				containerDef.getPropertyValues().add("transactionManager",
						new RuntimeBeanReference(transactionManagerBeanName));
			}
		}

		String concurrency = containerEle.getAttribute(CONCURRENCY_ATTRIBUTE);
		if (StringUtils.hasText(concurrency)) {
			containerDef.getPropertyValues().add("concurrency", concurrency);
		}

		String prefetch = containerEle.getAttribute(PREFETCH_ATTRIBUTE);
		if (StringUtils.hasText(prefetch)) {
			if (containerType.startsWith("default")) {
				containerDef.getPropertyValues().add("maxMessagesPerTask", prefetch);
			}
		}

		String receiveTimeout = containerEle.getAttribute(RECEIVE_TIMEOUT_ATTRIBUTE);
		if (StringUtils.hasText(receiveTimeout)) {
			if (containerType.startsWith("default")) {
				containerDef.getPropertyValues().add("receiveTimeout", receiveTimeout);
			}
		}

		String recoveryInterval = containerEle.getAttribute(RECOVERY_INTERVAL_ATTRIBUTE);
		if (StringUtils.hasText(recoveryInterval)) {
			if (containerType.startsWith("default")) {
				containerDef.getPropertyValues().add("recoveryInterval", recoveryInterval);
			}
		}

		String phase = containerEle.getAttribute(PHASE_ATTRIBUTE);
		if (StringUtils.hasText(phase)) {
			containerDef.getPropertyValues().add("phase", phase);
		}

		return containerDef;
	}

	@Override
	protected boolean indicatesPubSub(BeanDefinition containerDef) {
		return indicatesPubSubConfig(containerDef);
	}

}
