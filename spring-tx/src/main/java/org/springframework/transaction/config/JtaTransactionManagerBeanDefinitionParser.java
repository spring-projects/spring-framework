/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.transaction.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Parser for the &lt;tx:jta-transaction-manager/&gt; XML configuration element.
 *
 * @author Juergen Hoeller
 * @author Christian Dupuis
 * @since 2.5
 */
public class JtaTransactionManagerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return JtaTransactionManager.class;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return TxNamespaceHandler.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME;
	}

}
