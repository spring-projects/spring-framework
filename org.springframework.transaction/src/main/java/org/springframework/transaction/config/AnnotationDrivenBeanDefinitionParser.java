/*
 * Copyright 2002-2011 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.config.AbstractSpecificationBeanDefinitionParser;
import org.springframework.context.config.FeatureSpecification;
import org.springframework.transaction.annotation.TransactionManagementCapability;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} implementation that allows users to easily configure
 * all the infrastructure beans required to enable annotation-driven transaction
 * demarcation.
 *
 * <p>By default, all proxies are created as JDK proxies. This may cause some
 * problems if you are injecting objects as concrete classes rather than
 * interfaces. To overcome this restriction you can set the
 * '<code>proxy-target-class</code>' attribute to '<code>true</code>', which
 * will result in class-based proxies being created.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * The bean name of the internally managed transaction advisor (mode="proxy").
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link TransactionManagementCapability#TRANSACTION_ADVISOR_BEAN_NAME}
	 */
	@Deprecated
	public static final String TRANSACTION_ADVISOR_BEAN_NAME =
			TransactionManagementCapability.TRANSACTION_ADVISOR_BEAN_NAME;

	/**
	 * The bean name of the internally managed transaction aspect (mode="aspectj").
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link TransactionManagementCapability#TRANSACTION_ASPECT_BEAN_NAME}
	 */
	@Deprecated
	public static final String TRANSACTION_ASPECT_BEAN_NAME =
			TransactionManagementCapability.TRANSACTION_ASPECT_BEAN_NAME;


	/**
	 * Parses the {@code <tx:annotation-driven/>} tag. Will
	 * {@link AopNamespaceUtils#registerAutoProxyCreatorIfNecessary register an AutoProxyCreator}
	 * with the container as necessary.
	 */
	@Override
	protected FeatureSpecification doParse(Element element, ParserContext parserContext) {
		return new TxAnnotationDriven(element.getAttribute("transaction-manager"))
			.order(element.getAttribute("order"))
			.mode(element.getAttribute("mode"))
			.proxyTargetClass(Boolean.valueOf(element.getAttribute("proxy-target-class")));
	}

}
