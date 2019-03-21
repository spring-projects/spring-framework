/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.ejb.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Simple {@link org.springframework.beans.factory.xml.BeanDefinitionParser} implementation that
 * translates {@code jndi-lookup} tag into {@link JndiObjectFactoryBean} definitions.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see JndiObjectFactoryBean
 */
class JndiLookupBeanDefinitionParser extends AbstractJndiLocatingBeanDefinitionParser {

	public static final String DEFAULT_VALUE = "default-value";

	public static final String DEFAULT_REF = "default-ref";

	public static final String DEFAULT_OBJECT = "defaultObject";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return JndiObjectFactoryBean.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return (super.isEligibleAttribute(attributeName) &&
				!DEFAULT_VALUE.equals(attributeName) && !DEFAULT_REF.equals(attributeName));
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		String defaultValue = element.getAttribute(DEFAULT_VALUE);
		String defaultRef = element.getAttribute(DEFAULT_REF);
		if (StringUtils.hasLength(defaultValue)) {
			if (StringUtils.hasLength(defaultRef)) {
				parserContext.getReaderContext().error("<jndi-lookup> element is only allowed to contain either " +
						"'default-value' attribute OR 'default-ref' attribute, not both", element);
			}
			builder.addPropertyValue(DEFAULT_OBJECT, defaultValue);
		}
		else if (StringUtils.hasLength(defaultRef)) {
			builder.addPropertyValue(DEFAULT_OBJECT, new RuntimeBeanReference(defaultRef));
		}
	}

}
