/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the JMS {@code <jca-listener-container>} element.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 */
class JcaListenerContainerParser extends AbstractListenerContainerParser {

	private static final String RESOURCE_ADAPTER_ATTRIBUTE = "resource-adapter";

	private static final String ACTIVATION_SPEC_FACTORY_ATTRIBUTE = "activation-spec-factory";


	@Override
	protected RootBeanDefinition createContainerFactory(String factoryId, Element containerEle, ParserContext parserContext,
			PropertyValues commonContainerProperties, PropertyValues specificContainerProperties) {

		RootBeanDefinition factoryDef = new RootBeanDefinition();
		factoryDef.setBeanClassName("org.springframework.jms.config.DefaultJcaListenerContainerFactory");

		factoryDef.getPropertyValues().addPropertyValues(commonContainerProperties);
		factoryDef.getPropertyValues().addPropertyValues(specificContainerProperties);

		return factoryDef;
	}

	@Override
	protected RootBeanDefinition createContainer(Element containerEle, Element listenerEle, ParserContext parserContext,
			PropertyValues commonContainerProperties, PropertyValues specificContainerProperties) {

		RootBeanDefinition containerDef = new RootBeanDefinition();
		containerDef.setSource(parserContext.extractSource(containerEle));
		containerDef.setBeanClassName("org.springframework.jms.listener.endpoint.JmsMessageEndpointManager");
		containerDef.getPropertyValues().addPropertyValues(specificContainerProperties);

		RootBeanDefinition configDef = new RootBeanDefinition();
		configDef.setSource(parserContext.extractSource(containerEle));
		configDef.setBeanClassName("org.springframework.jms.listener.endpoint.JmsActivationSpecConfig");
		configDef.getPropertyValues().addPropertyValues(commonContainerProperties);
		parseListenerConfiguration(listenerEle, parserContext, configDef.getPropertyValues());

		containerDef.getPropertyValues().add("activationSpecConfig", configDef);

		return containerDef;
	}

	@Override
	protected MutablePropertyValues parseCommonContainerProperties(Element containerEle, ParserContext parserContext) {
		MutablePropertyValues properties = super.parseCommonContainerProperties(containerEle, parserContext);

		Integer acknowledgeMode = parseAcknowledgeMode(containerEle, parserContext);
		if (acknowledgeMode != null) {
			properties.add("acknowledgeMode", acknowledgeMode);
		}

		String concurrency = containerEle.getAttribute(CONCURRENCY_ATTRIBUTE);
		if (StringUtils.hasText(concurrency)) {
			properties.add("concurrency", concurrency);
		}

		String prefetch = containerEle.getAttribute(PREFETCH_ATTRIBUTE);
		if (StringUtils.hasText(prefetch)) {
			properties.add("prefetchSize", Integer.valueOf(prefetch));
		}

		return properties;
	}

	@Override
	protected MutablePropertyValues parseSpecificContainerProperties(Element containerEle, ParserContext parserContext) {
		MutablePropertyValues properties = new MutablePropertyValues();

		if (containerEle.hasAttribute(RESOURCE_ADAPTER_ATTRIBUTE)) {
			String resourceAdapterBeanName = containerEle.getAttribute(RESOURCE_ADAPTER_ATTRIBUTE);
			if (!StringUtils.hasText(resourceAdapterBeanName)) {
				parserContext.getReaderContext().error(
						"Listener container 'resource-adapter' attribute contains empty value.", containerEle);
			}
			else {
				properties.add("resourceAdapter", new RuntimeBeanReference(resourceAdapterBeanName));
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
			properties.add("activationSpecFactory", new RuntimeBeanReference(activationSpecFactoryBeanName));
		}
		if (StringUtils.hasText(destinationResolverBeanName)) {
			properties.add("destinationResolver", new RuntimeBeanReference(destinationResolverBeanName));
		}

		String transactionManagerBeanName = containerEle.getAttribute(TRANSACTION_MANAGER_ATTRIBUTE);
		if (StringUtils.hasText(transactionManagerBeanName)) {
			properties.add("transactionManager", new RuntimeBeanReference(transactionManagerBeanName));
		}

		String phase = containerEle.getAttribute(PHASE_ATTRIBUTE);
		if (StringUtils.hasText(phase)) {
			properties.add("phase", phase);
		}

		return properties;
	}

}
