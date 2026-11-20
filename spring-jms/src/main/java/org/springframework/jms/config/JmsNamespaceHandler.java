/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * A {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * for the JMS namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 */
public class JmsNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("listener-container", new JmsListenerContainerParser());
		registerBeanDefinitionParser("jca-listener-container", new JcaListenerContainerParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenJmsBeanDefinitionParser());
	}

}
