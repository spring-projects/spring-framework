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

package org.springframework.aop.aspectj;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests for overloaded advice.
 *
 * @author Adrian Colyer
 */
public class OverloadedAdviceTests extends TestCase {

	public void testExceptionOnConfigParsingWithMismatchedAdviceMethod() {
		try {
			new ClassPathXmlApplicationContext("org/springframework/aop/aspectj/overloaded-advice-tests.xml");
		}
		catch (BeanCreationException ex) {
			Throwable cause = ex.getRootCause();
			assertTrue("Should be IllegalArgumentException", cause instanceof IllegalArgumentException);
			assertTrue("invalidAbsoluteTypeName should be detected by AJ",
					cause.getMessage().indexOf("invalidAbsoluteTypeName") != -1);
		}
	}
	
	public void testExceptionOnConfigParsingWithAmbiguousAdviceMethod() {
		try {
			new ClassPathXmlApplicationContext("org/springframework/aop/aspectj/ambiguous-advice-tests.xml");
		}
		catch (BeanCreationException ex) {
			Throwable cause = ex.getRootCause();
			assertTrue("Should be IllegalArgumentException", cause instanceof IllegalArgumentException);
			assertTrue("Cannot resolve method 'myBeforeAdvice' to a unique method",
					cause.getMessage().indexOf("Cannot resolve method 'myBeforeAdvice' to a unique method") != -1);
		}
	}

}
