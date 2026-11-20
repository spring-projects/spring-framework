/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StringUtils;

/**
 * Parser for the {@code <context:property-placeholder/>} element.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @since 2.5
 */
class PropertyPlaceholderBeanDefinitionParser extends AbstractPropertyLoadingBeanDefinitionParser {

	private static final String SYSTEM_PROPERTIES_MODE_ATTRIBUTE = "system-properties-mode";

	private static final String SYSTEM_PROPERTIES_MODE_DEFAULT = "ENVIRONMENT";


	@Override
	@SuppressWarnings({"deprecation", "removal"})
	protected Class<?> getBeanClass(Element element) {
		// The default value of system-properties-mode is 'ENVIRONMENT'. This value
		// indicates that resolution of placeholders against system properties is a
		// function of the Environment and its current set of PropertySources.
		if (SYSTEM_PROPERTIES_MODE_DEFAULT.equals(element.getAttribute(SYSTEM_PROPERTIES_MODE_ATTRIBUTE))) {
			return PropertySourcesPlaceholderConfigurer.class;
		}

		// The user has explicitly specified a value for system-properties-mode: revert to
		// PropertyPlaceholderConfigurer to ensure backward compatibility with 3.0 and earlier.
		// This is deprecated; to be removed along with PropertyPlaceholderConfigurer itself.
		return org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		builder.addPropertyValue("ignoreUnresolvablePlaceholders",
				Boolean.valueOf(element.getAttribute("ignore-unresolvable")));

		String systemPropertiesModeName = element.getAttribute(SYSTEM_PROPERTIES_MODE_ATTRIBUTE);
		if (StringUtils.hasLength(systemPropertiesModeName) &&
				!systemPropertiesModeName.equals(SYSTEM_PROPERTIES_MODE_DEFAULT)) {
			builder.addPropertyValue("systemPropertiesModeName", "SYSTEM_PROPERTIES_MODE_" + systemPropertiesModeName);
		}

		if (element.hasAttribute("value-separator")) {
			builder.addPropertyValue("valueSeparator", element.getAttribute("value-separator"));
		}
		if (element.hasAttribute("trim-values")) {
			builder.addPropertyValue("trimValues", element.getAttribute("trim-values"));
		}
		if (element.hasAttribute("null-value")) {
			builder.addPropertyValue("nullValue", element.getAttribute("null-value"));
		}
	}

}
