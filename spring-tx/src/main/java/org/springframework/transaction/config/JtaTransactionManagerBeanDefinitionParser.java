/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
