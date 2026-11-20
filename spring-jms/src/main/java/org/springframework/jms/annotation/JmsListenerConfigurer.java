/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.annotation;

import org.springframework.jms.config.JmsListenerEndpointRegistrar;

/**
 * Optional interface to be implemented by a Spring managed bean willing
 * to customize how JMS listener endpoints are configured. Typically
 * used to define the default {@link org.springframework.jms.config.JmsListenerContainerFactory
 * JmsListenerContainerFactory} to use or for registering JMS endpoints
 * in a <em>programmatic</em> fashion as opposed to the <em>declarative</em>
 * approach of using the {@link JmsListener @JmsListener} annotation.
 *
 * <p>See {@link EnableJms @EnableJms} for detailed usage examples.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see EnableJms
 * @see JmsListenerEndpointRegistrar
 */
@FunctionalInterface
public interface JmsListenerConfigurer {

	/**
	 * Callback allowing a {@link org.springframework.jms.config.JmsListenerEndpointRegistry
	 * JmsListenerEndpointRegistry} and specific {@link org.springframework.jms.config.JmsListenerEndpoint
	 * JmsListenerEndpoint} instances to be registered against the given
	 * {@link JmsListenerEndpointRegistrar}. The default
	 * {@link org.springframework.jms.config.JmsListenerContainerFactory JmsListenerContainerFactory}
	 * can also be customized.
	 * @param registrar the registrar to be configured
	 */
	void configureJmsListeners(JmsListenerEndpointRegistrar registrar);

}
