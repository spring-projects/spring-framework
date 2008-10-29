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

package org.springframework.aop.config;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Mark Fisher
 */
public class AopNamespaceHandlerPointcutErrorTests extends TestCase {

	public void testDuplicatePointcutConfig() {
		try {
			new XmlBeanFactory(new ClassPathResource(
					"org/springframework/aop/config/aopNamespaceHandlerPointcutDuplicationTests.xml"));
			fail("parsing should have caused a BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			assertTrue(ex.contains(BeanDefinitionParsingException.class));
		}
	}
	
	public void testMissingPointcutConfig() {
		try {
			new XmlBeanFactory(new ClassPathResource(
					"org/springframework/aop/config/aopNamespaceHandlerPointcutMissingTests.xml"));
			fail("parsing should have caused a BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			assertTrue(ex.contains(BeanDefinitionParsingException.class));
		}
	}

}
