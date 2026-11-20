/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.endpoint;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;

/**
 * Strategy interface for creating JCA 1.5 ActivationSpec objects
 * based on a configured {@link JmsActivationSpecConfig} object.
 *
 * <p>JCA 1.5 ActivationSpec objects are typically JavaBeans, but
 * unfortunately provider-specific. This strategy interface allows
 * for plugging in any JCA-based JMS provider, creating corresponding
 * ActivationSpec objects based on common JMS configuration settings.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see JmsActivationSpecConfig
 * @see JmsMessageEndpointManager#setActivationSpecFactory
 * @see jakarta.resource.spi.ResourceAdapter#endpointActivation
 */
public interface JmsActivationSpecFactory {

	/**
	 * Create a JCA 1.5 ActivationSpec object based on the given
	 * {@link JmsActivationSpecConfig} object.
	 * @param adapter the ResourceAdapter to create an ActivationSpec object for
	 * @param config the configured object holding common JMS settings
	 * @return the provider-specific JCA ActivationSpec object,
	 * representing the same settings
	 */
	ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config);

}
