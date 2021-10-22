/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jms.config;

import jakarta.jms.Session;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Abstract parser for JMS listener container elements, providing support for
 * common properties that are identical for all listener container variants.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 */
abstract class AbstractListenerContainerParser implements BeanDefinitionParser {

	protected static final String FACTORY_ID_ATTRIBUTE = "factory-id";

	protected static final String LISTENER_ELEMENT = "listener";

	protected static final String ID_ATTRIBUTE = "id";

	protected static final String DESTINATION_ATTRIBUTE = "destination";

	protected static final String SUBSCRIPTION_ATTRIBUTE = "subscription";

	protected static final String SELECTOR_ATTRIBUTE = "selector";

	protected static final String REF_ATTRIBUTE = "ref";

	protected static final String METHOD_ATTRIBUTE = "method";

	protected static final String DESTINATION_RESOLVER_ATTRIBUTE = "destination-resolver";

	protected static final String MESSAGE_CONVERTER_ATTRIBUTE = "message-converter";

	protected static final String RESPONSE_DESTINATION_ATTRIBUTE = "response-destination";

	protected static final String DESTINATION_TYPE_ATTRIBUTE = "destination-type";

	protected static final String DESTINATION_TYPE_QUEUE = "queue";

	protected static final String DESTINATION_TYPE_TOPIC = "topic";

	protected static final String DESTINATION_TYPE_DURABLE_TOPIC = "durableTopic";

	protected static final String DESTINATION_TYPE_SHARED_TOPIC = "sharedTopic";

	protected static final String DESTINATION_TYPE_SHARED_DURABLE_TOPIC = "sharedDurableTopic";

	protected static final String RESPONSE_DESTINATION_TYPE_ATTRIBUTE = "response-destination-type";

	protected static final String CLIENT_ID_ATTRIBUTE = "client-id";

	protected static final String ACKNOWLEDGE_ATTRIBUTE = "acknowledge";

	protected static final String ACKNOWLEDGE_AUTO = "auto";

	protected static final String ACKNOWLEDGE_CLIENT = "client";

	protected static final String ACKNOWLEDGE_DUPS_OK = "dups-ok";

	protected static final String ACKNOWLEDGE_TRANSACTED = "transacted";

	protected static final String TRANSACTION_MANAGER_ATTRIBUTE = "transaction-manager";

	protected static final String CONCURRENCY_ATTRIBUTE = "concurrency";

	protected static final String PHASE_ATTRIBUTE = "phase";

	protected static final String PREFETCH_ATTRIBUTE = "prefetch";


	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		CompositeComponentDefinition compositeDef =
				new CompositeComponentDefinition(element.getTagName(), parserContext.extractSource(element));
		parserContext.pushContainingComponent(compositeDef);

		MutablePropertyValues commonProperties = parseCommonContainerProperties(element, parserContext);
		MutablePropertyValues specificProperties = parseSpecificContainerProperties(element, parserContext);

		String factoryId = element.getAttribute(FACTORY_ID_ATTRIBUTE);
		if (StringUtils.hasText(factoryId)) {
			RootBeanDefinition beanDefinition = createContainerFactory(
					factoryId, element, parserContext, commonProperties, specificProperties);
			if (beanDefinition != null) {
				beanDefinition.setSource(parserContext.extractSource(element));
				parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinition, factoryId));
			}
		}

		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String localName = parserContext.getDelegate().getLocalName(child);
				if (LISTENER_ELEMENT.equals(localName)) {
					parseListener(element, (Element) child, parserContext, commonProperties, specificProperties);
				}
			}
		}

		parserContext.popAndRegisterContainingComponent();
		return null;
	}

	private void parseListener(Element containerEle, Element listenerEle, ParserContext parserContext,
			MutablePropertyValues commonContainerProperties, PropertyValues specificContainerProperties) {

		RootBeanDefinition listenerDef = new RootBeanDefinition();
		listenerDef.setSource(parserContext.extractSource(listenerEle));
		listenerDef.setBeanClassName("org.springframework.jms.listener.adapter.MessageListenerAdapter");

		String ref = listenerEle.getAttribute(REF_ATTRIBUTE);
		if (!StringUtils.hasText(ref)) {
			parserContext.getReaderContext().error(
					"Listener 'ref' attribute contains empty value.", listenerEle);
		}
		else {
			listenerDef.getPropertyValues().add("delegate", new RuntimeBeanReference(ref));
		}

		if (listenerEle.hasAttribute(METHOD_ATTRIBUTE)) {
			String method = listenerEle.getAttribute(METHOD_ATTRIBUTE);
			if (!StringUtils.hasText(method)) {
				parserContext.getReaderContext().error(
						"Listener 'method' attribute contains empty value.", listenerEle);
			}
			listenerDef.getPropertyValues().add("defaultListenerMethod", method);
		}

		PropertyValue messageConverterPv = commonContainerProperties.getPropertyValue("messageConverter");
		if (messageConverterPv != null) {
			listenerDef.getPropertyValues().addPropertyValue(messageConverterPv);
		}

		BeanDefinition containerDef = createContainer(
				containerEle, listenerEle, parserContext, commonContainerProperties, specificContainerProperties);
		containerDef.getPropertyValues().add("messageListener", listenerDef);

		if (listenerEle.hasAttribute(RESPONSE_DESTINATION_ATTRIBUTE)) {
			String responseDestination = listenerEle.getAttribute(RESPONSE_DESTINATION_ATTRIBUTE);
			Boolean pubSubDomain = (Boolean) commonContainerProperties.get("replyPubSubDomain");
			if (pubSubDomain == null) {
				pubSubDomain = false;
			}
			listenerDef.getPropertyValues().add(
					pubSubDomain ? "defaultResponseTopicName" : "defaultResponseQueueName", responseDestination);
			PropertyValue destinationResolver = containerDef.getPropertyValues().getPropertyValue("destinationResolver");
			if (destinationResolver != null) {
				listenerDef.getPropertyValues().addPropertyValue(destinationResolver);
			}
		}


		String containerBeanName = listenerEle.getAttribute(ID_ATTRIBUTE);
		// If no bean id is given auto generate one using the ReaderContext's BeanNameGenerator
		if (!StringUtils.hasText(containerBeanName)) {
			containerBeanName = parserContext.getReaderContext().generateBeanName(containerDef);
		}

		// Register the listener and fire event
		parserContext.registerBeanComponent(new BeanComponentDefinition(containerDef, containerBeanName));
	}

	protected void parseListenerConfiguration(Element ele, ParserContext parserContext, MutablePropertyValues configValues) {
		String destination = ele.getAttribute(DESTINATION_ATTRIBUTE);
		if (!StringUtils.hasText(destination)) {
			parserContext.getReaderContext().error(
					"Listener 'destination' attribute contains empty value.", ele);
		}
		configValues.add("destinationName", destination);

		if (ele.hasAttribute(SUBSCRIPTION_ATTRIBUTE)) {
			String subscription = ele.getAttribute(SUBSCRIPTION_ATTRIBUTE);
			if (!StringUtils.hasText(subscription)) {
				parserContext.getReaderContext().error(
						"Listener 'subscription' attribute contains empty value.", ele);
			}
			configValues.add("subscriptionName", subscription);
		}

		if (ele.hasAttribute(SELECTOR_ATTRIBUTE)) {
			String selector = ele.getAttribute(SELECTOR_ATTRIBUTE);
			if (!StringUtils.hasText(selector)) {
				parserContext.getReaderContext().error(
						"Listener 'selector' attribute contains empty value.", ele);
			}
			configValues.add("messageSelector", selector);
		}

		if (ele.hasAttribute(CONCURRENCY_ATTRIBUTE)) {
			String concurrency = ele.getAttribute(CONCURRENCY_ATTRIBUTE);
			if (!StringUtils.hasText(concurrency)) {
				parserContext.getReaderContext().error(
						"Listener 'concurrency' attribute contains empty value.", ele);
			}
			configValues.add("concurrency", concurrency);
		}
	}

	protected MutablePropertyValues parseCommonContainerProperties(Element containerEle, ParserContext parserContext) {
		MutablePropertyValues properties = new MutablePropertyValues();

		String destinationType = containerEle.getAttribute(DESTINATION_TYPE_ATTRIBUTE);
		boolean pubSubDomain = false;
		boolean subscriptionDurable = false;
		boolean subscriptionShared = false;
		if (DESTINATION_TYPE_SHARED_DURABLE_TOPIC.equals(destinationType)) {
			pubSubDomain = true;
			subscriptionDurable = true;
			subscriptionShared = true;
		}
		else if (DESTINATION_TYPE_SHARED_TOPIC.equals(destinationType)) {
			pubSubDomain = true;
			subscriptionShared = true;
		}
		else if (DESTINATION_TYPE_DURABLE_TOPIC.equals(destinationType)) {
			pubSubDomain = true;
			subscriptionDurable = true;
		}
		else if (DESTINATION_TYPE_TOPIC.equals(destinationType)) {
			pubSubDomain = true;
		}
		else if (!StringUtils.hasLength(destinationType) || DESTINATION_TYPE_QUEUE.equals(destinationType)) {
			// the default: queue
		}
		else {
			parserContext.getReaderContext().error("Invalid listener container 'destination-type': only " +
					"\"queue\", \"topic\", \"durableTopic\", \"sharedTopic\", \"sharedDurableTopic\" supported.", containerEle);
		}
		properties.add("pubSubDomain", pubSubDomain);
		properties.add("subscriptionDurable", subscriptionDurable);
		properties.add("subscriptionShared", subscriptionShared);

		boolean replyPubSubDomain = false;
		String replyDestinationType = containerEle.getAttribute(RESPONSE_DESTINATION_TYPE_ATTRIBUTE);
		if (!StringUtils.hasText(replyDestinationType)) {
			replyPubSubDomain = pubSubDomain;  // the default: same value as pubSubDomain
		}
		else if (DESTINATION_TYPE_TOPIC.equals(replyDestinationType)) {
			replyPubSubDomain = true;
		}
		else if (!DESTINATION_TYPE_QUEUE.equals(replyDestinationType)) {
			parserContext.getReaderContext().error("Invalid listener container 'response-destination-type': only " +
					"\"queue\", \"topic\" supported.", containerEle);
		}
		properties.add("replyPubSubDomain", replyPubSubDomain);

		if (containerEle.hasAttribute(CLIENT_ID_ATTRIBUTE)) {
			String clientId = containerEle.getAttribute(CLIENT_ID_ATTRIBUTE);
			if (!StringUtils.hasText(clientId)) {
				parserContext.getReaderContext().error(
						"Listener 'client-id' attribute contains empty value.", containerEle);
			}
			properties.add("clientId", clientId);
		}

		if (containerEle.hasAttribute(MESSAGE_CONVERTER_ATTRIBUTE)) {
			String messageConverter = containerEle.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE);
			if (!StringUtils.hasText(messageConverter)) {
				parserContext.getReaderContext().error(
						"listener container 'message-converter' attribute contains empty value.", containerEle);
			}
			else {
				properties.add("messageConverter", new RuntimeBeanReference(messageConverter));
			}
		}

		return properties;
	}


	/**
	 * Parse the common properties for all listeners as defined by the specified
	 * container {@link Element}.
	 */
	protected abstract MutablePropertyValues parseSpecificContainerProperties(Element containerEle, ParserContext parserContext);

	/**
	 * Create the {@link BeanDefinition} for the container factory using the specified
	 * shared property values.
	 */
	@Nullable
	protected abstract RootBeanDefinition createContainerFactory(String factoryId, Element containerEle, ParserContext parserContext,
			PropertyValues commonContainerProperties, PropertyValues specificContainerProperties);

	/**
	 * Create the container {@link BeanDefinition} for the specified context.
	 */
	protected abstract RootBeanDefinition createContainer(Element containerEle, Element listenerEle, ParserContext parserContext,
			PropertyValues commonContainerProperties, PropertyValues specificContainerProperties);


	@Nullable
	protected Integer parseAcknowledgeMode(Element ele, ParserContext parserContext) {
		String acknowledge = ele.getAttribute(ACKNOWLEDGE_ATTRIBUTE);
		if (StringUtils.hasText(acknowledge)) {
			int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
			if (ACKNOWLEDGE_TRANSACTED.equals(acknowledge)) {
				acknowledgeMode = Session.SESSION_TRANSACTED;
			}
			else if (ACKNOWLEDGE_DUPS_OK.equals(acknowledge)) {
				acknowledgeMode = Session.DUPS_OK_ACKNOWLEDGE;
			}
			else if (ACKNOWLEDGE_CLIENT.equals(acknowledge)) {
				acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;
			}
			else if (!ACKNOWLEDGE_AUTO.equals(acknowledge)) {
				parserContext.getReaderContext().error("Invalid listener container 'acknowledge' setting [" +
						acknowledge + "]: only \"auto\", \"client\", \"dups-ok\" and \"transacted\" supported.", ele);
			}
			return acknowledgeMode;
		}
		else {
			return null;
		}
	}

}
