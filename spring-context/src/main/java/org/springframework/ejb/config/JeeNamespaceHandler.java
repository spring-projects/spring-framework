/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ejb.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * for the '{@code jee}' namespace.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class JeeNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("jndi-lookup", new JndiLookupBeanDefinitionParser());
		registerBeanDefinitionParser("local-slsb", new LocalStatelessSessionBeanDefinitionParser());
		registerBeanDefinitionParser("remote-slsb", new RemoteStatelessSessionBeanDefinitionParser());
	}

}
