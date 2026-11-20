/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerEndpointRegistry;

/**
 * {@code @Configuration} class that registers a {@link JmsListenerAnnotationBeanPostProcessor}
 * bean capable of processing Spring's {@link JmsListener @JmsListener} annotation.
 * Also registers a default {@link JmsListenerEndpointRegistry}.
 *
 * <p>This configuration class is automatically imported when using the
 * {@code @EnableJms} annotation. See the {@link EnableJms @EnableJms}
 * for complete usage details.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JmsListenerAnnotationBeanPostProcessor
 * @see JmsListenerEndpointRegistry
 * @see EnableJms
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class JmsBootstrapConfiguration {

	@Bean(name = JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JmsListenerAnnotationBeanPostProcessor jmsListenerAnnotationProcessor() {
		return new JmsListenerAnnotationBeanPostProcessor();
	}

	@Bean(name = JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)
	public JmsListenerEndpointRegistry defaultJmsListenerEndpointRegistry() {
		return new JmsListenerEndpointRegistry();
	}

}
