/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * A {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * for the JMS namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 */
public class JmsNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		registerBeanDefinitionParser("listener-container", new JmsListenerContainerParser());
		registerBeanDefinitionParser("jca-listener-container", new JcaListenerContainerParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenJmsBeanDefinitionParser());
	}

}
