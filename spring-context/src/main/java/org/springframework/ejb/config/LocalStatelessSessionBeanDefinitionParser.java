/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.ejb.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanUtils;
import org.springframework.jndi.JndiObjectFactoryBean;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 * implementation for parsing '{@code local-slsb}' tags and
 * creating plain {@link JndiObjectFactoryBean} definitions on 6.0.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
class LocalStatelessSessionBeanDefinitionParser extends AbstractJndiLocatingBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return JndiObjectFactoryBean.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return (super.isEligibleAttribute(attributeName) &&
				BeanUtils.getPropertyDescriptor(JndiObjectFactoryBean.class, extractPropertyName(attributeName)) != null);
	}

}
