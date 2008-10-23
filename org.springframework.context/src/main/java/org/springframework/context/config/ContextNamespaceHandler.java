/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.JdkVersion;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * for the '<code>context</code>' namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 */
public class ContextNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser("property-placeholder", new PropertyPlaceholderBeanDefinitionParser());
		registerBeanDefinitionParser("property-override", new PropertyOverrideBeanDefinitionParser());
		registerJava5DependentParser("annotation-config",
				"org.springframework.context.annotation.AnnotationConfigBeanDefinitionParser");
		registerJava5DependentParser("component-scan",
				"org.springframework.context.annotation.ComponentScanBeanDefinitionParser");
		registerBeanDefinitionParser("load-time-weaver", new LoadTimeWeaverBeanDefinitionParser());
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
		registerBeanDefinitionParser("mbean-export", new MBeanExportBeanDefinitionParser());
		registerBeanDefinitionParser("mbean-server", new MBeanServerBeanDefinitionParser());
	}

	private void registerJava5DependentParser(final String elementName, final String parserClassName) {
		BeanDefinitionParser parser = null;
		if (JdkVersion.isAtLeastJava15()) {
			try {
				Class parserClass = ClassUtils.forName(parserClassName, ContextNamespaceHandler.class.getClassLoader());
				parser = (BeanDefinitionParser) parserClass.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Unable to create Java 1.5 dependent parser: " + parserClassName, ex);
			}
		}
		else {
			parser = new BeanDefinitionParser() {
				public BeanDefinition parse(Element element, ParserContext parserContext) {
					throw new IllegalStateException("Context namespace element '" + elementName +
							"' and its parser class [" + parserClassName + "] are only available on JDK 1.5 and higher");
				}
			};
		}
		registerBeanDefinitionParser(elementName, parser);
	}

}
