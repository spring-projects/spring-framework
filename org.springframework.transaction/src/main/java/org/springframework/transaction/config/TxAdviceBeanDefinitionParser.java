/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.transaction.config;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 * for the <code>&lt;tx:advice&gt;</code> tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 2.0
 */
class TxAdviceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String ATTRIBUTES = "attributes";

	private static final String TIMEOUT = "timeout";

	private static final String READ_ONLY = "read-only";

	private static final String NAME_MAP = "nameMap";

	private static final String PROPAGATION = "propagation";

	private static final String ISOLATION = "isolation";

	private static final String ROLLBACK_FOR = "rollback-for";

	private static final String NO_ROLLBACK_FOR = "no-rollback-for";


	protected Class getBeanClass(Element element) {
		return TransactionInterceptor.class;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// Set the transaction manager property.
		String transactionManagerName = (element.hasAttribute(TxNamespaceUtils.TRANSACTION_MANAGER_ATTRIBUTE) ?
				element.getAttribute(TxNamespaceUtils.TRANSACTION_MANAGER_ATTRIBUTE) : "transactionManager");
		builder.addPropertyReference(TxNamespaceUtils.TRANSACTION_MANAGER_PROPERTY, transactionManagerName);

		List txAttributes = DomUtils.getChildElementsByTagName(element, ATTRIBUTES);
		if (txAttributes.size() > 1) {
			parserContext.getReaderContext().error(
					"Element <attributes> is allowed at most once inside element <advice>", element);
		}
		else if (txAttributes.size() == 1) {
			// Using attributes source.
			Element attributeSourceElement = (Element) txAttributes.get(0);
			RootBeanDefinition attributeSourceDefinition = parseAttributeSource(attributeSourceElement, parserContext);
			builder.addPropertyValue(TxNamespaceUtils.TRANSACTION_ATTRIBUTE_SOURCE, attributeSourceDefinition);
		}
		else {
			// Assume annotations source.
			Class sourceClass = TxNamespaceUtils.getAnnotationTransactionAttributeSourceClass();
			builder.addPropertyValue(TxNamespaceUtils.TRANSACTION_ATTRIBUTE_SOURCE, new RootBeanDefinition(sourceClass));
		}
	}

	private RootBeanDefinition parseAttributeSource(Element attrEle, ParserContext parserContext) {
		List methods = DomUtils.getChildElementsByTagName(attrEle, "method");
		ManagedMap transactionAttributeMap = new ManagedMap(methods.size());
		transactionAttributeMap.setSource(parserContext.extractSource(attrEle));

		for (int i = 0; i < methods.size(); i++) {
			Element methodEle = (Element) methods.get(i);

			String name = methodEle.getAttribute("name");
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(methodEle));

			RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
			String propagation = methodEle.getAttribute(PROPAGATION);
			String isolation = methodEle.getAttribute(ISOLATION);
			String timeout = methodEle.getAttribute(TIMEOUT);
			String readOnly = methodEle.getAttribute(READ_ONLY);
			if (StringUtils.hasText(propagation)) {
				attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
			}
			if (StringUtils.hasText(isolation)) {
				attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
			}
			if (StringUtils.hasText(timeout)) {
				try {
					attribute.setTimeout(Integer.parseInt(timeout));
				}
				catch (NumberFormatException ex) {
					parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]", methodEle);
				}
			}
			if (StringUtils.hasText(readOnly)) {
				attribute.setReadOnly(Boolean.valueOf(methodEle.getAttribute(READ_ONLY)).booleanValue());
			}

			List rollbackRules = new LinkedList();
			if (methodEle.hasAttribute(ROLLBACK_FOR)) {
				String rollbackForValue = methodEle.getAttribute(ROLLBACK_FOR);
				addRollbackRuleAttributesTo(rollbackRules,rollbackForValue);
			}
			if (methodEle.hasAttribute(NO_ROLLBACK_FOR)) {
				String noRollbackForValue = methodEle.getAttribute(NO_ROLLBACK_FOR);
				addNoRollbackRuleAttributesTo(rollbackRules,noRollbackForValue);
			}
			attribute.setRollbackRules(rollbackRules);

			transactionAttributeMap.put(nameHolder, attribute);
		}

		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchTransactionAttributeSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(attrEle));
		attributeSourceDefinition.getPropertyValues().addPropertyValue(NAME_MAP, transactionAttributeMap);
		return attributeSourceDefinition;
	}

	private void addRollbackRuleAttributesTo(List rollbackRules, String rollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
		for (int i = 0; i < exceptionTypeNames.length; i++) {
			rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(exceptionTypeNames[i])));
		}
	}

	private void addNoRollbackRuleAttributesTo(List rollbackRules, String noRollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
		for (int i = 0; i < exceptionTypeNames.length; i++) {
			rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(exceptionTypeNames[i])));
		}
	}

}
