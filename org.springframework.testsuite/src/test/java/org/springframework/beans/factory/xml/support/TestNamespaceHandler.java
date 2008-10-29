/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.factory.xml.support;

import org.springframework.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator;
import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.aop.interceptor.NopInterceptor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Rob Harrop
 */
public class TestNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser("testBean", new TestBeanDefinitionParser());
		registerBeanDefinitionParser("person", new PersonDefinitionParser());

		registerBeanDefinitionDecorator("set", new PropertyModifyingBeanDefinitionDecorator());
		registerBeanDefinitionDecorator("debug", new DebugBeanDefinitionDecorator());
		registerBeanDefinitionDecorator("nop", new NopInterceptorBeanDefinitionDecorator());
		registerBeanDefinitionDecoratorForAttribute("object-name", new ObjectNameBeanDefinitionDecorator());
	}

	private static class TestBeanDefinitionParser implements BeanDefinitionParser {

		public BeanDefinition parse(Element element, ParserContext parserContext) {
			RootBeanDefinition definition = new RootBeanDefinition();
			definition.setBeanClass(TestBean.class);

			MutablePropertyValues mpvs = new MutablePropertyValues();
			mpvs.addPropertyValue("name", element.getAttribute("name"));
			mpvs.addPropertyValue("age", element.getAttribute("age"));
			definition.setPropertyValues(mpvs);

			parserContext.getRegistry().registerBeanDefinition(element.getAttribute("id"), definition);

			return null;
		}
	}

	private static final class PersonDefinitionParser extends AbstractSingleBeanDefinitionParser {

		protected Class getBeanClass(Element element) {
			return TestBean.class;
		}

		protected void doParse(Element element, BeanDefinitionBuilder builder) {
			builder.addPropertyValue("name", element.getAttribute("name"));
			builder.addPropertyValue("age", element.getAttribute("age"));
		}
	}

	private static class PropertyModifyingBeanDefinitionDecorator implements BeanDefinitionDecorator {

		public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
			Element element = (Element) node;
			BeanDefinition def = definition.getBeanDefinition();

			MutablePropertyValues mpvs = (def.getPropertyValues() == null) ? new MutablePropertyValues() : def.getPropertyValues();
			mpvs.addPropertyValue("name", element.getAttribute("name"));
			mpvs.addPropertyValue("age", element.getAttribute("age"));

			((AbstractBeanDefinition) def).setPropertyValues(mpvs);
			return definition;
		}
	}

	private static class DebugBeanDefinitionDecorator extends AbstractInterceptorDrivenBeanDefinitionDecorator {

		protected BeanDefinition createInterceptorDefinition(Node node) {
			return new RootBeanDefinition(DebugInterceptor.class);
		}
	}

	private static class NopInterceptorBeanDefinitionDecorator extends AbstractInterceptorDrivenBeanDefinitionDecorator {

		protected BeanDefinition createInterceptorDefinition(Node node) {
			return new RootBeanDefinition(NopInterceptor.class);
		}
	}

	private static class ObjectNameBeanDefinitionDecorator implements BeanDefinitionDecorator {

		public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
			Attr objectNameAttribute = (Attr) node;
			definition.getBeanDefinition().setAttribute("objectName", objectNameAttribute.getValue());
			return definition;
		}
	}
}
