/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenient base class for when there exists a one-to-one mapping
 * between attribute names on the element that is to be parsed and
 * the property names on the {@link Class} being configured.
 *
 * <p>Extend this parser class when you want to create a single
 * bean definition from a relatively simple custom XML element. The
 * resulting {@code BeanDefinition} will be automatically
 * registered with the relevant
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
 *
 * <p>An example will hopefully make the use of this particular parser
 * class immediately clear. Consider the following class definition:
 *
 * <pre class="code">public class SimpleCache implements Cache {
 *
 *     public void setName(String name) {...}
 *     public void setTimeout(int timeout) {...}
 *     public void setEvictionPolicy(EvictionPolicy policy) {...}
 *
 *     // remaining class definition elided for clarity...
 * }</pre>
 *
 * <p>Then let us assume the following XML tag has been defined to
 * permit the easy configuration of instances of the above class;
 *
 * <pre class="code">&lt;caching:cache name="..." timeout="..." eviction-policy="..."/&gt;</pre>
 *
 * <p>All that is required of the Java developer tasked with writing
 * the parser to parse the above XML tag into an actual
 * {@code SimpleCache} bean definition is the following:
 *
 * <pre class="code">public class SimpleCacheBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {
 *
 *     protected Class getBeanClass(Element element) {
 *         return SimpleCache.class;
 *     }
 * }</pre>
 *
 * <p>Please note that the {@code AbstractSimpleBeanDefinitionParser}
 * is limited to populating the created bean definition with property values.
 * if you want to parse constructor arguments and nested elements from the
 * supplied XML element, then you will have to implement the
 * {@link #postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.w3c.dom.Element)}
 * method and do such parsing yourself, or (more likely) subclass the
 * {@link AbstractSingleBeanDefinitionParser} or {@link AbstractBeanDefinitionParser}
 * classes directly.
 *
 * <p>The process of actually registering the
 * {@code SimpleCacheBeanDefinitionParser} with the Spring XML parsing
 * infrastructure is described in the Spring Framework reference documentation
 * (in one of the appendices).
 *
 * <p>For an example of this parser in action (so to speak), do look at
 * the source code for the
 * {@link org.springframework.beans.factory.xml.UtilNamespaceHandler.PropertiesBeanDefinitionParser};
 * the observant (and even not so observant) reader will immediately notice that
 * there is next to no code in the implementation. The
 * {@code PropertiesBeanDefinitionParser} populates a
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean}
 * from an XML element that looks like this:
 *
 * <pre class="code">&lt;util:properties location="jdbc.properties"/&gt;</pre>
 *
 * <p>The observant reader will notice that the sole attribute on the
 * {@code &lt;util:properties/&gt;} element matches the
 * {@link org.springframework.beans.factory.config.PropertiesFactoryBean#setLocation(org.springframework.core.io.Resource)}
 * method name on the {@code PropertiesFactoryBean} (the general
 * usage thus illustrated holds true for any number of attributes).
 * All that the {@code PropertiesBeanDefinitionParser} needs
 * actually do is supply an implementation of the
 * {@link #getBeanClass(org.w3c.dom.Element)} method to return the
 * {@code PropertiesFactoryBean} type.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 * @see Conventions#attributeNameToPropertyName(String)
 */
public abstract class AbstractSimpleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * Parse the supplied {@link Element} and populate the supplied
	 * {@link BeanDefinitionBuilder} as required.
	 * <p>This implementation maps any attributes present on the
	 * supplied element to {@link org.springframework.beans.PropertyValue}
	 * instances, and
	 * {@link BeanDefinitionBuilder#addPropertyValue(String, Object) adds them}
	 * to the
	 * {@link org.springframework.beans.factory.config.BeanDefinition builder}.
	 * <p>The {@link #extractPropertyName(String)} method is used to
	 * reconcile the name of an attribute with the name of a JavaBean
	 * property.
	 * @param element the XML element being parsed
	 * @param builder used to define the {@code BeanDefinition}
	 * @see #extractPropertyName(String)
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		NamedNodeMap attributes = element.getAttributes();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			if (isEligibleAttribute(attribute, parserContext)) {
				String propertyName = extractPropertyName(attribute.getLocalName());
				Assert.state(StringUtils.hasText(propertyName),
						"Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
				builder.addPropertyValue(propertyName, attribute.getValue());
			}
		}
		postProcess(builder, element);
	}

	/**
	 * Determine whether the given attribute is eligible for being
	 * turned into a corresponding bean property value.
	 * <p>The default implementation considers any attribute as eligible,
	 * except for the "id" attribute and namespace declaration attributes.
	 * @param attribute the XML attribute to check
	 * @param parserContext the {@code ParserContext}
	 * @see #isEligibleAttribute(String)
	 */
	protected boolean isEligibleAttribute(Attr attribute, ParserContext parserContext) {
		boolean eligible = isEligibleAttribute(attribute);
		if(!eligible) {
			String fullName = attribute.getName();
			eligible = (!fullName.equals("xmlns") && !fullName.startsWith("xmlns:") &&
					isEligibleAttribute(parserContext.getDelegate().getLocalName(attribute)));
		}
		return eligible;
	}

	/**
	 * Determine whether the given attribute is eligible for being
	 * turned into a corresponding bean property value.
	 * <p>The default implementation considers any attribute as eligible,
	 * except for the "id" attribute and namespace declaration attributes.
	 * @param attribute the XML attribute to check
	 * @see #isEligibleAttribute(String)
	 * @deprecated in favour of {@link #isEligibleAttribute(org.w3c.dom.Attr, ParserContext)}
	 */
	@Deprecated
	protected boolean isEligibleAttribute(Attr attribute) {
		return false;
	}

	/**
	 * Determine whether the given attribute is eligible for being
	 * turned into a corresponding bean property value.
	 * <p>The default implementation considers any attribute as eligible,
	 * except for the "id" attribute.
	 * @param attributeName the attribute name taken straight from the
	 * XML element being parsed (never {@code null})
	 */
	protected boolean isEligibleAttribute(String attributeName) {
		return !ID_ATTRIBUTE.equals(attributeName);
	}

	/**
	 * Extract a JavaBean property name from the supplied attribute name.
	 * <p>The default implementation uses the
	 * {@link Conventions#attributeNameToPropertyName(String)}
	 * method to perform the extraction.
	 * <p>The name returned must obey the standard JavaBean property name
	 * conventions. For example for a class with a setter method
	 * '{@code setBingoHallFavourite(String)}', the name returned had
	 * better be '{@code bingoHallFavourite}' (with that exact casing).
	 * @param attributeName the attribute name taken straight from the
	 * XML element being parsed (never {@code null})
	 * @return the extracted JavaBean property name (must never be {@code null})
	 */
	protected String extractPropertyName(String attributeName) {
		return Conventions.attributeNameToPropertyName(attributeName);
	}

	/**
	 * Hook method that derived classes can implement to inspect/change a
	 * bean definition after parsing is complete.
	 * <p>The default implementation does nothing.
	 * @param beanDefinition the parsed (and probably totally defined) bean definition being built
	 * @param element the XML element that was the source of the bean definition's metadata
	 */
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
	}

}
