package org.springframework.scripting.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.DESTROY_METHOD_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.INIT_METHOD_ATTRIBUTE;
import static org.springframework.scripting.support.ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE;

class ScriptBeanDefinitionParserTest {

	@Test
	void parseInternal_addsScriptSourceAsFirstConstructorArgument() {
		Element element = createElement("classpath:test.groovy", null, null);
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		ValueHolder argument = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().get(1);
		assertEquals("classpath:test.groovy", argument.getValue());
	}

	@Test
	void parseInternal_withScriptInterfaces_addsAsSecondConstructorArgument() {
		Element element = createElement("classpath:test.groovy", "com.example.TestInterface", null);
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		ValueHolder argument = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().get(2);
		assertEquals("com.example.TestInterface", argument.getValue());
		assertEquals("java.lang.Class[]", argument.getType());
	}

	@Test
	void parseInternal_withCustomizerRef_addsAsNextConstructorArgument() {
		Element element = createElement("classpath:test.groovy", null, "customizerBean");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		ValueHolder argument = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().get(2);
		assertTrue(argument.getValue() instanceof RuntimeBeanReference);
		assertEquals("customizerBean", ((RuntimeBeanReference) argument.getValue()).getBeanName());
	}

	@Test
	void parseInternal_withBothScriptInterfacesAndCustomizerRef_addsInCorrectOrder() {
		Element element = createElement("classpath:test.groovy", "com.example.TestInterface", "customizerBean");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		var arguments = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues();
		assertEquals(3, arguments.size());
		assertEquals("classpath:test.groovy", arguments.get(1).getValue());
		assertEquals("com.example.TestInterface", arguments.get(2).getValue());
		assertEquals("customizerBean", ((RuntimeBeanReference) arguments.get(3).getValue()).getBeanName());
	}

	private Element createElement(String scriptSource, String scriptInterfaces, String customizerRef) {
		Element element = mock(Element.class);

		// Basic required attribute
		when(element.hasAttribute("script-source")).thenReturn(scriptSource != null);
		when(element.getAttribute("script-source")).thenReturn(scriptSource);

		// Optional: script-interfaces
		when(element.hasAttribute("script-interfaces")).thenReturn(scriptInterfaces != null);
		when(element.getAttribute("script-interfaces")).thenReturn(scriptInterfaces);

		// Optional: customizer-ref
		when(element.hasAttribute("customizer-ref")).thenReturn(customizerRef != null);
		when(element.getAttribute("customizer-ref")).thenReturn(customizerRef);

		// Common attributes
		when(element.getLocalName()).thenReturn("groovy");
		NodeList emptyNodeList = mock(NodeList.class);
		when(emptyNodeList.getLength()).thenReturn(0);
		when(element.getElementsByTagName("inline-script")).thenReturn(emptyNodeList);

		// Other default behaviors
		when(element.hasAttribute(anyString())).thenAnswer(invocation -> {
			String attr = invocation.getArgument(0);
			return switch (attr) {
				case "script-source" -> scriptSource != null;
				case "script-interfaces" -> scriptInterfaces != null;
				case "customizer-ref" -> customizerRef != null;
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
		when(delegate.getBeanDefinitionDefaults()).thenReturn(new org.springframework.beans.factory.support.BeanDefinitionDefaults());
		doNothing().when(delegate).parsePropertyElements(any(), any());
		when(parserContext.getDelegate()).thenReturn(delegate);
		return parserContext;
	}

	@Test
	void parseInternal_withEngineAttribute_addsEngineAsFirstConstructorArgument() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("engine")).thenReturn(true);
		when(element.getAttribute("engine")).thenReturn("groovy");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		ValueHolder argument = beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().get(1);
		assertEquals("groovy", argument.getValue());
		assertEquals("classpath:test.groovy",
				beanDefinition.getConstructorArgumentValues().getIndexedArgumentValues().get(2).getValue());
	}

	@Test
	void parseInternal_withRefreshCheckDelay_setsRefreshAttribute() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("refresh-check-delay")).thenReturn(true);
		when(element.getAttribute("refresh-check-delay")).thenReturn("5000");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertNull(beanDefinition.getAttribute("refresh-check-delay"));
	}

	@Test
	void parseInternal_withProxyTargetClass_setsProxyTargetClassAttribute() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute(PROXY_TARGET_CLASS_ATTRIBUTE)).thenReturn(true);
		when(element.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE)).thenReturn("true");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertNull(beanDefinition.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
	}

	@Test
	void parseInternal_withDependsOn_setsDependencies() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute("depends-on")).thenReturn(true);
		when(element.getAttribute("depends-on")).thenReturn("bean1,bean2");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertArrayEquals(new String[]{"bean1", "bean2"}, beanDefinition.getDependsOn());
	}


	@Test
	void parseInternal_withEmptyCustomizerRef_logsError() {
		Element element = createElement("classpath:test.groovy", null, "");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		XmlReaderContext readerContext = mock(XmlReaderContext.class);
		ParserContext parserContext = mockParserContext();
		when(parserContext.getReaderContext()).thenReturn(readerContext);

		parser.parseInternal(element, parserContext);

		verify(readerContext).error(eq("Attribute 'customizer-ref' has empty value"), eq(element));
	}

	@Test
	void parseInternal_withInitAndDestroyMethods_setsMethods() {
		Element element = createElement("classpath:test.groovy", null, null);
		when(element.hasAttribute(INIT_METHOD_ATTRIBUTE)).thenReturn(true);
		when(element.getAttribute(INIT_METHOD_ATTRIBUTE)).thenReturn("init");
		when(element.hasAttribute(DESTROY_METHOD_ATTRIBUTE)).thenReturn(true);
		when(element.getAttribute(DESTROY_METHOD_ATTRIBUTE)).thenReturn("destroy");
		ScriptBeanDefinitionParser parser = new ScriptBeanDefinitionParser("com.example.ScriptFactory");

		AbstractBeanDefinition beanDefinition = parser.parseInternal(element, mockParserContext());

		assertEquals("init", beanDefinition.getInitMethodName());
		assertEquals("destroy", beanDefinition.getDestroyMethodName());
	}


}
