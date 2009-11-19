/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
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


	@Override
	protected Class getBeanClass(Element element) {
		return TransactionInterceptor.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addPropertyReference("transactionManager", TxNamespaceHandler.getTransactionManagerName(element));

		List txAttributes = DomUtils.getChildElementsByTagName(element, ATTRIBUTES);
		if (txAttributes.size() > 1) {
			parserContext.getReaderContext().error(
					"Element <attributes> is allowed at most once inside element <advice>", element);
		}
		else if (txAttributes.size() == 1) {
			// Using attributes source.
			Element attributeSourceElement = (Element) txAttributes.get(0);
			RootBeanDefinition attributeSourceDefinition = parseAttributeSource(attributeSourceElement, parserContext);
			builder.addPropertyValue("transactionAttributeSource", attributeSourceDefinition);
		}
		else {
			// Assume annotations source.
			builder.addPropertyValue("transactionAttributeSource",
					new RootBeanDefinition(AnnotationTransactionAttributeSource.class));
		}
	}

	private RootBeanDefinition parseAttributeSource(Element attrEle, ParserContext parserContext) {
		List<Element> methods = DomUtils.getChildElementsByTagName(attrEle, "method");
		ManagedMap transactionAttributeMap = new ManagedMap(methods.size());
		transactionAttributeMap.setSource(parserContext.extractSource(attrEle));

		for (Element methodEle : methods) {
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
				attribute.setReadOnly(Boolean.valueOf(methodEle.getAttribute(READ_ONLY)));
			}

			List<RollbackRuleAttribute> rollbackRules = new LinkedList<RollbackRuleAttribute>();
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
		attributeSourceDefinition.getPropertyValues().add(NAME_MAP, transactionAttributeMap);
		return attributeSourceDefinition;
	}

	private void addRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String rollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

	private void addNoRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String noRollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue);
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

}
