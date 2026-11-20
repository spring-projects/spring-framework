/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jmx.support.MBeanServerFactoryBean;
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

		RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
		bd.getPropertyValues().add("locateExistingServerIfPossible", Boolean.TRUE);

		// Mark as infrastructure bean and attach source location.
		bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		bd.setSource(parserContext.extractSource(element));
		return bd;
	}

}
