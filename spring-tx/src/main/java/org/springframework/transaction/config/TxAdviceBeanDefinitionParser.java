/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.interceptor.*;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 *
 * 标签 <tx:advice/> 的 BeanDefinition 解析器
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @author Chris Beams
 * @since 2.0
 */
class TxAdviceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String METHOD_ELEMENT = "method";
	private static final String METHOD_NAME_ATTRIBUTE = "name";
	private static final String ATTRIBUTES_ELEMENT = "attributes";
	private static final String TIMEOUT_ATTRIBUTE = "timeout";
	private static final String READ_ONLY_ATTRIBUTE = "read-only";
	private static final String PROPAGATION_ATTRIBUTE = "propagation";
	private static final String ISOLATION_ATTRIBUTE = "isolation";
	private static final String ROLLBACK_FOR_ATTRIBUTE = "rollback-for";
	private static final String NO_ROLLBACK_FOR_ATTRIBUTE = "no-rollback-for";

	@Override
	protected Class<?> getBeanClass(Element element) {
		return TransactionInterceptor.class; // 创建的是 TransactionInterceptor 对象
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
	    // 设置 transactionManager 属性为 <tx:transaction-manager /> 标签对应的 Bean 对象。
		builder.addPropertyReference("transactionManager", TxNamespaceHandler.getTransactionManagerName(element));

		// 解析 <tx:attributes /> 标签
		List<Element> txAttributes = DomUtils.getChildElementsByTagName(element, ATTRIBUTES_ELEMENT);
		if (txAttributes.size() > 1) { // 大于 1 ，报错。只允许有一个
			parserContext.getReaderContext().error(
					"Element <attributes> is allowed at most once inside element <advice>", element);
		} else if (txAttributes.size() == 1) { // 等于 1 个，进行解析
			// Using attributes source.
			Element attributeSourceElement = txAttributes.get(0);
			RootBeanDefinition attributeSourceDefinition = parseAttributeSource(attributeSourceElement, parserContext);
			// 设置 transactionAttributeSource 属性为 NameMatchTransactionAttributeSource 对象。
			builder.addPropertyValue("transactionAttributeSource", attributeSourceDefinition);
		} else { // 等于 0 ，假设有注解，使用 AnnotationTransactionAttributeSource 。TODO 芋艿，这个情况，还没调试过 AnnotationTransactionAttributeSource
			// Assume annotations source.
            // 设置 transactionAttributeSource 属性为 AnnotationTransactionAttributeSource 对象。
			builder.addPropertyValue("transactionAttributeSource",
					new RootBeanDefinition("org.springframework.transaction.annotation.AnnotationTransactionAttributeSource"));
		}
	}

	private RootBeanDefinition parseAttributeSource(Element attrEle, ParserContext parserContext) {
	    // 解析 <tx:method /> 标签
		List<Element> methods = DomUtils.getChildElementsByTagName(attrEle, METHOD_ELEMENT);
		ManagedMap<TypedStringValue, RuleBasedTransactionAttribute> transactionAttributeMap =
				new ManagedMap<>(methods.size());
		transactionAttributeMap.setSource(parserContext.extractSource(attrEle));

		// 遍历 <tx:method /> 标签
		for (Element methodEle : methods) {
		    // 创建 TypedStringValue 对象，会存储到 transactionAttributeMap 中
			String name = methodEle.getAttribute(METHOD_NAME_ATTRIBUTE); // name 属性
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(methodEle));

			// 创建 RuleBasedTransactionAttribute 对象
			RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
			String propagation = methodEle.getAttribute(PROPAGATION_ATTRIBUTE); // propagation 属性
			String isolation = methodEle.getAttribute(ISOLATION_ATTRIBUTE); // isolation 属性
			String timeout = methodEle.getAttribute(TIMEOUT_ATTRIBUTE); // timeout 属性
			String readOnly = methodEle.getAttribute(READ_ONLY_ATTRIBUTE); // readOnly 属性
			if (StringUtils.hasText(propagation)) {
				attribute.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + propagation);
			}
			if (StringUtils.hasText(isolation)) {
				attribute.setIsolationLevelName(RuleBasedTransactionAttribute.PREFIX_ISOLATION + isolation);
			}
			if (StringUtils.hasText(timeout)) {
				try {
					attribute.setTimeout(Integer.parseInt(timeout));
				} catch (NumberFormatException ex) {
					parserContext.getReaderContext().error("Timeout must be an integer value: [" + timeout + "]", methodEle);
				}
			}
			if (StringUtils.hasText(readOnly)) {
				attribute.setReadOnly(Boolean.valueOf(methodEle.getAttribute(READ_ONLY_ATTRIBUTE)));
			}

			List<RollbackRuleAttribute> rollbackRules = new LinkedList<>(); // rollback 规则集合
			if (methodEle.hasAttribute(ROLLBACK_FOR_ATTRIBUTE)) { // rollback-for 属性
				String rollbackForValue = methodEle.getAttribute(ROLLBACK_FOR_ATTRIBUTE);
				addRollbackRuleAttributesTo(rollbackRules,rollbackForValue);
			}
			if (methodEle.hasAttribute(NO_ROLLBACK_FOR_ATTRIBUTE)) { // no-rollback-for 属性
				String noRollbackForValue = methodEle.getAttribute(NO_ROLLBACK_FOR_ATTRIBUTE);
				addNoRollbackRuleAttributesTo(rollbackRules,noRollbackForValue);
			}
			attribute.setRollbackRules(rollbackRules);

			// 添加到 transactionAttributeMap 中
			transactionAttributeMap.put(nameHolder, attribute);
		}

		// 创建 RootBeanDefinition 对象，beanClass 为 NameMatchTransactionAttributeSource 类。
		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchTransactionAttributeSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(attrEle));
		attributeSourceDefinition.getPropertyValues().add("nameMap", transactionAttributeMap); // 设置 nameMap 属性
		return attributeSourceDefinition;
	}

	private void addRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String rollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(rollbackForValue); // , 分隔
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new RollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

	private void addNoRollbackRuleAttributesTo(List<RollbackRuleAttribute> rollbackRules, String noRollbackForValue) {
		String[] exceptionTypeNames = StringUtils.commaDelimitedListToStringArray(noRollbackForValue); // , 分隔
		for (String typeName : exceptionTypeNames) {
			rollbackRules.add(new NoRollbackRuleAttribute(StringUtils.trimWhitespace(typeName)));
		}
	}

}
