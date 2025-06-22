/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.scripting.config;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.DESTROY_METHOD_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.INIT_METHOD_ATTRIBUTE;
import static org.springframework.scripting.support.ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE;

public class ScriptBeanDefinitionParserUnitTests {

	@Test
	void parseInternalAddsScriptSourceAsFirstConstructorArgument() {
		Element element = createElement("classpath:test.groovy", null, null);
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		ValueHolder argument = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().get(0);
		assertThat(argument.getValue()).isNotNull().isEqualTo("classpath:test.groovy");
	}

	@Test
	void parseInternalWithScriptInterfacesAddsAsSecondConstructorArgument() {
		Element element = createElement("classpath:test.groovy", "com.example.TestInterface", null);
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition).isNotNull();
		ValueHolder argument = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().get(1);
		assertThat(argument.getValue()).isNotNull().isEqualTo("com.example.TestInterface");
		assertThat(argument.getType()).isEqualTo("java.lang.Class[]");
	}

	@Test
	void parseInternalWithCustomizerRefAddsAsConstructorArgument() {
		Element element = createElement("classpath:test.groovy", null, "customizerBean");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		ValueHolder argument = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().values().stream()
				.filter(vh -> vh.getValue() instanceof RuntimeBeanReference)
				.findFirst()
				.orElseThrow(() -> new AssertionError("No RuntimeBeanReference found in arguments"));

		assertThat(argument.getValue())
				.isInstanceOf(RuntimeBeanReference.class)
				.extracting("beanName")
				.isEqualTo("customizerBean");
	}

	@Test
	void parseInternalWithBothScriptInterfacesAndCustomizerRefAddsInCorrectOrder() {
		Element element = createElement("classpath:test.groovy", "com.example.TestInterface", "customizerBean");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		var arguments = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
		assertThat(arguments).hasSize(3);
		assertThat(arguments.get(0).getValue()).isEqualTo("classpath:test.groovy");
		assertThat(arguments.get(1).getValue()).isEqualTo("com.example.TestInterface");
		assertThat(arguments.get(2).getValue())
				.asInstanceOf(InstanceOfAssertFactories.type(RuntimeBeanReference.class))
				.extracting(RuntimeBeanReference::getBeanName)
				.isEqualTo("customizerBean");
	}

	@Test
	void parseInternalWithEngineAttributeAddsEngineFirst() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("engine")).thenReturn(true);
		when(element.getAttribute("engine")).thenReturn("groovy");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		var arguments = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
		assertThat(arguments.get(0).getValue()).isEqualTo("groovy");
		assertThat(arguments.get(1).getValue()).isEqualTo("classpath:test.groovy");
	}

	@Test
	void parseInternalWithRefreshCheckDelayIgnoresAttribute() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("refresh-check-delay")).thenReturn(true);
		when(element.getAttribute("refresh-check-delay")).thenReturn("5000");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAttribute("refresh-check-delay")).isNull();
	}

	@Test
	void parseInternalWithProxyTargetClassAttributeIgnored() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute(PROXY_TARGET_CLASS_ATTRIBUTE)).thenReturn(true);
		when(element.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE)).thenReturn("true");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE)).isNull();
	}

	@Test
	void parseInternalWithDependsOnSetsDependencies() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("depends-on")).thenReturn(true);
		when(element.getAttribute("depends-on")).thenReturn("bean1,bean2");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getDependsOn()).containsExactly("bean1", "bean2");
	}

	@Test
	void parseInternalWithEmptyCustomizerRefLogsError() {
		Element element = createElement("classpath:test.groovy", null, "");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		XmlReaderContext readerContext = mock(XmlReaderContext.class);
		ParserContext parserContext = mockParserContext();
		when(parserContext.getReaderContext()).thenReturn(readerContext);

		parser.parseInternal(element, parserContext);

		verify(readerContext).error(eq("Attribute 'customizer-ref' has empty value"), eq(element));
	}

	@Test
	void parseInternalWithInitAndDestroyMethodsSetsThem() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute(INIT_METHOD_ATTRIBUTE)).thenReturn(true);
		when(element.getAttribute(INIT_METHOD_ATTRIBUTE)).thenReturn("init");
		when(element.hasAttribute(DESTROY_METHOD_ATTRIBUTE)).thenReturn(true);
		when(element.getAttribute(DESTROY_METHOD_ATTRIBUTE)).thenReturn("destroy");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getInitMethodName()).isEqualTo("init");
		assertThat(beanDefinition.getDestroyMethodName()).isEqualTo("destroy");
	}

	@Test
	void parseInternalWithBothScriptSourceAndInlineScriptLogsError() {
		Element element = createElement("classpath:test.groovy", null, null);
		NodeList nodeList = mock(NodeList.class);
		when(nodeList.getLength()).thenReturn(1);
		when(element.getElementsByTagName("inline-script")).thenReturn(nodeList);

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		XmlReaderContext readerContext = mock(XmlReaderContext.class);
		ParserContext parserContext = mockParserContext();
		when(parserContext.getReaderContext()).thenReturn(readerContext);

		assertThat(parser.parseInternal(element, parserContext)).isNotNull();
	}

	@Test
	void parseInternalWithScopeAttributeSetsScope() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("scope")).thenReturn(true);
		when(element.getAttribute("scope")).thenReturn("prototype");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getScope()).isEqualTo("prototype");
	}

	@Test
	void parseInternalWithAutowireAttributeSetsMode() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("autowire")).thenReturn(true);
		when(element.getAttribute("autowire")).thenReturn("byName");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAutowireMode()).isEqualTo(AbstractBeanDefinition.AUTOWIRE_NO);
	}

	@Test
	void parseInternalWithAutowireConstructorSetsMode() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("autowire")).thenReturn(true);
		when(element.getAttribute("autowire")).thenReturn("constructor");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAutowireMode()).isEqualTo(AbstractBeanDefinition.AUTOWIRE_NO);
	}

	@Test
	void parseInternalWithAutowireAutodetectSetsMode() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("autowire")).thenReturn(true);
		when(element.getAttribute("autowire")).thenReturn("autodetect");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAutowireMode()).isEqualTo(AbstractBeanDefinition.AUTOWIRE_NO);
	}

	@Test
	void parseInternalWithDefaultInitAndDestroyMethodsUsesDefaults() {
		Element element = createElement("classpath:test.groovy", null, null);
		ParserContext parserContext = mockParserContext();

		BeanDefinitionDefaults defaults = new BeanDefinitionDefaults();
		defaults.setInitMethodName("defaultInit");
		defaults.setDestroyMethodName("defaultDestroy");
		when(parserContext.getDelegate().getBeanDefinitionDefaults()).thenReturn(defaults);

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, parserContext);

		assertThat(beanDefinition.getInitMethodName()).isEqualTo("defaultInit");
		assertThat(beanDefinition.getDestroyMethodName()).isEqualTo("defaultDestroy");
	}

	@Test
	void parseInternalWithoutScriptSourceOrInlineScriptReturnsNull() {
		Element element = createElement(null, null, null);
		when(element.getElementsByTagName("inline-script")).thenReturn(mock(NodeList.class));

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition result = parser.parseInternal(element, mockParserContext());

		assertThat(result).isNull();
	}

	@Test
	void parseInternalWithValidRefreshCheckDelaySetsAttribute() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("refresh-check-delay")).thenReturn(true);
		when(element.getAttribute("refresh-check-delay")).thenReturn("3000");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAttribute("refresh-check-delay")).isNull();
	}

	@Test
	void parseInternalWithRefreshCheckDelaySetsAttribute() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("refresh-check-delay")).thenReturn(true);
		when(element.getAttribute("refresh-check-delay")).thenReturn("1000");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAttribute("refresh-check-delay")).isNull();
	}

	@Test
	void parseInternalWithProxyTargetClassAttribute() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("proxy-target-class")).thenReturn(true);
		when(element.getAttribute("proxy-target-class")).thenReturn("true");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAttribute("proxy-target-class")).isNull();
	}

	@Test
	void parseInternalWithAutowireByTypeSetsMode() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("autowire")).thenReturn(true);
		when(element.getAttribute("autowire")).thenReturn("byType");

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertThat(beanDefinition.getAutowireMode()).isEqualTo(AbstractBeanDefinition.AUTOWIRE_NO);
	}

	@Test
	void parseInternalWithDefaultDestroyMethod() {
		Element element = createElement("classpath:test.groovy", null, null);
		ParserContext parserContext = mockParserContext();

		BeanDefinitionDefaults defaults = new BeanDefinitionDefaults();
		defaults.setDestroyMethodName("defaultDestroy");
		when(parserContext.getDelegate().getBeanDefinitionDefaults()).thenReturn(defaults);

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, parserContext);

		assertThat(beanDefinition.getDestroyMethodName()).isEqualTo("defaultDestroy");
	}

	@Test
	void parseInternalWithPropertyElements() {
		Element element = createElement("classpath:test.groovy", null, null);
		ParserContext parserContext = mockParserContext();
		BeanDefinitionParserDelegate delegate = parserContext.getDelegate();

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		parser.parseInternal(element, parserContext);

		verify(delegate).parsePropertyElements(eq(element), any(AbstractBeanDefinition.class));
	}

	@Test
	void shouldGenerateIdAsFallbackReturnsTrue() {
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		assertThat(parser.shouldGenerateIdAsFallback()).isTrue();
	}

	@Test
	void parseInternalWithNeitherScriptSourceNorInlineScriptLogsError() {
		Element element = mock(Element.class);
		when(element.hasAttribute("script-source")).thenReturn(false);
		when(element.getElementsByTagName("inline-script")).thenReturn(mock(NodeList.class));
		when(element.getChildNodes()).thenReturn(mock(NodeList.class));

		XmlReaderContext readerContext = mock(XmlReaderContext.class);
		ParserContext parserContext = mockParserContext();
		when(parserContext.getReaderContext()).thenReturn(readerContext);

		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");
		AbstractBeanDefinition result = parser.parseInternal(element, parserContext);

		assertThat(result).isNull();
		verify(readerContext).error(eq("Must specify either 'script-source' or 'inline-script'."), eq(element));
	}


	private Element createElement(String scriptSource, String scriptInterfaces, String customizerRef) {
		Element element = mock(Element.class);

		when(element.hasAttribute("script-source")).thenReturn(true);
		when(element.getAttribute("script-source")).thenReturn(scriptSource);

		when(element.hasAttribute("script-interfaces")).thenReturn(true);
		when(element.getAttribute("script-interfaces")).thenReturn(scriptInterfaces);

		when(element.hasAttribute("customizer-ref")).thenReturn(true);
		when(element.getAttribute("customizer-ref")).thenReturn(customizerRef);

		when(element.getLocalName()).thenReturn("groovy");
		NodeList emptyNodeList = mock(NodeList.class);
		when(element.getChildNodes()).thenReturn(emptyNodeList);
		when(emptyNodeList.getLength()).thenReturn(0);
		when(element.getElementsByTagName("inline-script")).thenReturn(emptyNodeList);

		when(element.hasAttribute(anyString())).thenAnswer(invocation -> {
			String attr = invocation.getArgument(0);
			return switch (attr) {
				case "script-source" -> true;
				case "script-interfaces" -> true;
				case "customizer-ref" -> true;
				default -> false;
			};
		});

		return element;
	}

	private ParserContext mockParserContext() {
		ParserContext parserContext = mock(ParserContext.class);
		XmlReaderContext readerContext = mock(XmlReaderContext.class);
		when(parserContext.getReaderContext()).thenReturn(readerContext);
		when(parserContext.extractSource(any())).thenReturn(null);
		when(parserContext.getRegistry()).thenReturn(mock(org.springframework.beans.factory.support.BeanDefinitionRegistry.class));

		BeanDefinitionParserDelegate delegate = mock(BeanDefinitionParserDelegate.class);
		when(delegate.getAutowireMode(anyString())).thenReturn(0);
		when(delegate.getBeanDefinitionDefaults()).thenReturn(new BeanDefinitionDefaults());
		doNothing().when(delegate).parsePropertyElements(any(), any());
		when(parserContext.getDelegate()).thenReturn(delegate);
		return parserContext;
	}

}
