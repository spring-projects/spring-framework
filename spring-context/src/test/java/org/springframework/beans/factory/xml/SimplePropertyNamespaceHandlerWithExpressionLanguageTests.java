/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;

import static org.junit.Assert.*;

/**
 * Tests for combining the expression language and the p namespace. Due to the required EL dependency, this test is in
 * context module rather than the beans module.
 *
 * @author Arjen Poutsma
 */
public class SimplePropertyNamespaceHandlerWithExpressionLanguageTests {

	@Test
	public void combineWithExpressionLanguage() {
		ApplicationContext applicationContext =
				new ClassPathXmlApplicationContext("simplePropertyNamespaceHandlerWithExpressionLanguageTests.xml",
						getClass());
		ITestBean foo = applicationContext.getBean("foo", ITestBean.class);
		ITestBean bar = applicationContext.getBean("bar", ITestBean.class);
		assertEquals("Invalid name", "Baz", foo.getName());
		assertEquals("Invalid name", "Baz", bar.getName());
	}

}
