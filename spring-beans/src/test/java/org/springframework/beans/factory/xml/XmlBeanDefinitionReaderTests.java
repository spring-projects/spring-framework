/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.TestCase;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.util.ReflectionUtils;
import org.xml.sax.InputSource;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import test.beans.TestBean;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class XmlBeanDefinitionReaderTests extends TestCase {

	public void testSetParserClassSunnyDay() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
		new XmlBeanDefinitionReader(registry).setDocumentReaderClass(DefaultBeanDefinitionDocumentReader.class);
	}

	public void testSetParserClassToNull() {
		try {
			SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
			new XmlBeanDefinitionReader(registry).setDocumentReaderClass(null);
			fail("Should have thrown IllegalArgumentException (null parserClass)");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testSetParserClassToUnsupportedParserType() throws Exception {
		try {
			SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
			new XmlBeanDefinitionReader(registry).setDocumentReaderClass(String.class);
			fail("Should have thrown IllegalArgumentException (unsupported parserClass)");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testWithOpenInputStream() {
		try {
			SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
			Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
			new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
			fail("Should have thrown BeanDefinitionStoreException (can't determine validation mode)");
		}
		catch (BeanDefinitionStoreException expected) {
		}
	}

	public void testWithOpenInputStreamAndExplicitValidationMode() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
		Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	public void testWithImport() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
		Resource resource = new ClassPathResource("import.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	public void testWithWildcardImport() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
		Resource resource = new ClassPathResource("importPattern.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	public void testWithInputSource() {
		try {
			SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
			InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
			new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
			fail("Should have thrown BeanDefinitionStoreException (can't determine validation mode)");
		}
		catch (BeanDefinitionStoreException expected) {
		}
	}
	                                                                           
	public void testWithInputSourceAndExplicitValidationMode() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
		InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	public void testWithFreshInputStream() {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
		Resource resource = new ClassPathResource("test.xml", getClass());
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
		testBeanDefinitions(registry);
	}

	private void testBeanDefinitions(BeanDefinitionRegistry registry) {
		assertEquals(24, registry.getBeanDefinitionCount());
		assertEquals(24, registry.getBeanDefinitionNames().length);
		assertTrue(Arrays.asList(registry.getBeanDefinitionNames()).contains("rod"));
		assertTrue(Arrays.asList(registry.getBeanDefinitionNames()).contains("aliased"));
		assertTrue(registry.containsBeanDefinition("rod"));
		assertTrue(registry.containsBeanDefinition("aliased"));
		assertEquals(TestBean.class.getName(), registry.getBeanDefinition("rod").getBeanClassName());
		assertEquals(TestBean.class.getName(), registry.getBeanDefinition("aliased").getBeanClassName());
		assertTrue(registry.isAlias("youralias"));
		assertEquals(2, registry.getAliases("aliased").length);
		assertEquals("myalias", registry.getAliases("aliased")[0]);
		assertEquals("youralias", registry.getAliases("aliased")[1]);
	}

	public void testDtdValidationAutodetect() throws Exception {
		doTestValidation("validateWithDtd.xml");
	}

	public void testXsdValidationAutodetect() throws Exception {
		doTestValidation("validateWithXsd.xml");
	}

	private void doTestValidation(String resourceName) throws Exception {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();;
		Resource resource = new ClassPathResource(resourceName, getClass());
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(resource);
		TestBean bean = (TestBean) factory.getBean("testBean");
		assertNotNull(bean);
	}


    /**
     * Verifies that the arg-type sub element of replaced-method is parsed correctly.
     *
     * This relates to SPR-9812.
     *
     */
    public void testReplaceMethodSubElements(){
        SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();;
        ClassPathResource classPathResource = new ClassPathResource("replacedMethodSubElements.xml", getClass());
        Resource resource = classPathResource;
        new XmlBeanDefinitionReader(registry).loadBeanDefinitions(resource);
        GenericBeanDefinition beanDefinition = (GenericBeanDefinition) registry.getBeanDefinition("testBean");
        assertNotNull(beanDefinition);

        MethodOverrides methodOverrides = beanDefinition.getMethodOverrides();
        assertEquals("Unexpected number of method overrides for testBean", 2, methodOverrides.getOverrides().size());

        Method replaceMeString = ReflectionUtils.findMethod( TestBean.class, "replaceMe", String.class );
        Method replaceMeInt = ReflectionUtils.findMethod( TestBean.class, "replaceMe", int.class );

        // check that the override responds to replaceMe(String) but not replaceMe(int)
        // this indicates that the match attribute (specified via <arg-type>String</arg-type>
        // is the same as <arg-type match="String"/>
        assertNotNull(methodOverrides.getOverride( replaceMeString ));
        assertNull(methodOverrides.getOverride( replaceMeInt ));

        // check that the setInt(int) is still override
        Method setAgeInt = ReflectionUtils.findMethod( TestBean.class, "setAge", int.class );
        assertNotNull( methodOverrides.getOverride(setAgeInt) );
    }
}
