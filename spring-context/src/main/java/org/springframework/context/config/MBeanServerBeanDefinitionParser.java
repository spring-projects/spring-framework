/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.WebSphereMBeanServerFactoryBean;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;context:mbean-server/&gt; element.
 *
 * <p>Registers an instance of
 * {@link org.springframework.jmx.export.annotation.AnnotationMBeanExporter}
 * within the context.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.jmx.export.annotation.AnnotationMBeanExporter
 */
class MBeanServerBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String MBEAN_SERVER_BEAN_NAME = "mbeanServer";

	private static final String AGENT_ID_ATTRIBUTE = "agent-id";


	private static final boolean weblogicPresent = ClassUtils.isPresent(
			"weblogic.management.Helper", MBeanServerBeanDefinitionParser.class.getClassLoader());

	private static final boolean webspherePresent = ClassUtils.isPresent(
			"com.ibm.websphere.management.AdminServiceFactory", MBeanServerBeanDefinitionParser.class.getClassLoader());


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		String id = element.getAttribute(ID_ATTRIBUTE);
		return (StringUtils.hasText(id) ? id : MBEAN_SERVER_BEAN_NAME);
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String agentId = element.getAttribute(AGENT_ID_ATTRIBUTE);
		if (StringUtils.hasText(agentId)) {
			RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
			bd.getPropertyValues().add("agentId", agentId);
			return bd;
		}
		AbstractBeanDefinition specialServer = findServerForSpecialEnvironment();
		if (specialServer != null) {
			return specialServer;
		}
		RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
		bd.getPropertyValues().add("locateExistingServerIfPossible", Boolean.TRUE);

		// Mark as infrastructure bean and attach source location.
		bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		bd.setSource(parserContext.extractSource(element));
		return bd;
	}

	static AbstractBeanDefinition findServerForSpecialEnvironment() {
		if (weblogicPresent) {
			RootBeanDefinition bd = new RootBeanDefinition(JndiObjectFactoryBean.class);
			bd.getPropertyValues().add("jndiName", "java:comp/env/jmx/runtime");
			return bd;
		}
		else if (webspherePresent) {
			return new RootBeanDefinition(WebSphereMBeanServerFactoryBean.class);
		}
		else {
			return null;
		}
	}

}
