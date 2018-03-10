/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;

/**
 * Standard implementation of the {@link JmsActivationSpecFactory} interface.
 * Supports the standard JMS properties as defined by the JMS 1.5 specification
 * (Appendix B); ignores Spring's "maxConcurrency" and "prefetchSize" settings.
 *
 * <p>The 'activationSpecClass' property is required, explicitly defining
 * the fully-qualified class name of the provider's ActivationSpec class
 * (e.g. "org.apache.activemq.ra.ActiveMQActivationSpec").
 *
 * <p>Check out {@link DefaultJmsActivationSpecFactory} for an extended variant
 * of this class, supporting some further default conventions beyond the plain
 * JMS 1.5 specification.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setActivationSpecClass
 * @see DefaultJmsActivationSpecFactory
 */
public class StandardJmsActivationSpecFactory implements JmsActivationSpecFactory {

	@Nullable
	private Class<?> activationSpecClass;

	@Nullable
	private Map<String, String> defaultProperties;

	@Nullable
	private DestinationResolver destinationResolver;


	/**
	 * Specify the fully-qualified ActivationSpec class name for the target
	 * provider (e.g. "org.apache.activemq.ra.ActiveMQActivationSpec").
	 */
	public void setActivationSpecClass(Class<?> activationSpecClass) {
		this.activationSpecClass = activationSpecClass;
	}

	/**
	 * Specify custom default properties, with String keys and String values.
	 * <p>Applied to each ActivationSpec object before it gets populated with
	 * listener-specific settings. Allows for configuring vendor-specific properties
	 * beyond the Spring-defined settings in {@link JmsActivationSpecConfig}.
	 */
	public void setDefaultProperties(Map<String, String> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}

	/**
	 * Set the DestinationResolver to use for resolving destination names
	 * into the JCA 1.5 ActivationSpec "destination" property.
	 * <p>If not specified, destination names will simply be passed in as Strings.
	 * If specified, destination names will be resolved into Destination objects first.
	 * <p>Note that a DestinationResolver for use with this factory must be
	 * able to work <i>without</i> an active JMS Session: e.g.
	 * {@link org.springframework.jms.support.destination.JndiDestinationResolver}
	 * or {@link org.springframework.jms.support.destination.BeanFactoryDestinationResolver}
	 * but not {@link org.springframework.jms.support.destination.DynamicDestinationResolver}.
	 */
	public void setDestinationResolver(@Nullable DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Return the {@link DestinationResolver} to use for resolving destinations names.
	 */
	@Nullable
	public DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}


	@Override
	public ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config) {
		Class<?> activationSpecClassToUse = this.activationSpecClass;
		if (activationSpecClassToUse == null) {
			activationSpecClassToUse = determineActivationSpecClass(adapter);
			if (activationSpecClassToUse == null) {
				throw new IllegalStateException("Property 'activationSpecClass' is required");
			}
		}

		ActivationSpec spec = (ActivationSpec) BeanUtils.instantiateClass(activationSpecClassToUse);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(spec);
		if (this.defaultProperties != null) {
			bw.setPropertyValues(this.defaultProperties);
		}
		populateActivationSpecProperties(bw, config);
		return spec;
	}

	/**
	 * Determine the ActivationSpec class for the given ResourceAdapter,
	 * if possible. Called if no 'activationSpecClass' has been set explicitly
	 * @param adapter the ResourceAdapter to check
	 * @return the corresponding ActivationSpec class, or {@code null}
	 * if not determinable
	 * @see #setActivationSpecClass
	 */
	@Nullable
	protected Class<?> determineActivationSpecClass(ResourceAdapter adapter) {
		return null;
	}

	/**
	 * Populate the given ApplicationSpec object with the settings
	 * defined in the given configuration object.
	 * <p>This implementation applies all standard JMS settings, but ignores
	 * "maxConcurrency" and "prefetchSize" - not supported in standard JCA 1.5.
	 * @param bw the BeanWrapper wrapping the ActivationSpec object
	 * @param config the configured object holding common JMS settings
	 */
	protected void populateActivationSpecProperties(BeanWrapper bw, JmsActivationSpecConfig config) {
		String destinationName = config.getDestinationName();
		if (destinationName != null) {
			boolean pubSubDomain = config.isPubSubDomain();
			Object destination = destinationName;
			if (this.destinationResolver != null) {
				try {
					destination = this.destinationResolver.resolveDestinationName(null, destinationName, pubSubDomain);
				}
				catch (JMSException ex) {
					throw new DestinationResolutionException(
							"Cannot resolve destination name [" + destinationName + "]", ex);
				}
			}
			bw.setPropertyValue("destination", destination);
			bw.setPropertyValue("destinationType", pubSubDomain ? Topic.class.getName() : Queue.class.getName());
		}

		if (bw.isWritableProperty("subscriptionDurability")) {
			bw.setPropertyValue("subscriptionDurability", config.isSubscriptionDurable() ? "Durable" : "NonDurable");
		}
		else if (config.isSubscriptionDurable()) {
			// Standard JCA 1.5 "subscriptionDurability" apparently not supported...
			throw new IllegalArgumentException("Durable subscriptions not supported by underlying provider");
		}
		if (config.isSubscriptionShared()) {
			throw new IllegalArgumentException("Shared subscriptions not supported for JCA-driven endpoints");
		}

		if (config.getSubscriptionName() != null) {
			bw.setPropertyValue("subscriptionName", config.getSubscriptionName());
		}
		if (config.getClientId() != null) {
			bw.setPropertyValue("clientId", config.getClientId());
		}
		if (config.getMessageSelector() != null) {
			bw.setPropertyValue("messageSelector", config.getMessageSelector());
		}
		applyAcknowledgeMode(bw, config.getAcknowledgeMode());
	}

	/**
	 * Apply the specified acknowledge mode to the ActivationSpec object.
	 * <p>This implementation applies the standard JCA 1.5 acknowledge modes
	 * "Auto-acknowledge" and "Dups-ok-acknowledge". It throws an exception in
	 * case of {@code CLIENT_ACKNOWLEDGE} or {@code SESSION_TRANSACTED}
	 * having been requested.
	 * @param bw the BeanWrapper wrapping the ActivationSpec object
	 * @param ackMode the configured acknowledge mode
	 * (according to the constants in {@link javax.jms.Session}
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#SESSION_TRANSACTED
	 */
	protected void applyAcknowledgeMode(BeanWrapper bw, int ackMode) {
		if (ackMode == Session.SESSION_TRANSACTED) {
			throw new IllegalArgumentException("No support for SESSION_TRANSACTED: Only \"Auto-acknowledge\" " +
					"and \"Dups-ok-acknowledge\" supported in standard JCA 1.5");
		}
		else if (ackMode == Session.CLIENT_ACKNOWLEDGE) {
			throw new IllegalArgumentException("No support for CLIENT_ACKNOWLEDGE: Only \"Auto-acknowledge\" " +
					"and \"Dups-ok-acknowledge\" supported in standard JCA 1.5");
		}
		else if (bw.isWritableProperty("acknowledgeMode")) {
			bw.setPropertyValue("acknowledgeMode",
					ackMode == Session.DUPS_OK_ACKNOWLEDGE ? "Dups-ok-acknowledge" : "Auto-acknowledge");
		}
		else if (ackMode == Session.DUPS_OK_ACKNOWLEDGE) {
			// Standard JCA 1.5 "acknowledgeMode" apparently not supported (e.g. WebSphere MQ 6.0.2.1)
			throw new IllegalArgumentException("Dups-ok-acknowledge not supported by underlying provider");
		}
	}

}
