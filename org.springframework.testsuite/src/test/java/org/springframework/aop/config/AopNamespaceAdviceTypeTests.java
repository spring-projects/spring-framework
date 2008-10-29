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

import org.springframework.beans.ITestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xml.sax.SAXParseException;

/**
 * @author Adrian Colyer
 */
public class AopNamespaceAdviceTypeTests extends TestCase {

	private ApplicationContext context;

	protected String getOKConfigLocation() {
		return "org/springframework/aop/config/aopNamespaceHandlerAdviceTypeOKTests.xml";
	}

	protected String getErrorConfigLocation() {
		return "org/springframework/aop/config/aopNamespaceHandlerAdviceTypeErrorTests.xml";
	}

	public void testParsingOfAdviceTypes() {
		this.context = new ClassPathXmlApplicationContext(getOKConfigLocation());
	}
	
	public void testParsingOfAdviceTypesWithError() {
		try {
			this.context = new ClassPathXmlApplicationContext(getErrorConfigLocation());
			fail("Expected BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			assertTrue(ex.contains(SAXParseException.class));
		}
	}

	protected ITestBean getTestBean() {
		return (ITestBean) this.context.getBean("testBean");
	}

}
