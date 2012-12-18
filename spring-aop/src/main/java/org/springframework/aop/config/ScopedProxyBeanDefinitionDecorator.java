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

package org.springframework.aop.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * {@link BeanDefinitionDecorator} responsible for parsing the
 * <code>&lt;aop:scoped-proxy/&gt;</code> tag.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 */
class ScopedProxyBeanDefinitionDecorator implements BeanDefinitionDecorator {

	private static final String PROXY_TARGET_CLASS = "proxy-target-class";


	public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		boolean proxyTargetClass = true;
		if (node instanceof Element) {
			Element ele = (Element) node;
			if (ele.hasAttribute(PROXY_TARGET_CLASS)) {
				proxyTargetClass = Boolean.valueOf(ele.getAttribute(PROXY_TARGET_CLASS));
			}
		}

		// Register the original bean definition as it will be referenced by the scoped proxy
		// and is relevant for tooling (validation, navigation).
		BeanDefinitionHolder holder =
				ScopedProxyUtils.createScopedProxy(definition, parserContext.getRegistry(), proxyTargetClass);
		String targetBeanName = ScopedProxyUtils.getTargetBeanName(definition.getBeanName());
		parserContext.getReaderContext().fireComponentRegistered(
				new BeanComponentDefinition(definition.getBeanDefinition(), targetBeanName));
		return holder;
	}

}
