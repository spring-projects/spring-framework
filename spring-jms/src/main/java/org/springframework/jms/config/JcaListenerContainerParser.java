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

import org.w3c.dom.Element;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the JMS {@code &lt;jca-listener-container&gt;} element.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 */
class JcaListenerContainerParser extends AbstractListenerContainerParser {

	private static final String RESOURCE_ADAPTER_ATTRIBUTE = "resource-adapter";

	private static final String ACTIVATION_SPEC_FACTORY_ATTRIBUTE = "activation-spec-factory";


	@Override
	protected PropertyValues parseProperties(Element containerEle, ParserContext parserContext) {
		final MutablePropertyValues properties = new MutablePropertyValues();
		PropertyValues containerValues = parseContainerProperties(containerEle, parserContext);

		// Common values are added to the activationSpecConfig
		PropertyValues commonValues = parseCommonContainerProperties(containerEle, parserContext);
		BeanDefinition beanDefinition = getActivationSpecConfigBeanDefinition(containerValues);
		beanDefinition.getPropertyValues().addPropertyValues(commonValues);

		properties.addPropertyValues(containerValues);
		return properties;
	}

	@Override
	protected RootBeanDefinition createContainerFactory(String factoryId,
			Element containerElement, PropertyValues propertyValues) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setBeanClassName("org.springframework.jms.config.DefaultJcaListenerContainerFactory");
		beanDefinition.getPropertyValues().addPropertyValues(propertyValues);
		return beanDefinition;
	}

	@Override
	protected BeanDefinition createContainer(ListenerContainerParserContext context) {
		RootBeanDefinition containerDef = new RootBeanDefinition();
		containerDef.setSource(context.getSource());
		containerDef.setBeanClassName("org.springframework.jms.listener.endpoint.JmsMessageEndpointManager");

		containerDef.getPropertyValues().addPropertyValues(context.getContainerValues());


		BeanDefinition activationSpec = getActivationSpecConfigBeanDefinition(context.getContainerValues());
		parseListenerConfiguration(context.getListenerElement(), context.getParserContext(), activationSpec);

		String phase = context.getContainerElement().getAttribute(PHASE_ATTRIBUTE);
		if (StringUtils.hasText(phase)) {
			containerDef.getPropertyValues().add("phase", phase);
		}

		return containerDef;
	}

	@Override
	protected boolean indicatesPubSub(PropertyValues propertyValues) {
		BeanDefinition configDef = getActivationSpecConfigBeanDefinition(propertyValues);
		return indicatesPubSubConfig(configDef.getPropertyValues());
	}

	@Override
	protected PropertyValue getMessageConverter(PropertyValues containerValues) {
		BeanDefinition configDef = getActivationSpecConfigBeanDefinition(containerValues);
		return super.getMessageConverter(configDef.getPropertyValues());
	}

	private BeanDefinition getActivationSpecConfigBeanDefinition(PropertyValues containerValues) {
		PropertyValue activationSpecConfig = containerValues.getPropertyValue("activationSpecConfig");
		return (BeanDefinition) activationSpecConfig.getValue();
	}

	private PropertyValues parseContainerProperties(Element containerEle,
			ParserContext parserContext) {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		if (containerEle.hasAttribute(RESOURCE_ADAPTER_ATTRIBUTE)) {
			String resourceAdapterBeanName = containerEle.getAttribute(RESOURCE_ADAPTER_ATTRIBUTE);
			if (!StringUtils.hasText(resourceAdapterBeanName)) {
				parserContext.getReaderContext().error(
						"Listener container 'resource-adapter' attribute contains empty value.", containerEle);
			}
			else {
				propertyValues.add("resourceAdapter",
						new RuntimeBeanReference(resourceAdapterBeanName));
			}
		}

		String activationSpecFactoryBeanName = containerEle.getAttribute(ACTIVATION_SPEC_FACTORY_ATTRIBUTE);
		String destinationResolverBeanName = containerEle.getAttribute(DESTINATION_RESOLVER_ATTRIBUTE);
		if (StringUtils.hasText(activationSpecFactoryBeanName)) {
			if (StringUtils.hasText(destinationResolverBeanName)) {
				parserContext.getReaderContext().error("Specify either 'activation-spec-factory' or " +
						"'destination-resolver', not both. If you define a dedicated JmsActivationSpecFactory bean, " +
						"specify the custom DestinationResolver there (if possible).", containerEle);
			}
			propertyValues.add("activationSpecFactory",
					new RuntimeBeanReference(activationSpecFactoryBeanName));
		}
		if (StringUtils.hasText(destinationResolverBeanName)) {
			propertyValues.add("destinationResolver",
					new RuntimeBeanReference(destinationResolverBeanName));
		}

		String transactionManagerBeanName = containerEle.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE);
		if (StringUtils.hasText(transactionManagerBeanName)) {
			propertyValues.add("transactionManager",
					new RuntimeBeanReference(transactionManagerBeanName));
		}

		RootBeanDefinition configDef = new RootBeanDefinition();
		configDef.setSource(parserContext.extractSource(configDef));
		configDef.setBeanClassName("org.springframework.jms.listener.endpoint.JmsActivationSpecConfig");


		Integer acknowledgeMode = parseAcknowledgeMode(containerEle, parserContext);
		if (acknowledgeMode != null) {
			configDef.getPropertyValues().add("acknowledgeMode", acknowledgeMode);
		}
		String concurrency = containerEle.getAttribute(CONCURRENCY_ATTRIBUTE);
		if (StringUtils.hasText(concurrency)) {
			configDef.getPropertyValues().add("concurrency", concurrency);
		}

		String prefetch = containerEle.getAttribute(PREFETCH_ATTRIBUTE);
		if (StringUtils.hasText(prefetch)) {
			configDef.getPropertyValues().add("prefetchSize", new Integer(prefetch));
		}

		if (containerEle.hasAttribute(MESSAGE_CONVERTER_ATTRIBUTE)) {
			String messageConverter = containerEle.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE);
			if (!StringUtils.hasText(messageConverter)) {
				parserContext.getReaderContext().error(
						"listener container 'message-converter' attribute contains empty value.", containerEle);
			}
			else {
				configDef.getPropertyValues().add("messageConverter",
						new RuntimeBeanReference(messageConverter));
			}
		}

		propertyValues.add("activationSpecConfig", configDef);

		return propertyValues;
	}

}
