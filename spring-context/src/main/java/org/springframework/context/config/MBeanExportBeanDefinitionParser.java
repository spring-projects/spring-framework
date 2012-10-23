/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.MBeanRegistrationSupport;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;context:mbean-export/&gt; element.
 *
 * <p>Registers an instance of
 * {@link org.springframework.jmx.export.annotation.AnnotationMBeanExporter}
 * within the context.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see org.springframework.jmx.export.annotation.AnnotationMBeanExporter
 */
class MBeanExportBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "mbeanExporter";

	private static final String DEFAULT_DOMAIN_ATTRIBUTE = "default-domain";

	private static final String SERVER_ATTRIBUTE = "server";

	private static final String REGISTRATION_ATTRIBUTE = "registration";

	private static final String REGISTRATION_IGNORE_EXISTING = "ignoreExisting";

	private static final String REGISTRATION_REPLACE_EXISTING = "replaceExisting";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return MBEAN_EXPORTER_BEAN_NAME;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AnnotationMBeanExporter.class);

		// Mark as infrastructure bean and attach source location.
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));

		String defaultDomain = element.getAttribute(DEFAULT_DOMAIN_ATTRIBUTE);
		if (StringUtils.hasText(defaultDomain)) {
			builder.addPropertyValue("defaultDomain", defaultDomain);
		}

		String serverBeanName = element.getAttribute(SERVER_ATTRIBUTE);
		if (StringUtils.hasText(serverBeanName)) {
			builder.addPropertyReference("server", serverBeanName);
		}
		else {
			AbstractBeanDefinition specialServer = MBeanServerBeanDefinitionParser.findServerForSpecialEnvironment();
			if (specialServer != null) {
				builder.addPropertyValue("server", specialServer);
			}
		}

		String registration = element.getAttribute(REGISTRATION_ATTRIBUTE);
		int registrationBehavior = MBeanRegistrationSupport.REGISTRATION_FAIL_ON_EXISTING;
		if (REGISTRATION_IGNORE_EXISTING.equals(registration)) {
			registrationBehavior = MBeanRegistrationSupport.REGISTRATION_IGNORE_EXISTING;
		}
		else if (REGISTRATION_REPLACE_EXISTING.equals(registration)) {
			registrationBehavior = MBeanRegistrationSupport.REGISTRATION_REPLACE_EXISTING;
		}
		builder.addPropertyValue("registrationBehavior", registrationBehavior);

		return builder.getBeanDefinition();
	}

}
