/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
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
 * @author Stephane Nicoll
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


	protected PropertyValues parseProperties(Element containerEle, ParserContext parserContext) {
		final MutablePropertyValues properties = new MutablePropertyValues();
		PropertyValues commonValues = parseCommonContainerProperties(containerEle, parserContext);
		PropertyValues containerValues = parseContainerProperties(containerEle,
				parserContext, isSimpleContainer(containerEle));
		properties.addPropertyValues(commonValues);
		properties.addPropertyValues(containerValues);
		return properties;
	}

	@Override
	protected RootBeanDefinition createContainerFactory(String factoryId, Element containerEle, PropertyValues propertyValues) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		String containerType = containerEle.getAttribute(CONTAINER_TYPE_ATTRIBUTE);
		String containerClass = containerEle.getAttribute(CONTAINER_CLASS_ATTRIBUTE);
		if (!"".equals(containerClass)) {
			return null; // Not supported
		}
		else if ("".equals(containerType) || containerType.startsWith("default")) {
			beanDefinition.setBeanClassName("org.springframework.jms.config.DefaultJmsListenerContainerFactory");
		}
		else if (containerType.startsWith("simple")) {
			beanDefinition.setBeanClassName("org.springframework.jms.config.SimpleJmsListenerContainerFactory");
		}
		beanDefinition.getPropertyValues().addPropertyValues(propertyValues);
		return beanDefinition;
	}

	@Override
	protected BeanDefinition createContainer(ListenerContainerParserContext context) {
		RootBeanDefinition containerDef = new RootBeanDefinition();
		containerDef.setSource(context.getSource());

		// Set all container values
		containerDef.getPropertyValues().addPropertyValues(context.getContainerValues());
		parseListenerConfiguration(context.getListenerElement(), context.getParserContext(), containerDef);

		Element containerEle = context.getContainerElement();
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
			context.getParserContext().getReaderContext().error(
					"Invalid 'container-type' attribute: only \"default\" and \"simple\" supported.", containerEle);
		}

		String phase = containerEle.getAttribute(PHASE_ATTRIBUTE);
		if (StringUtils.hasText(phase)) {
			containerDef.getPropertyValues().add("phase", phase);
		}

		return containerDef;
	}

	@Override
	protected boolean indicatesPubSub(PropertyValues propertyValues) {
		return indicatesPubSubConfig(propertyValues);
	}

	private PropertyValues parseContainerProperties(Element containerEle,
			ParserContext parserContext, boolean isSimpleContainer) {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		String connectionFactoryBeanName = "connectionFactory";
		if (containerEle.hasAttribute(CONNECTION_FACTORY_ATTRIBUTE)) {
			connectionFactoryBeanName = containerEle.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
			if (!StringUtils.hasText(connectionFactoryBeanName)) {
				parserContext.getReaderContext().error(
						"Listener container 'connection-factory' attribute contains empty value.", containerEle);
			}
		}
		if (StringUtils.hasText(connectionFactoryBeanName)) {
			propertyValues.add("connectionFactory",
					new RuntimeBeanReference(connectionFactoryBeanName));
		}

		String taskExecutorBeanName = containerEle.getAttribute(TASK_EXECUTOR_ATTRIBUTE);
		if (StringUtils.hasText(taskExecutorBeanName)) {
			propertyValues.add("taskExecutor",
					new RuntimeBeanReference(taskExecutorBeanName));
		}

		String errorHandlerBeanName = containerEle.getAttribute(ERROR_HANDLER_ATTRIBUTE);
		if (StringUtils.hasText(errorHandlerBeanName)) {
			propertyValues.add("errorHandler",
					new RuntimeBeanReference(errorHandlerBeanName));
		}

		if (containerEle.hasAttribute(MESSAGE_CONVERTER_ATTRIBUTE)) {
			String messageConverter = containerEle.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE);
			if (!StringUtils.hasText(messageConverter)) {
				parserContext.getReaderContext().error(
						"listener container 'message-converter' attribute contains empty value.", containerEle);
			}
			else {
				propertyValues.add("messageConverter",
						new RuntimeBeanReference(messageConverter));
			}
		}

		String destinationResolverBeanName = containerEle.getAttribute(DESTINATION_RESOLVER_ATTRIBUTE);
		if (StringUtils.hasText(destinationResolverBeanName)) {
			propertyValues.add("destinationResolver",
					new RuntimeBeanReference(destinationResolverBeanName));
		}

		String cache = containerEle.getAttribute(CACHE_ATTRIBUTE);
		if (StringUtils.hasText(cache)) {
			if (isSimpleContainer) {
				if (!("auto".equals(cache) || "consumer".equals(cache))) {
					parserContext.getReaderContext().warning(
							"'cache' attribute not actively supported for listener container of type \"simple\". " +
									"Effective runtime behavior will be equivalent to \"consumer\" / \"auto\".", containerEle);
				}
			}
			else {
				propertyValues.add("cacheLevelName", "CACHE_" + cache.toUpperCase());
			}
		}

		Integer acknowledgeMode = parseAcknowledgeMode(containerEle, parserContext);
		if (acknowledgeMode != null) {
			if (acknowledgeMode == Session.SESSION_TRANSACTED) {
				propertyValues.add("sessionTransacted", Boolean.TRUE);
			}
			else {
				propertyValues.add("sessionAcknowledgeMode", acknowledgeMode);
			}
		}

		String transactionManagerBeanName = containerEle.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE);
		if (StringUtils.hasText(transactionManagerBeanName)) {
			if (isSimpleContainer) {
				parserContext.getReaderContext().error(
						"'transaction-manager' attribute not supported for listener container of type \"simple\".", containerEle);
			}
			else {
				propertyValues.add("transactionManager",
						new RuntimeBeanReference(transactionManagerBeanName));
			}
		}

		String concurrency = containerEle.getAttribute(CONCURRENCY_ATTRIBUTE);
		if (StringUtils.hasText(concurrency)) {
			propertyValues.add("concurrency", concurrency);
		}

		String prefetch = containerEle.getAttribute(PREFETCH_ATTRIBUTE);
		if (StringUtils.hasText(prefetch)) {
			if (!isSimpleContainer) {
				propertyValues.add("maxMessagesPerTask", prefetch);
			}
		}

		String receiveTimeout = containerEle.getAttribute(RECEIVE_TIMEOUT_ATTRIBUTE);
		if (StringUtils.hasText(receiveTimeout)) {
			if (!isSimpleContainer) {
				propertyValues.add("receiveTimeout", receiveTimeout);
			}
		}

		String recoveryInterval = containerEle.getAttribute(RECOVERY_INTERVAL_ATTRIBUTE);
		if (StringUtils.hasText(recoveryInterval)) {
			if (!isSimpleContainer) {
				propertyValues.add("recoveryInterval", recoveryInterval);
			}
		}

		return propertyValues;
	}

	private boolean isSimpleContainer(Element containerEle) {
		String containerType = containerEle.getAttribute(CONTAINER_TYPE_ATTRIBUTE);
		return containerType.startsWith("simple");
	}

}
